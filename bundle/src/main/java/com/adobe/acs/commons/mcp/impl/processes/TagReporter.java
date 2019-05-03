/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import javax.jcr.RepositoryException;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.fam.ActionManager;
import com.adobe.acs.commons.mcp.ProcessDefinition;
import com.adobe.acs.commons.mcp.ProcessInstance;
import com.adobe.acs.commons.mcp.form.FormField;
import com.adobe.acs.commons.mcp.form.PathfieldComponent;
import com.adobe.acs.commons.mcp.model.GenericReport;
import com.adobe.acs.commons.mcp.util.StringUtil;
import com.day.cq.commons.RangeIterator;
import com.day.cq.tagging.Tag;
import com.day.cq.tagging.TagManager;

/**
 * Creates a report of the tags in an AEM instance, showing how many times they
 * are referenced
 */
public class TagReporter extends ProcessDefinition implements Serializable {
  public enum ITEM_STATUS {
    FAILURE, INVALID_TAG, SUCCESS
  }

  public enum REPORT_COLUMNS {
    REFERENCE_COUNT, STATUS, TAG_ID, TAG_TITLE
  }

  private static final Logger log = LoggerFactory.getLogger(TagReporter.class);

  public static final String PROCESS_NAME = "Tag Report";

  private static final long serialVersionUID = 4325471295421747160L;
  @FormField(name = "Root Tag path", description = "The path to the root tag / namespace of this report, all tags under this tag will be included in the report", component = PathfieldComponent.NodeSelectComponent.class, required = true, options = {
      "default=/content/cq:tags", "base=/" })
  public transient String path = null;
  private final transient GenericReport report = new GenericReport();

  private final transient List<EnumMap<REPORT_COLUMNS, Object>> reportRows = new ArrayList<>();

  private transient TagManager tagManager;

  @Override
  public void buildProcess(ProcessInstance instance, ResourceResolver rr) throws LoginException, RepositoryException {
    log.trace("buildProcess");
    report.setName(instance.getName());
    instance.getInfo().setDescription(String.format("Report for tags under [ %s ]", path));
    instance.defineCriticalAction("Traversing tags", rr, this::traverseTags);
  }

  public GenericReport getReport() {
    return report;
  }

  public List<EnumMap<REPORT_COLUMNS, Object>> getReportRows() {
    return reportRows;
  }

  @Override
  public void init() throws RepositoryException {
    // nothing to do here
  }

  private void record(ITEM_STATUS status, String tagId, String title, long referenceCount) {
    final EnumMap<REPORT_COLUMNS, Object> row = new EnumMap<>(REPORT_COLUMNS.class);

    row.put(REPORT_COLUMNS.STATUS, StringUtil.getFriendlyName(status.name()));
    row.put(REPORT_COLUMNS.TAG_ID, tagId);
    row.put(REPORT_COLUMNS.REFERENCE_COUNT, referenceCount);
    row.put(REPORT_COLUMNS.TAG_TITLE, title);

    reportRows.add(row);
  }

  private void recordTag(Tag tag) {
    log.trace("recordTag {}", tag);

    RangeIterator<Resource> it = tagManager.find(tag.getTagID());
    if (it != null) {
      long count = it.getSize();
      if (count == -1) {
        count = 0L;
        while (it.hasNext()) {
          count++;
          it.next();
        }
      }
      record(ITEM_STATUS.SUCCESS, tag.getTagID(), tag.getTitle(), count);
    } else {
      record(ITEM_STATUS.INVALID_TAG, tag.getTagID(), tag.getTitle(), -1);
    }

    Iterator<Tag> children = tag.listChildren();
    while (children.hasNext()) {
      recordTag(children.next());
    }
  }

  @Override
  public void storeReport(ProcessInstance instance, ResourceResolver rr)
      throws RepositoryException, PersistenceException {
    log.trace("storeReport");
    report.setRows(reportRows, REPORT_COLUMNS.class);
    report.persist(rr, instance.getPath() + "/jcr:content/report");
  }

  public void traverseTags(ActionManager manager) throws Exception {
    log.trace("traverseTags");
    manager.withResolver(resolver -> {
      tagManager = resolver.adaptTo(TagManager.class);
      Tag rootTag = Optional.ofNullable(resolver.getResource(path)).map(r -> r.adaptTo(Tag.class)).orElse(null);
      if (rootTag == null) {
        log.warn("Failed to find tag at path: {}", path);
        record(ITEM_STATUS.FAILURE, "Failed to find tag at path: " + path, "N/A", 0);
      } else {
        recordTag(rootTag);
      }
    });
  }
}
