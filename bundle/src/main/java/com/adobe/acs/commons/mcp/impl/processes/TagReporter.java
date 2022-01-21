/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2019 Adobe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.adobe.acs.commons.mcp.impl.processes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;
import javax.jcr.RepositoryException;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.fam.ActionManager;
import com.adobe.acs.commons.fam.actions.Actions;
import com.adobe.acs.commons.mcp.ProcessDefinition;
import com.adobe.acs.commons.mcp.ProcessInstance;
import com.adobe.acs.commons.mcp.form.CheckboxComponent;
import com.adobe.acs.commons.mcp.form.Description;
import com.adobe.acs.commons.mcp.form.FormField;
import com.adobe.acs.commons.mcp.form.PathfieldComponent;
import com.adobe.acs.commons.mcp.form.RadioComponent;
import com.adobe.acs.commons.mcp.form.TextfieldComponent;
import com.adobe.acs.commons.mcp.model.GenericBlobReport;
import com.adobe.acs.commons.mcp.util.StringUtil;
import com.day.cq.commons.RangeIterator;
import com.day.cq.tagging.Tag;
import com.day.cq.tagging.TagManager;

/**
 * Creates a report of the tags in an AEM instance, showing how many times they
 * are referenced
 */
public class TagReporter extends ProcessDefinition implements Serializable {
  public enum ItemStatus {
    EXTENDED_DATA, FAILURE, INVALID_TAG, SUCCESS
  }

  public enum ReferenceMethod {

    @Description("Slower method, will find references in any attribute, but runs a full text search for each tag")
    DEEP_SEARCH,

    @Description("Quicker method, requires the references to be in a cq:tags field of a cq:Taggable node (or mixin)")
    DEFAULT_TAG_MANAGER_FIND

  }

  public enum ReportColumns {
    REFERENCE_COUNT, REFERENCES, STATUS, TAG_ID, TAG_TITLE
  }

  private static final Logger log = LoggerFactory.getLogger(TagReporter.class);

  public static final String PROCESS_NAME = "Tag Report";

  private static final long serialVersionUID = 4325471295421747160L;

  /*
   * Limit based on cell char limit in Excel.
   *
   * @See https://bz.apache.org/bugzilla/show_bug.cgi?id=56579
   */
  public static final int CELL_CHAR_LIMIT = 32767;

  @FormField(name = "Root Search path", description = "The path under which to search for references to the tags", component = PathfieldComponent.NodeSelectComponent.class, required = true, options = {
      "default=/content", "base=/" })
  public transient String rootSearchPath = null;

  @FormField(name = "Tag path", description = "The path to the root tag / namespace of this report, all tags under this tag will be included in the report", component = PathfieldComponent.NodeSelectComponent.class, required = true, options = {
      "default=/content/cq:tags", "base=/" })
  public transient String tagPath = null;

  @FormField(name = "Include References", description = "Include the references to the tags in the report", component = CheckboxComponent.class)
  public boolean includeReferences = false;

  @FormField(name = "References Char Limit", description = "Character limit for references when saving to the spreadsheet cells, must be less than 32,767", component = TextfieldComponent.class, required = true, options = {
      "default=4096" })
  public String referencesCharacterLimit = "4096";

  @FormField(name = "Reference Method", description = "The method used for finding references to the tag", component = RadioComponent.EnumerationSelector.class, options = {
      "vertical", "default=DEFAULT_TAG_FIND" })
  public ReferenceMethod referenceMethod = ReferenceMethod.DEFAULT_TAG_MANAGER_FIND;

  private final transient GenericBlobReport report = new GenericBlobReport();

  private final transient List<EnumMap<ReportColumns, Object>> reportRows = new ArrayList<>();

  private List<Pair<String, String>> tags = new ArrayList<>();

  @Override
  public void buildProcess(ProcessInstance instance, ResourceResolver rr) throws LoginException, RepositoryException {
    log.trace("buildProcess");
    report.setName(instance.getName());
    instance.getInfo()
        .setDescription(String.format("Report for tags under [ %s ], referenced from [ %s ]", tagPath, rootSearchPath));
    instance.defineCriticalAction("Traversing tags", rr, this::traverseTags);

    instance.defineCriticalAction("Finding references", rr, this::recordTags);
  }

  private @Nonnull List<String> computeReferenceCellValues(Collection<String> references) {
    List<String> cells = new ArrayList<>();
    List<String> cell = new ArrayList<>();
    int len = 0;
    for (String ref : references) {
      if (len + ref.length() < CELL_CHAR_LIMIT) {
        cell.add(ref);
        len = len + ref.length() + 1;
      } else {
        cells.add(cell.stream().collect(Collectors.joining(",")));
        cell.clear();
        cell.add(ref);
        len = ref.length() + 1;
      }
    }
    if (!cell.isEmpty()) {
      cells.add(cell.stream().collect(Collectors.joining(",")));
    }
    if (cells.isEmpty()) {
      cells.add("");
    }
    return cells;
  }

  private void findReferencesDeep(ResourceResolver resolver, String id, String title) {
    ReferenceFinder referenceFinder = new ReferenceFinder(resolver, id, this.rootSearchPath, true);
    if (this.includeReferences) {
      record(ItemStatus.SUCCESS, id, title,
          referenceFinder.getAllReferences().stream().map(Pair::getLeft).collect(Collectors.toSet()));
    } else {
      record(ItemStatus.SUCCESS, id, title, referenceFinder.getAllReferences().size());
    }
  }

