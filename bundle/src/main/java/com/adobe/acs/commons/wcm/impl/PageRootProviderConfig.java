/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */
package com.adobe.acs.commons.wcm.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration instance for Page Root Provider.
 * Use `service.ranking` to guarantee priority between conflicting configurations.
 *
 * @see PageRootProviderMultiImpl
 */
@Component(
        service = PageRootProviderConfig.class,
        configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(ocd = PageRootProviderConfig.Config.class, factory = true)
public class PageRootProviderConfig {

    /* Default root. */
    static final String DEFAULT_PAGE_ROOT_PATH = "/content";

    private static final Logger log = LoggerFactory.getLogger(PageRootProviderConfig.class);

    private List<Pattern> pageRootPatterns = null;
    private String xfRootPathMethod = null;
    private boolean historyViewerFallback = false;
    private boolean launchFallback = false;

    /**
     * Retrieves the configured patterns.
     *
     * @return list of page root patterns.
     */
    public List<Pattern> getPageRootPatterns() {
        return Optional.ofNullable(this.pageRootPatterns)
                .map(Collections::unmodifiableList)
                .orElse(null);
    }

    /**
     * Get XF root path method
     *
     * @return XF root path method
     */
    public String getXfRootPathMethod() {
        return this.xfRootPathMethod;
    }

    /**
     * Get history viewer fallback setting
     *
     * @return true if history viewer should get root from current/live site/XF paths
     */
    public boolean getHistoryViewerFallback() {
        return this.historyViewerFallback;
    }

    /**
     * Get launch fallback setting
     *
     * @return true if launches should get root from current/live site paths
     */
    public boolean getLaunchFallback() {
        return this.launchFallback;
    }

    @Activate
    protected void activate(Config config) {
        List<Pattern> patterns = new ArrayList<>();
        String[] regexes = config.page_root_path();

        for(String regex : regexes) {
            try {
                Pattern p = Pattern.compile("^(" + regex + ")(|/.*)$");
                patterns.add(p);
                log.debug("Added Page Root Pattern [ {} ] to PageRootProvider", p);
            } catch (Exception e) {
                log.error("Could not compile regex [ {} ] to pattern. Skipping...", regex, e);
            }
        }

        this.pageRootPatterns = Collections.unmodifiableList(patterns);
        this.xfRootPathMethod = config.xf_root_path_method();
        this.historyViewerFallback = config.history_viewer_fallback();
        this.launchFallback = config.launch_fallback();
    }

    @Deactivate
    protected void deactivate() {
        if (this.pageRootPatterns != null) {
            for (Pattern p : this.pageRootPatterns) {
                log.debug("Removed Page Root Pattern [ {} ] from PageRootProvider", p);
            }

            this.pageRootPatterns = null;
        }
    }

    @ObjectClassDefinition(
            name = "ACS AEM Commons - Page Root Provider Configuration",
            description = "Configuration instance for Page Root Provider, a service to fetch the site root page for a given resource."
    )
    protected @interface Config {
        @AttributeDefinition(
                name = "Root page path pattern",
                description = "Regex(es) used to select the root page root path. Regex must contain at least one group (with index 1) which is used as page root. It is matched against the given path. Evaluates list top-down; first match wins. Defaults to [ " + DEFAULT_PAGE_ROOT_PATH + " ]"
        )
        String[] page_root_path() default { DEFAULT_PAGE_ROOT_PATH };

        @AttributeDefinition(
                name = "Experience Fragment Root Path Method",
                description = "Method to determine the root path for experience fragments. If not set (default), the page root path method will be used. Supported values are: [ site ]."
        )
        String xf_root_path_method() default "";

        @AttributeDefinition(
                name = "History Viewer Fallback",
                description = "Enable this feature to use the corresponding live path when determining the page root. Note that for values reliant on page root (e.g. Shared Component Properties) the history viewer will reflect current values rather than historical values. Default false."
        )
        boolean history_viewer_fallback() default false;

        @AttributeDefinition(
                name = "Content Launch Fallback",
                description = "Enable this feature to have a content launch use the corresponding live path when determining the page root. This is generally desirable, as values reliant on page root (e.g. Shared Component Properties) will reflect the values that will apply when this page is promoted (except in the rare case where the launch also includes the page root and that page root is also being promoted with new values). Default false."
        )
        boolean launch_fallback() default false;

        @AttributeDefinition
        String webconsole_configurationFactory_nameHint() default "Page Root Provider - Patterns: [ {page.root.path} ]";
    }
}
