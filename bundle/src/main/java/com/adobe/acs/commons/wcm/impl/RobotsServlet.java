/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2020 Adobe
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
package com.adobe.acs.commons.wcm.impl;

import com.day.cq.commons.Externalizer;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageFilter;
import com.day.cq.wcm.api.PageManager;
import com.google.common.io.ByteStreams;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.*;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.*;
import java.util.function.BiFunction;

@Component(service = Servlet.class, immediate = true, property = {
        "sling.servlet.selectors=robots",
        "sling.servlet.extensions=txt",
        "sling.servlet.methods=GET"
}, configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = RobotsServlet.RobotsServletConfig.class, factory = true)
public final class RobotsServlet extends SlingSafeMethodsServlet {

    private static final Logger log = LoggerFactory.getLogger(RobotsServlet.class);

    private String externalizerDomain;

    private String robotsContentsPropertyPath;

    private boolean printGroupingComments;

    private RobotsRuleSet rules;

    @Reference
    private Externalizer externalizer;

    @Activate
    protected void activate(RobotsServletConfig config) {
        externalizerDomain = config.externalizer_domain();
        robotsContentsPropertyPath = config.robots_content_property_path();
        printGroupingComments = config.print_grouping_comments();

        rules = new RobotsRuleSet(config);
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");

        write(request, response);
    }

    private void write(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        if (StringUtils.isNotBlank(robotsContentsPropertyPath)) {
            writeFromJcrProperty(request, response);
        } else {
            writeFromOsgiConfig(request, response);
        }
    }

    private void writeFromOsgiConfig(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        PrintWriter writer = response.getWriter();

        rules.getGroups().forEach(group -> writeGroup(group, request, writer));

        rules.getSitemaps().stream().map(sitemap -> buildSitemapString(sitemap, request.getResourceResolver())).forEach(writer::println);
    }

    private void writeGroup(RobotsRuleGroup group, SlingHttpServletRequest request, PrintWriter writer) {
        if (printGroupingComments) {
            writer.println("# Start Group: " + group.getGroupName());
        }

        group.getUserAgents().stream().map(this::buildUserAgentsString).forEach(writer::println);

        group.getAllowed().stream().map(allowed -> buildAllowedString(allowed, request.getResourceResolver())).forEach(writer::println);

        List<String> allowProperties = group.getAllowProperties();
        if (!allowProperties.isEmpty()) {
            PageManager pageManager = request.getResourceResolver().adaptTo(PageManager.class);
            Page page = pageManager.getContainingPage(request.getResource());
            addRulesFromPages(page, request.getResourceResolver(), allowProperties, writer, this::buildAllowedString);
        }

        group.getDisallowed().stream().map(disallowed -> buildDisallowedString(disallowed, request.getResourceResolver())).forEach(writer::println);

        List<String> disallowProperties = group.getDisallowProperties();
        if (!disallowProperties.isEmpty()) {
            PageManager pageManager = request.getResourceResolver().adaptTo(PageManager.class);
            Page page = pageManager.getContainingPage(request.getResource());
            addRulesFromPages(page, request.getResourceResolver(), disallowProperties, writer, this::buildDisallowedString);
        }

        if (printGroupingComments) {
            writer.println("# End Group: " + group.getGroupName());
        }
    }

    private void addRulesFromPages(Page page, ResourceResolver resourceResolver, List<String> propNames, PrintWriter writer, BiFunction<Page, ResourceResolver, String> func) {
        ValueMap pageProps = page.getProperties();
        boolean added =false;
        for (String prop : propNames) {
            boolean shouldAdd = pageProps.get(prop, false);
            if (shouldAdd) {
                String rule = func.apply(page, resourceResolver);
                writer.println(rule);
                added=true;
                break;
            }
        }

        if(!added) {
            //since the rules are added to (dis)allow a page and it's children, we only need to recurse if no rule is added for the current page.
            Iterator<Page> pageIterator = page.listChildren(new PageFilter(false, true), false);
            pageIterator.forEachRemaining(child -> addRulesFromPages(child, resourceResolver, propNames, writer, func));
        }

    }

