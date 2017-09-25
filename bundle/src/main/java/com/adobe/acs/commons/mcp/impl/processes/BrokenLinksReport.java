package com.adobe.acs.commons.mcp.impl.processes;

import com.adobe.acs.commons.fam.ActionManager;
import com.adobe.acs.commons.mcp.ProcessDefinition;
import com.adobe.acs.commons.mcp.ProcessInstance;
import com.adobe.acs.commons.mcp.form.FormField;
import com.adobe.acs.commons.mcp.form.PathfieldComponent;
import com.adobe.acs.commons.mcp.model.GenericReport;
import com.adobe.acs.commons.util.visitors.TreeFilteringResourceVisitor;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;

import javax.jcr.RepositoryException;
import java.io.Serializable;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Yegor Kozlov
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

    transient private Set<String> excludeList;
    transient private Pattern regex;

    @Override
    public void init() throws RepositoryException {
        excludeList = Arrays.stream(excludeProperties.split(",")).map(String::trim).collect(Collectors.toSet());
        regex = Pattern.compile(propertyRegex);
    }

    transient private final GenericReport report = new GenericReport();

    enum REPORT {
        reference
    }

    ;
    transient private final Map<String, EnumMap<REPORT, Object>> reportData = new TreeMap<>();

    @Override
    public void buildProcess(ProcessInstance instance, ResourceResolver rr) throws LoginException, RepositoryException {
        report.setName(instance.getName());
        instance.defineAction("Collect Broken References", rr, this::collectBrokenLinks);
        instance.getInfo().setDescription(sourcePath);

    }

    @Override
    public void storeReport(ProcessInstance instance, ResourceResolver rr) throws RepositoryException, PersistenceException {
        GenericReport report = new GenericReport();
        report.setRows(reportData, "Source", REPORT.class);
        report.persist(rr, instance.getPath() + "/jcr:content/report");

    }

    public void collectBrokenLinks(ActionManager manager) {
        TreeFilteringResourceVisitor visitor = new TreeFilteringResourceVisitor();
        visitor.setBreadthFirstMode();
        visitor.setTraversalFilter(null);
        visitor.setResourceVisitor((resource, depth) -> {
            ResourceResolver resolver = resource.getResourceResolver();
            resource.getValueMap().entrySet().stream()
                    .filter(entry -> !excludeList.contains(entry.getKey()))
                    .filter(entry -> entry.getValue() instanceof String || entry.getValue() instanceof String[])
                    .forEach(entry -> {

                        List<String> paths = collectPaths(entry.getValue())
                                .filter(path -> ResourceUtil.isNonExistingResource(resolver.resolve(path)))
                                .collect(Collectors.toList());
                        if (!paths.isEmpty()) {
                            String propertyPath = resource.getPath() + "/" + entry.getKey();
                            reportData.put(propertyPath, new EnumMap<>(REPORT.class));
                            reportData.get(propertyPath).put(REPORT.reference, paths.stream().collect(Collectors.joining(",")));
                        }

                    });
        });
        manager.deferredWithResolver(rr -> visitor.accept(rr.getResource(sourcePath)));
    }

    Stream<String> collectPaths(Object p) {
        Stream<String> stream;
        if (p.getClass().isArray()) {
            stream = Arrays.stream((String[]) p);
        } else {
            stream = Stream.of(p.toString());
        }
        return stream.filter(val -> regex.matcher(val).matches());
    }
}
