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

import com.adobe.acs.commons.fam.ActionManager;
import com.adobe.acs.commons.mcp.ProcessDefinition;
import com.adobe.acs.commons.mcp.ProcessInstance;
import com.adobe.acs.commons.mcp.form.CheckboxComponent;
import com.adobe.acs.commons.mcp.form.FormField;
import com.adobe.acs.commons.mcp.form.PathfieldComponent;
import com.adobe.acs.commons.mcp.model.GenericBlobReport;
import com.adobe.acs.commons.util.visitors.TreeFilteringResourceVisitor;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.html.HtmlParser;
import org.apache.tika.sax.Link;
import org.apache.tika.sax.LinkContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.List;
import java.util.EnumMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Broken Links Checker MCP task
 *
 */
public class BrokenLinksReport extends ProcessDefinition implements Serializable {

    @FormField(name = "Source page",
            description = "Select page/site to be analyzed",
            hint = "/content/my-site",
            component = PathfieldComponent.PageSelectComponent.class,
            options = {"base=/content"})
    private String sourcePath;

    @FormField(name = "Property Regex",
            description = "Regular expression to detect properties containing references to Sling resources",
            required = false,
            options = {"default=^/(etc|content)/.+"})
    private String propertyRegex;

    @FormField(name = "Exclude Properties",
            description = "Comma-separated list of properties to ignore",
            hint = "cq:template,cq:allowedTemplates,....",
            required = false,
            options = {"default=cq:allowedTemplates"})
    private String excludeProperties;

    @FormField(
            name = "Deep check in html",
            description = "If checked, links will be extracted from html properties",
            component = CheckboxComponent.class,
            options = {"checked"}
    )
    private boolean deepCheck = false;

    @FormField(name = "Properties containing html",
            description = "Comma-separate list of properties containing html to extract links",
            required = false,
            options = {"default=text"})
    private String htmlFields;

    private transient Set<String> excludeList;
    private transient Set<String> deepCheckList;
    private transient Pattern regex;

    private static final Logger log = LoggerFactory.getLogger(BrokenLinksReport.class);

    @Override
    public void init() throws RepositoryException {
        excludeList = Arrays.stream(excludeProperties.split(",")).map(String::trim).collect(Collectors.toSet());
        deepCheckList = deepCheck ? Arrays.stream(htmlFields.split(",")).map(String::trim).collect(Collectors.toSet())
            : new HashSet<>();
        regex = Pattern.compile(propertyRegex);
    }

    private final transient GenericBlobReport report = new GenericBlobReport();

    @SuppressWarnings("squid:S00115")
    enum Report {
        reference
    }

    private final transient Map<String, EnumMap<Report, Object>> reportData = new ConcurrentHashMap<>();

    @Override
    public void buildProcess(ProcessInstance instance, ResourceResolver rr) throws LoginException, RepositoryException {
        report.setName(instance.getName());
        instance.defineAction("Collect Broken References", rr, this::buildReport);
        instance.getInfo().setDescription(sourcePath);

    }

    @Override
    public void storeReport(ProcessInstance instance, ResourceResolver rr) throws RepositoryException, PersistenceException {
        GenericBlobReport genericReport = new GenericBlobReport();
        genericReport.setRows(reportData, "Source", Report.class);
        genericReport.persist(rr, instance.getPath() + "/jcr:content/report");

    }

    public void buildReport(ActionManager manager) {
        TreeFilteringResourceVisitor visitor = new TreeFilteringResourceVisitor();
        visitor.setBreadthFirstMode();
        visitor.setTraversalFilterChecked(null);
        visitor.setResourceVisitorChecked((resource, depth) -> {
            manager.deferredWithResolver(rr -> {
                Map<String, List<String>> brokenRefs = collectBrokenReferences(resource, regex, excludeList, deepCheckList);
                for(Map.Entry<String, List<String>> ref : brokenRefs.entrySet()){
                    String propertyPath = ref.getKey();
                    List<String> refs = ref.getValue();
                    reportData.put(propertyPath, new EnumMap<>(Report.class));
                    reportData.get(propertyPath).put(Report.reference, refs.stream().collect(Collectors.joining(",")));
                }
            });
        });
        manager.deferredWithResolver(rr -> visitor.accept(rr.getResource(sourcePath)));
    }

    /**
     * Collect references from a JCR property.
     * A property can be one of:
     * <ol>
     *     <li>A string containing a reference, e.g, fileReference=/content/dam/image.png. </li>
     *     <li>An array of strings, e.g, fileReference=[/content/dam/image1.png, /content/dam/image2.png]</li>
     *     <li>An html fragment containing links , e.g,
     *     <pre>
     *       &lt;p&gt;
     *         &lt;a href="/content/site/page.html"&gt;hello&lt;/a&gt;
     *         &lt;img src="/content/dam/image1.png"&gt;hello&lt;/a&gt;
     *       &lt;/p&gt;
     *     </pre>
     *     </li>
     * </ol>
     *
     * @param property an entry from a ValueMap
     * @param htmlFields  lst of properties containing html
     * @return stream containing extracted references
     */
    static Stream<String> collectPaths(Map.Entry<String, Object> property, Set<String> htmlFields) {
        Object p = property.getValue();

        Stream<String> stream;
        if (p.getClass() == String[].class) {
            stream = Arrays.stream((String[]) p);
        } else if (p.getClass() == String.class){
            stream = Stream.of((String) p);
        } else {
            stream = Stream.empty();
        }
        if (htmlFields.contains(property.getKey())) {
            stream = stream.flatMap(val -> {
                try {
                    // parse html and extract links via underlying tagsoup library
                    LinkContentHandler linkHandler = new LinkContentHandler();
                    HtmlParser parser = new HtmlParser();
                    parser.parse(new ByteArrayInputStream(val.getBytes("utf-8")), linkHandler, new Metadata(), new ParseContext());
                    return linkHandler.getLinks().stream().map(Link::getUri);
                } catch (Exception e) {
                    log.warn("Could not parse links from property value of {}", property.getKey(), e);
                    return Stream.empty();
                }
            });
        }
        return stream;
    }

    /**
     * Collect broken references from properties of the given resource
     *
     * @param resource      the resource to check
     * @param regex         regex to to detect properties containing references. Set from @FormField
     * @param skipList      properties to ignore. Set from @FormField
     * @param htmlFields    field containing html .
     * @return broken references keyed by property. The value is a List because a property can contain multiple links,
     * e.g. if it is multivalued or it is html containing multiple links.
     */
    static Map<String, List<String>> collectBrokenReferences(Resource resource, Pattern regex, Set<String> skipList, Set<String> htmlFields) {

        return resource.getValueMap().entrySet().stream()
                .filter(entry -> !skipList.contains(entry.getKey()))
                .collect(Collectors.toMap(
                        entry -> resource.getPath() + "/" + entry.getKey(),
                        entry -> {
                            List<String> brokenPaths =  collectPaths(entry, htmlFields)
                                    .filter(href -> regex.matcher(href).matches())
                                    .filter(path -> ResourceUtil.isNonExistingResource(resource.getResourceResolver().resolve(path)))
                                    .collect(Collectors.toList());
                            return brokenPaths;
                        })).entrySet().stream().filter(e -> !e.getValue().isEmpty())
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
     }

    // access from unit tests
    Map<String, EnumMap<Report, Object>> getReportData() {
        return reportData;
    }
}