    private void writeFromJcrProperty(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        if (!robotsContentsPropertyPath.startsWith("/")) {
            robotsContentsPropertyPath = request.getResource().getPath() + "/" + robotsContentsPropertyPath;
        }

        Session session = request.getResourceResolver().adaptTo(Session.class);
        boolean written = false;
        try {
            if (session.itemExists(robotsContentsPropertyPath)) {
                Item item = session.getItem(robotsContentsPropertyPath);
                if (item instanceof Property) {
                    Property prop = (Property) item;
                    int propertyType = prop.getType();
                    Value value = prop.getValue();
                    if (propertyType == PropertyType.BINARY) {
                        Binary binary = value.getBinary();
                        InputStream stream = binary.getStream();
                        ByteStreams.copy(stream, response.getOutputStream());
                        written = true;
                    } else if (propertyType == PropertyType.STRING) {
                        response.getWriter().print(value.getString());
                        written = true;
                    }
                }
            }

            if (!written) {
                log.error("no response was written while processing robots with jcr property {}", robotsContentsPropertyPath);
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (RepositoryException e) {
            log.error("Repository Exception while processing robots with jcr property {}", robotsContentsPropertyPath, e);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private String buildUserAgentsString(String agent) {
        return "User-agent: " + agent;
    }

    private String buildSitemapString(String sitemap, ResourceResolver resourceResolver) {
        PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
        Page page = pageManager.getContainingPage(sitemap);
        if (page != null) {
            sitemap = externalizer.externalLink(resourceResolver, externalizerDomain, sitemap) + ".sitemap.xml";
        }

        return "Sitemap: " + sitemap;
    }

    private String buildAllowedString(String allowedRule, ResourceResolver resourceResolver) {
        PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
        Page page = pageManager.getContainingPage(allowedRule);
        if (page != null) {
            allowedRule = resourceResolver.map(allowedRule) + "/";
        }

        return "Allow: " + allowedRule;
    }

    private String buildAllowedString(Page page, ResourceResolver resourceResolver) {
        String allowedRule = resourceResolver.map(page.getPath()) + "/";

        return "Allow: " + allowedRule;
    }

    private String buildDisallowedString(String disallowedRule, ResourceResolver resourceResolver) {
        PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
        Page page = pageManager.getContainingPage(disallowedRule);
        if (page != null) {
            disallowedRule = resourceResolver.map(disallowedRule) + "/";
        }

        return "Disallow: " + disallowedRule;
    }

    private String buildDisallowedString(Page page, ResourceResolver resourceResolver) {
        String disallowedRule = resourceResolver.map(page.getPath()) + "/";

        return "Disallow: " + disallowedRule;
    }

    @ObjectClassDefinition(name = "ACS AEM Commons - Robots Servlet")
    protected @interface RobotsServletConfig {

        @AttributeDefinition(name = "Web-Console Configuration Name Hint")
        String webconsole_configurationFactory_nameHint() default "Robots.txt for resource types: [{sling.servlet.resourceTypes}]";

        @AttributeDefinition(name = "Sling Resource Type", description = "Sling Resource Type for the Home Page component or components.")
        String[] sling_servlet_resourceTypes() default {};

        @AttributeDefinition(name = "Robots Content Property", description = "Path (either relative or absolute) to a String or Binary property containing the entire robots.txt contents. This could be a page property (e.g. robotsTxtContents) or the contents of a file within the DAM (e.g. /content/dam/my-site/seo/robots.txt/jcr:content/renditions/original/jcr:content/jcr:data).")
        String robots_content_property_path();

        @AttributeDefinition(name = "Externalizer Domain", description = "Must correspond to a configuration of the Externalizer component.")
        String externalizer_domain() default "publish";

        @AttributeDefinition(name = "User Agent Directives", description = "A set of User-agent directives to add to the robots file. Each directive is optionally pre-fixed with a ruleGroupName. Syntax: [<ruleGroupName>:]<user agent name>")
        String[] user_agent_directives() default {};

        @AttributeDefinition(name = "Allow Directives", description = "A set of Allow directives to add to the robots file. Each directive is optionally pre-fixed with a ruleGroupName. Syntax: [<ruleGroupName>:]<allowed path>. If the specified path is a valid cq page, resourceResolver.map() will be called prior to adding the rule.")
        String[] allow_directives() default {};

        @AttributeDefinition(name = "Allow Property Name", description = "A list of page properties used to generate the allow directives.  Any directives added through this method are in addition to those specified in the allowed.directives property. Each property name is optionally pre-fixed with a ruleGroupName. Syntax: [<ruleGroupName>:]<propertyName>")
        String[] allow_property_names() default {};

        @AttributeDefinition(name = "Disallow Directives", description = "A set of Disallow directives to add to the robots file. Each directive is optionally pre-fixed with a ruleGroupName. Syntax: [<ruleGroupName>:]<disallowed path>. If the specified path is a valid cq page, resourceResolver.map() will be called prior to adding the rule.")
        String[] disallow_directives() default {};

        @AttributeDefinition(name = "Disallow Property Name", description = "A list of page properties used to generate the disallow directives.  Any directives added through this method are in addition to those specified in the disallowed.directives property. Each property name is optionally pre-fixed with a ruleGroupName. Syntax: [<ruleGroupName>:]<propertyName>")
        String[] disallow_property_names() default {};

        @AttributeDefinition(name = "Sitemap Directives", description = "A set of Sitemap directives to add to the robots file. If the specified path is a valid cq page, externalizer is called with the specified Externalizer Domain to generate an absolute url to that page's .sitemap.xml, which will resolve to the ACS Commons Site Map Servlet.")
        String[] sitemap_directives() default {};

        @AttributeDefinition(name = "Print Grouping Comments", description = "When enabled, comments are printed to the file for start and end of each rule group.")
        boolean print_grouping_comments() default false;
    }

    private class RobotsRuleSet {

        private static final String GROUP_NAME_DEFAULT = "RobotsServletDefaultGroup";

        private List<String> sitemaps;
        private Map<String, RobotsRuleGroup> groups;

        public RobotsRuleSet(RobotsServletConfig config) {
            sitemaps = Arrays.asList(config.sitemap_directives());
            groups = new LinkedHashMap<>();

            String[] userAgents = config.user_agent_directives();
            for (String agent : userAgents) {
                Pair<String, String> groupNamePair = getGroupNameTuple(agent);
                String groupName = groupNamePair.getKey();
                String value = groupNamePair.getValue();

                RobotsRuleGroup group = getRobotsRuleGroup(groupName);
                group.getUserAgents().add(value);
                groups.put(groupName, group);
            }

            String[] allowed = config.allow_directives();
            for (String allow : allowed) {
                Pair<String, String> groupNamePair = getGroupNameTuple(allow);
                String groupName = groupNamePair.getKey();
                String value = groupNamePair.getValue();

                RobotsRuleGroup group = getRobotsRuleGroup(groupName);
                group.getAllowed().add(value);
                groups.put(groupName, group);
            }

            String[] disallowed = config.disallow_directives();
            for (String disallow : disallowed) {
                Pair<String, String> groupNamePair = getGroupNameTuple(disallow);
                String groupName = groupNamePair.getKey();
                String value = groupNamePair.getValue();

                RobotsRuleGroup group = getRobotsRuleGroup(groupName);
                group.getDisallowed().add(value);
                groups.put(groupName, group);
            }

            String[] disallowProps = config.disallow_property_names();
            for (String prop : disallowProps) {
                Pair<String, String> groupNamePair = getGroupNameTuple(prop);
                String groupName = groupNamePair.getKey();
                String value = groupNamePair.getValue();

                RobotsRuleGroup group = getRobotsRuleGroup(groupName);
                group.getDisallowProperties().add(value);
                groups.put(groupName, group);
            }

            String[] allowProps = config.allow_property_names();
            for (String prop : allowProps) {
                Pair<String, String> groupNamePair = getGroupNameTuple(prop);
                String groupName = groupNamePair.getKey();
                String value = groupNamePair.getValue();

                RobotsRuleGroup group = getRobotsRuleGroup(groupName);
                group.getAllowProperties().add(value);
                groups.put(groupName, group);
            }

        }

        private RobotsRuleGroup getRobotsRuleGroup(String groupName) {
            RobotsRuleGroup group = groups.get(groupName);
            if (group == null) {
                group = new RobotsRuleGroup();
                group.groupName = groupName;
            }
            return group;
        }

        private Pair<String, String> getGroupNameTuple(String configValue) {
            String groupName = GROUP_NAME_DEFAULT;
            String value = configValue;
            String[] split = configValue.split(":");
            if (split.length > 1) {
                groupName = split[0];
                value = split[1];
            }

            return new ImmutablePair<>(groupName, value);
        }

        public List<String> getSitemaps() {
            return Collections.unmodifiableList(sitemaps);
        }

        public List<RobotsRuleGroup> getGroups() {
            return new ArrayList<>(groups.values());
        }
    }

    private class RobotsRuleGroup {

        private String groupName;
        private List<String> userAgents = new ArrayList<>();
        private List<String> allowed = new ArrayList<>();
        private List<String> disallowed = new ArrayList<>();
        private List<String> allowProperties = new ArrayList<>();
        private List<String> disallowProperties = new ArrayList<>();

        public String getGroupName() {
            return groupName;
        }

        public List<String> getUserAgents() {
            return userAgents;
        }

        public List<String> getAllowed() {
            return allowed;
        }

        public List<String> getDisallowed() {
            return disallowed;
        }

        public List<String> getAllowProperties() {
            return allowProperties;
        }

        public List<String> getDisallowProperties() {
            return disallowProperties;
        }
    }
}