  private void findReferencesTagMgr(ResourceResolver resolver, String id, String title) {
    TagManager tagManager = resolver.adaptTo(TagManager.class);
    RangeIterator<Resource> refs = Optional.ofNullable(tagManager)
        .map(tm -> tm.find(this.rootSearchPath, new String[] { id })).orElse(null);
    List<String> references = new ArrayList<>();

    if (refs != null) {
      int count = 0;
      while (refs.hasNext()) {
        count++;
        Resource ref = refs.next();
        if (this.includeReferences) {
          references.add(ref.getPath());
        }
      }
      if (this.includeReferences) {
        record(ItemStatus.SUCCESS, id, title, references);
      } else {
        record(ItemStatus.SUCCESS, id, title, count);
      }
    } else {
      log.debug("TagManager failed to return reference list for: {}", id);
      record(ItemStatus.INVALID_TAG, id, title, -1);
    }
  }

  public GenericBlobReport getReport() {
    return report;
  }

  public List<EnumMap<ReportColumns, Object>> getReportRows() {
    return Collections.unmodifiableList(reportRows);
  }

  @Override
  public void init() throws RepositoryException {
    if (referenceMethod == null) {
      referenceMethod = ReferenceMethod.DEFAULT_TAG_MANAGER_FIND;
    }
  }

  private void record(ItemStatus status, String tagId, String title, Collection<String> references) {

    if (this.includeReferences) {
      List<String> referenceCellValues = computeReferenceCellValues(references);
      for (int i = 0; i < referenceCellValues.size(); i++) {
        final EnumMap<ReportColumns, Object> row = new EnumMap<>(ReportColumns.class);

        ItemStatus stat = i == 0 ? status : ItemStatus.EXTENDED_DATA;
        row.put(ReportColumns.STATUS, StringUtil.getFriendlyName(stat.name()));
        row.put(ReportColumns.TAG_ID, tagId);
        row.put(ReportColumns.REFERENCE_COUNT, (long) references.size());
        row.put(ReportColumns.TAG_TITLE, title);
        row.put(ReportColumns.REFERENCES, referenceCellValues.get(i));
        reportRows.add(row);
      }
    } else {
      final EnumMap<ReportColumns, Object> row = new EnumMap<>(ReportColumns.class);

      row.put(ReportColumns.STATUS, StringUtil.getFriendlyName(status.name()));
      row.put(ReportColumns.TAG_ID, tagId);
      row.put(ReportColumns.REFERENCE_COUNT, (long) references.size());
      row.put(ReportColumns.TAG_TITLE, title);
      reportRows.add(row);
    }

  }

  private void record(ItemStatus status, String tagId, String title, long referenceCount) {
    final EnumMap<ReportColumns, Object> row = new EnumMap<>(ReportColumns.class);

    row.put(ReportColumns.STATUS, StringUtil.getFriendlyName(status.name()));
    row.put(ReportColumns.TAG_ID, tagId);
    row.put(ReportColumns.REFERENCE_COUNT, referenceCount);
    row.put(ReportColumns.TAG_TITLE, title);

    reportRows.add(row);
  }

  private void recordTag(ResourceResolver resolver, Pair<String, String> tag) {
    log.trace("recordTag {}", tag);
    String id = tag.getLeft();
    String title = tag.getRight();
    try {
      if (this.referenceMethod == ReferenceMethod.DEEP_SEARCH) {
        findReferencesDeep(resolver, id, title);
      } else {
        findReferencesTagMgr(resolver, id, title);
      }
    } catch (Exception e) {
      log.warn("Failed to find references to tag due to exception", e);
      record(ItemStatus.INVALID_TAG, id, title, -1);
    }
  }

  public void recordTags(ActionManager manager) {
    log.trace("recordReferences");

    this.tags.forEach(t -> manager.deferredWithResolver(resolver -> {
      log.debug("Recording references to: {}", t.getLeft());
      Actions.setCurrentItem(String.format("Recording references to [ %s ]", t.getLeft()));
      recordTag(resolver, t);
    }));
  }

  @Override
  public void storeReport(ProcessInstance instance, ResourceResolver rr)
      throws RepositoryException, PersistenceException {
    log.trace("storeReport");
    report.setRows(reportRows, ReportColumns.class);
    report.persist(rr, instance.getPath() + "/jcr:content/report");
  }

  public void traverseTags(ActionManager manager) throws Exception {
    log.trace("traverseTags");
    manager.withResolver(resolver -> {
      Resource root = resolver.getResource(tagPath);
      log.info("Finding tags under: {}", root);
      if (root != null) {
        List<Tag> roots = Optional.ofNullable(root.adaptTo(Tag.class)).map(Collections::singletonList)
            .orElse(StreamSupport.stream(root.getChildren().spliterator(), false).map(c -> {
              log.debug("Checking if child resource {} is a tag", c);
              return c.adaptTo(Tag.class);
            }).filter(Objects::nonNull).collect(Collectors.toList()));
        roots.forEach(this::traverseTags);
      } else {
        log.warn("Failed to find resource at path: {}", tagPath);
        record(ItemStatus.FAILURE, "Failed to find resource at path: " + tagPath, "N/A", -1);
      }

    });
  }

  private void traverseTags(Tag tag) {
    log.debug("traverseTags({})", tag.getTagID());
    Actions.setCurrentItem("Traversing tags: " + tag.getTagID());
    this.tags.add(new ImmutablePair<String, String>(tag.getTagID(), tag.getTitle()));
    Iterator<Tag> children = tag.listChildren();
    while (children.hasNext()) {
      traverseTags(children.next());
    }
  }
}
