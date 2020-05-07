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

import javax.jcr.Binary;
import javax.jcr.Item;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

@Component(service = Servlet.class, property = {
        "sling.servlet.selectors=robots",
        "sling.servlet.extensions=txt",
        "sling.servlet.methods=GET"
}, configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = RobotsServlet.RobotsServletConfig.class, factory = true)
public final class RobotsServlet extends SlingSafeMethodsServlet {

    private static final Logger log = LoggerFactory.getLogger(RobotsServlet.class);
    private static final String ALLOW = "Allow: ";
    private static final String USER_AGENT = "User-agent: ";
    private static final String SITEMAP = "Sitemap: ";
    private static final String DISALLOW = "Disallow: ";

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

        PageManager pageManager = request.getResourceResolver().adaptTo(PageManager.class);
        Page page = pageManager.getContainingPage(request.getResource());
        if (page != null) {
            rules.getGroups().forEach(group -> writeGroup(group, request.getResourceResolver(), page, writer));

            rules.getSitemaps().stream().map(sitemap -> buildSitemapString(sitemap, request.getResourceResolver())).forEach(writer::println);
            if (!rules.getSitemapProperties().isEmpty()) {
                addRulesFromPages(page, request.getResourceResolver(), rules.getSitemapProperties(), writer, this::buildSitemapString);
            }
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }

    }

    private void writeGroup(RobotsRuleGroup group, ResourceResolver resourceResolver, Page page, PrintWriter writer) {
        if (printGroupingComments) {
            writer.println("# Start Group: " + group.getGroupName());
        }

        group.getUserAgents().stream().map(this::buildUserAgentsString).forEach(writer::println);

        group.getAllowed().stream().map(allowed -> buildAllowedString(allowed, resourceResolver)).forEach(writer::println);

        List<String> allowProperties = group.getAllowProperties();
        if (!allowProperties.isEmpty()) {
            addRulesFromPages(page, resourceResolver, allowProperties, writer, this::buildAllowedString);
        }

        group.getDisallowed().stream().map(disallowed -> buildDisallowedString(disallowed, resourceResolver)).forEach(writer::println);

        List<String> disallowProperties = group.getDisallowProperties();
        if (!disallowProperties.isEmpty()) {
            addRulesFromPages(page, resourceResolver, disallowProperties, writer, this::buildDisallowedString);
        }

        if (printGroupingComments) {
            writer.println("# End Group: " + group.getGroupName());
        }
    }

    private void addRulesFromPages(Page page, ResourceResolver resourceResolver, List<String> propNames, PrintWriter writer, BiFunction<Page, ResourceResolver, String> ruleBuilderFunc) {
        ValueMap pageProps = page.getProperties();
        boolean added = false;
        for (String prop : propNames) {
            boolean shouldAdd = pageProps.get(prop, false);
            if (shouldAdd) {
                String rule = ruleBuilderFunc.apply(page, resourceResolver);
                writer.println(rule);
                added = true;
                break;
            }
        }

        if (!added) {
            //since the rules are added to (dis)allow a page and it's children, we only need to recurse if no rule is added for the current page.
            Iterator<Page> pageIterator = page.listChildren(new PageFilter(false, true), false);
            pageIterator.forEachRemaining(child -> addRulesFromPages(child, resourceResolver, propNames, writer, ruleBuilderFunc));
        }

    }

    private void writeFromJcrProperty(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        String absoluteRobotsContentsPropertyPath = robotsContentsPropertyPath;
        if (!absoluteRobotsContentsPropertyPath.startsWith("/")) {
            absoluteRobotsContentsPropertyPath = request.getResource().getPath() + "/" + robotsContentsPropertyPath;
        }

        Session session = request.getResourceResolver().adaptTo(Session.class);
        boolean written = false;
        try {
            if (session.itemExists(absoluteRobotsContentsPropertyPath)) {
                Item item = session.getItem(absoluteRobotsContentsPropertyPath);
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
                    } else {
                        log.warn("configured property {} found, but type {} is not String or Binary.", absoluteRobotsContentsPropertyPath, PropertyType.nameFromValue(propertyType));
                    }
                } else {
                    log.warn("Item found at configured property {}, but is not a property.", absoluteRobotsContentsPropertyPath);
                }
            } else {
                log.warn("configured property {} does not exist.", absoluteRobotsContentsPropertyPath);
            }

            if (!written) {
                log.error("no response was written while processing robots with jcr property {}.", absoluteRobotsContentsPropertyPath);
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (RepositoryException e) {
            log.error("Repository Exception while processing robots with jcr property {}", absoluteRobotsContentsPropertyPath, e);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private String buildUserAgentsString(String agent) {
        return USER_AGENT + agent;
    }

    private String buildSitemapString(String sitemap, ResourceResolver resourceResolver) {
        PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
        Page page = pageManager.getContainingPage(sitemap);
        if (page != null) {
           return buildSitemapString(page, resourceResolver);
        } else {
            return SITEMAP + sitemap;
        }
    }

    private String buildSitemapString(Page page, ResourceResolver resourceResolver) {
        String sitemapRule = externalizer.externalLink(resourceResolver, externalizerDomain, page.getPath()) + ".sitemap.xml";

        return SITEMAP + sitemapRule;
    }

    private String buildAllowedString(String allowedRule, ResourceResolver resourceResolver) {
        PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
        Page page = pageManager.getContainingPage(allowedRule);
        if (page != null) {
            return buildAllowedString(page, resourceResolver);
        } else {
            return ALLOW + allowedRule;
        }
    }

    private String buildAllowedString(Page page, ResourceResolver resourceResolver) {
        String allowedRule = resourceResolver.map(page.getPath()) + "/";

        return ALLOW + allowedRule;
    }

    private String buildDisallowedString(String disallowedRule, ResourceResolver resourceResolver) {
        PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
        Page page = pageManager.getContainingPage(disallowedRule);
        if (page != null) {
            return buildDisallowedString(page, resourceResolver);
        } else {
            return DISALLOW + disallowedRule;
        }
    }

    private String buildDisallowedString(Page page, ResourceResolver resourceResolver) {
        String disallowedRule = resourceResolver.map(page.getPath()) + "/";

        return DISALLOW + disallowedRule;
    }

    @ObjectClassDefinition(name = "ACS AEM Commons - Robots Servlet")
    protected @interface RobotsServletConfig {

        @AttributeDefinition(name = "Web-Console Configuration Name Hint")
        String webconsole_configurationFactory_nameHint() default "Robots.txt for resource types: [{sling.servlet.resourceTypes}]";

        @AttributeDefinition(name = "Sling Resource Type", description = "Sling Resource Type for the Home Page component or components.")
        String[] sling_servlet_resourceTypes() default {};

        @AttributeDefinition(name = "Robots Content Property", description = "Path (either relative or absolute) to a String or Binary property containing the entire robots.txt contents. This could be a page property (e.g. robotsTxtContents) or the contents of a file within the DAM (e.g. /content/dam/my-site/seo/robots.txt/jcr:content/renditions/original/jcr:content/jcr:data). If this is specified, all other configurations are effectively ignored.")
        String robots_content_property_path();

        @AttributeDefinition(name = "User Agent Directives", description = "A set of User-agent directives to add to the robots file. Each directive is optionally pre-fixed with a ruleGroupName. Syntax: [<ruleGroupName>:]<user agent name>")
        String[] user_agent_directives() default {};

        @AttributeDefinition(name = "Allow Directives", description = "A set of Allow directives to add to the robots file. Each directive is optionally pre-fixed with a ruleGroupName. Syntax: [<ruleGroupName>:]<allowed path>. If the specified path is a valid cq page, resourceResolver.map() will be called prior to adding the rule.")
        String[] allow_directives() default {};

        @AttributeDefinition(name = "Allow Property Names", description = "A list of page properties used to generate the allow directives.  Any directives added through this method are in addition to those specified in the allow.directives property. Each property name is optionally pre-fixed with a ruleGroupName. Syntax: [<ruleGroupName>:]<propertyName>")
        String[] allow_property_names() default {};

        @AttributeDefinition(name = "Disallow Directives", description = "A set of Disallow directives to add to the robots file. Each directive is optionally pre-fixed with a ruleGroupName. Syntax: [<ruleGroupName>:]<disallowed path>. If the specified path is a valid cq page, resourceResolver.map() will be called prior to adding the rule.")
        String[] disallow_directives() default {};

        @AttributeDefinition(name = "Disallow Property Names", description = "A list of page properties used to generate the disallow directives.  Any directives added through this method are in addition to those specified in the disallowed.directives property. Each property name is optionally pre-fixed with a ruleGroupName. Syntax: [<ruleGroupName>:]<propertyName>")
        String[] disallow_property_names() default {};

        @AttributeDefinition(name = "Sitemap Directives", description = "A set of Sitemap directives to add to the robots file. If the specified path is a valid cq page, externalizer is called with the specified Externalizer Domain to generate an absolute url to that page's .sitemap.xml, which will resolve to the ACS Commons Site Map Servlet.")
        String[] sitemap_directives() default {};

        @AttributeDefinition(name = "Sitemap Property Names", description = "A list of page properties used to generate the sitemap directives.  Any directives added through this method are in addition to those specified in the sitemap.directives property.")
        String[] sitemap_property_names() default {};

        @AttributeDefinition(name = "Externalizer Domain", description = "Must correspond to a configuration of the Externalizer component.")
        String externalizer_domain() default "publish";

        @AttributeDefinition(name = "Print Grouping Comments", description = "When enabled, comments are printed to the file for start and end of each rule group. This is primarily for debugging purposes.")
        boolean print_grouping_comments() default false;
    }

    private class RobotsRuleSet {

        private static final String GROUP_NAME_DEFAULT = "RobotsServletDefaultGroup";

        private List<String> sitemaps;
        private List<String> sitemapProperties;
        private Map<String, RobotsRuleGroup> groups;

        public RobotsRuleSet(RobotsServletConfig config) {
            sitemaps = Arrays.asList(config.sitemap_directives());
            sitemapProperties = Arrays.asList(config.sitemap_property_names());
            groups = new LinkedHashMap<>();

            String[] userAgents = config.user_agent_directives();
            processConfig(userAgents, RobotsRuleGroup::getUserAgents);

            String[] allowed = config.allow_directives();
            processConfig(allowed, RobotsRuleGroup::getAllowed);

            String[] disallowed = config.disallow_directives();
            processConfig(disallowed, RobotsRuleGroup::getDisallowed);

            String[] disallowProps = config.disallow_property_names();
            processConfig(disallowProps, RobotsRuleGroup::getDisallowProperties);

            String[] allowProps = config.allow_property_names();
            processConfig(allowProps, RobotsRuleGroup::getAllowProperties);
        }

        private void processConfig(String[] configs, Function<RobotsRuleGroup, List<String>> configListGetFunc) {
            for (String config : configs) {
                Pair<String, String> groupNamePair = getGroupNameTuple(config);
                String groupName = groupNamePair.getKey();
                String value = groupNamePair.getValue();

                RobotsRuleGroup group = getRobotsRuleGroup(groupName);
                configListGetFunc.apply(group).add(value);
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

        public List<String> getSitemapProperties() {
            return Collections.unmodifiableList(sitemapProperties);
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
