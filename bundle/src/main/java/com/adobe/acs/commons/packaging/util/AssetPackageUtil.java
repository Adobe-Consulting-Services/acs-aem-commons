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

package com.adobe.acs.commons.packaging.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.day.cq.dam.api.DamConstants;
import com.day.cq.dam.commons.util.DamUtil;
import com.day.cq.wcm.api.NameConstants;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.vault.fs.api.PathFilterSet;
import org.apache.jackrabbit.vault.fs.filter.DefaultPathFilter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AssetPackageUtil {

    protected static final Logger log = LoggerFactory.getLogger(AssetPackageUtil.class);

    /* Property names */
    private static final String PN_PAGE_PATH = "pagePath";
    private static final String PN_EXCLUDE_PAGES = "excludePages";
    private static final String PN_ASSET_PREFIX = "assetPrefix";
    private static final String PN_PAGE_EXCLUSIONS = "pageExclusions";
    private static final String PN_ASSET_EXCLUSIONS = "assetExclusions";

    private String customPrefix;
    private List<Pattern> pageExclusionPatterns;
    private List<Pattern> assetExclusionPatterns;
    private final Set<String> excludedPages;
    private final ValueMap properties;
    private final ResourceResolver resourceResolver;

    public AssetPackageUtil(ValueMap properties, ResourceResolver resourceResolver) {
        this.properties = properties;
        this.resourceResolver = resourceResolver;
        this.customPrefix = properties.get(PN_ASSET_PREFIX, String.class);
        this.pageExclusionPatterns = generatePatterns(properties.get(PN_PAGE_EXCLUSIONS, new String[]{}));
        this.assetExclusionPatterns = generatePatterns(properties.get(PN_ASSET_EXCLUSIONS, new String[]{}));
        this.excludedPages = new LinkedHashSet<>();
    }

    /**
     * Generates the filter paths by recursing through the content and returns the list.
     *
     * @return the compiled list of {@link PathFilterSet} objects with assets and pages.
     */
    public List<PathFilterSet> getPackageFilterPaths() {
        final String pagePath = properties.get(PN_PAGE_PATH, String.class);
        final Set<PathFilterSet> packageFilterPathSet;
        if (StringUtils.isBlank(pagePath)) {
            packageFilterPathSet = Collections.emptySet();
        } else {
            packageFilterPathSet = this.findAssetPaths(resourceResolver, pagePath);
        }

        // Add the page path as a filter unless it is explicitly excluded
        this.addPagePath(packageFilterPathSet, pagePath, properties.get(PN_EXCLUDE_PAGES, false));

        List<PathFilterSet> packageFilterPaths = new ArrayList<>(packageFilterPathSet);
        // Reverse the new list to put the page inclusion at the top
        Collections.reverse(packageFilterPaths);
        return packageFilterPaths;
    }

    /**
     * Takes a list of Strings that should be valid patterns. If they are not then the exception is
     * logged and the pattern is not added to the list of checked patterns.
     *
     * @param input The array of intended patterns
     * @return The converted array of patterns
     */
    private List<Pattern> generatePatterns(String[] input) {
        final List<Pattern> patterns = new ArrayList<>();
        for (String item : input) {
            try {
                if (StringUtils.isBlank(item)) {
                    continue;
                }
                patterns.add(Pattern.compile(item));
            } catch (PatternSyntaxException e) {
                log.error("Pattern invalid, skipping. Pattern value: {}", item);
            }
        }
        return patterns;
    }

    /**
     * Recursively iterate over the parent path specified in the configuration to aggregate all the
     * String or String Array property values that are referencing a path in the DAM.
     *
     * @param resourceResolver Resolver from the current Request
     * @param pagePath         The page path
     * @return The full list of filter sets
     */
    private Set<PathFilterSet> findAssetPaths(final ResourceResolver resourceResolver,
                                               final String pagePath) {

        final Set<PathFilterSet> filters = new LinkedHashSet<>();
        final Resource parentResource = resourceResolver.resolve(pagePath);
        if (isExcluded(this.pageExclusionPatterns, pagePath)) {
            if (pagePath.endsWith("/" + NameConstants.NN_CONTENT)) {
                return filters;
            }
            final PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
            Page page = pageManager.getContainingPage(parentResource);
            if (page != null) {
                excludedPages.add(page.getPath());
            } else {
                excludedPages.add(parentResource.getPath());
            }
            return filters;
        }
        final ValueMap parentResourceProperties = parentResource.getValueMap();

        // Iterate over property map for Strings and String arrays and optionally add a filter
        for (String key : parentResourceProperties.keySet()) {
            final Object value = parentResourceProperties.get(key);
            if (value instanceof String) {
                addFilter(filters, (String) value, resourceResolver);
            } else if (value instanceof String[]) {
                final String[] arrayValue = (String[]) value;
                for (String stringValue : arrayValue) {
                    addFilter(filters, stringValue, resourceResolver);
                }
            }
        }

        // Recurse over child nodes and add the asset references found there.
        final Iterator<Resource> children = parentResource.listChildren();
        while (children.hasNext()) {
            final Resource child = children.next();
            filters.addAll(findAssetPaths(resourceResolver, child.getPath()));
        }

        return filters;
    }

    /**
     * Optionally adds a single page path to the list of package path filters. Also adds any
     * exclusions generated when creating the list of asset paths.
     *
     * @param currentPaths The current set of path filters
     * @param pagePath     The page path specified in the packager dialog
     * @param excludePages The property value for whether the page path should be added to the
     *                     package
     */
    private void addPagePath(Set<PathFilterSet> currentPaths, String pagePath, boolean excludePages) {
        if (!excludePages && currentPaths.size() > 0) {
            PathFilterSet pageFilter = new PathFilterSet(pagePath);
            if (excludedPages.size() > 0) {
                for (String excludedPath : excludedPages) {
                    pageFilter.addExclude(new DefaultPathFilter(excludedPath));
                }
            }
            currentPaths.add(pageFilter);
        }
    }

    /**
     * Adds a property value to the filter set list if it is not empty, referencing the DAM, and is
     * an actual DAM Asset.
     *
     * @param filters The total list of filter sets
     * @param value   The current property value
     */
    private void addFilter(final Set<PathFilterSet> filters, final String value,
                           final ResourceResolver resourceResolver) {
        if (StringUtils.isNotBlank(value) && DamUtil.isAsset(resourceResolver.getResource(value))
                && fitsAssetPattern(value)) {
            filters.add(new PathFilterSet(value));
        }
    }

    /**
     * Checks to see if the passed asset path fits the patterns necessary. It checks the path prefix
     * as well as seeing if the path is excluded from the aggregation.
     *
     * @param assetPath The current asset path property value
     * @return The boolean result
     */
    private boolean fitsAssetPattern(final String assetPath) {
        boolean startsWith = this.customPrefix != null
                ? assetPath.startsWith(this.customPrefix)
                : assetPath.startsWith(DamConstants.MOUNTPOINT_ASSETS);
        boolean notExcluded = !isExcluded(this.assetExclusionPatterns, assetPath);
        return startsWith && notExcluded;
    }

    /**
     * Checks if the path should be excluded by checking it against the passed pattern list.
     *
     * @param patterns The passed list of patterns to check against
     * @param path     The current path to check
     * @return The boolean result
     */
    private boolean isExcluded(List<Pattern> patterns, String path) {
        for (Pattern pattern : patterns) {

            // Check the current path against the actual pattern (supporting regex)
            if (pattern.matcher(path).matches()) {
                return true;
            }

            // Check the current path against the literal string of the path (supporting folder
            // exclusion without needing /.* at the end of a folder for assets)
            String literalPattern = pattern.toString();
            literalPattern = literalPattern.endsWith("/") ? literalPattern : literalPattern + "/";
            if (path.startsWith(literalPattern)) {
                return true;
            }
        }
        return false;
    }
}
