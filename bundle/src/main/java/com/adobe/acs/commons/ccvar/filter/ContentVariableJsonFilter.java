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

package com.adobe.acs.commons.ccvar.filter;

import com.adobe.acs.commons.ccvar.PropertyAggregatorService;
import com.adobe.acs.commons.ccvar.PropertyConfigService;
import com.adobe.acs.commons.ccvar.util.ContentVariableReplacementUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.engine.EngineConstants;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Filter used to look for and rewrite content variables present in JSON responses. By default only handles .model.json
 * requests.
 */
@Component(
        service = Filter.class,
        property = {
                Constants.SERVICE_RANKING + ":Integer=" + Integer.MIN_VALUE,
                EngineConstants.SLING_FILTER_SCOPE + "=" + EngineConstants.FILTER_SCOPE_REQUEST,
                EngineConstants.SLING_FILTER_PATTERN + "=/content/.*"
        },
        configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(ocd = ContentVariableJsonFilter.Config.class)
public class ContentVariableJsonFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(ContentVariableJsonFilter.class);

    @Reference
    private PropertyAggregatorService propertyAggregatorService;

    @Reference
    private PropertyConfigService propertyConfigService;

    private List<Pattern> includePatterns;
    private List<Pattern> excludePatterns;
    private boolean allInvalidIncludes;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // do nothing
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        SlingHttpServletRequest slingHttpServletRequest = (SlingHttpServletRequest) servletRequest;
        RequestPathInfo currentPathInfo = slingHttpServletRequest.getRequestPathInfo();
        if (StringUtils.equals(currentPathInfo.getExtension(), "json") && shouldProcess(slingHttpServletRequest.getPathInfo())) {
            CapturingResponseWrapper capturingResponseWrapper = new CapturingResponseWrapper((SlingHttpServletResponse) servletResponse);
            filterChain.doFilter(servletRequest, capturingResponseWrapper);

            String currentResponse = capturingResponseWrapper.getCaptureAsString();
            String toReturn = currentResponse;
            try {
                Map<String, Object> contentVariableReplacements = propertyAggregatorService.getProperties(slingHttpServletRequest);
                if (contentVariableReplacements.size() > 0) {
                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode currentTree = objectMapper.readTree(currentResponse);
                    replaceInElements(currentTree, contentVariableReplacements);
                    toReturn = currentTree.toString();
                }
            } catch (Exception e) {
                LOG.error("Exception during JSON property replacement", e);
            } finally {
                servletResponse.getWriter().write(toReturn);
            }
        } else {
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    /**
     * Checks the current URL path against the included and excluded patterns. Exclusions hold priority.
     *
     * @param urlPath Current request path
     * @return if the request should be processed
     */
    private boolean shouldProcess(String urlPath) {
        // If includes are specified but none are valid we skip all requests.
        if (allInvalidIncludes) {
            LOG.debug("Include patterns are empty due to invalid regex patterns, not processing any requests");
            return false;
        }
        // If include and exclude lists are both empty we process all requests.
        if (includePatterns.isEmpty() && excludePatterns.isEmpty()) {
            LOG.debug("Include and Exclude patterns are empty, processing all requests");
            return true;
        }
        boolean shouldProcess = false;
        for (Pattern pattern : includePatterns) {
            if (pattern.matcher(urlPath).matches()) {
                LOG.debug("URL path {} matches INCLUDE pattern {}", urlPath, pattern.toString());
                shouldProcess = true;
                break;
            }
        }
        for (Pattern pattern : excludePatterns) {
            if (pattern.matcher(urlPath).matches()) {
                LOG.debug("URL path {} matches EXCLUDE pattern {}", urlPath, pattern.toString());
                shouldProcess = false;
                break;
            }
        }
        LOG.debug("URL path {} is processed: {}", urlPath, shouldProcess);
        return shouldProcess;
    }

    /**
     * Delegate replacement to different object types.
     *
     * @param node Input JSON node
     * @param contentVariableReplacements current map of content variables
     */
    private void replaceInElements(JsonNode node, Map<String, Object> contentVariableReplacements) {
        if (node.isArray()) {
            replaceInArray(node, contentVariableReplacements);
        } else if (node.isObject()) {
            replaceInObject(node, contentVariableReplacements);
        }
    }

    /**
     * Iterates through array items and replaces any placeholders found.
     *
     * @param node Array node
     * @param contentVariableReplacements current map of content variables
     */
    private void replaceInArray(JsonNode node, Map<String, Object> contentVariableReplacements) {
        if (node.get(0) != null && node.get(0).isTextual()) {
            List<String> updated = new LinkedList<>();
            for (JsonNode arrayItem : node) {
                String current = arrayItem.asText();
                updated.add(replaceInString(current, contentVariableReplacements));
            }
            ((ArrayNode) node).removeAll();
            for (int i = 0; i < updated.size(); i++) {
                ((ArrayNode) node).insert(i, updated.get(i));
            }
        } else if (node.get(0) != null && node.get(0).isContainerNode()) {
            for (JsonNode arrayItem : node) {
                replaceInElements(arrayItem, contentVariableReplacements);
            }
        }
    }

    /**
     * Iterates over keys to replace any placeholders in the values.
     *
     * @param node Object node
     * @param contentVariableReplacements current map of content variables
     */
    private void replaceInObject(JsonNode node, Map<String, Object> contentVariableReplacements) {
        Iterator<String> fieldNames = node.fieldNames();
        while (fieldNames.hasNext()) {
            String name = fieldNames.next();
            JsonNode nodeValue = node.get(name);
            if (nodeValue.isContainerNode()) {
                replaceInElements(nodeValue, contentVariableReplacements);
            } else if (nodeValue.isTextual()) {
                String current = nodeValue.asText();
                String replaced = replaceInString(current, contentVariableReplacements);
                if (!StringUtils.equals(current, replaced)) {
                    ((ObjectNode) node).put(name, replaced);
                }
            }
        }
    }

    /**
     * Reusable method to replace placeholders in the input string./
     *
     * @param input String input
     * @param contentVariableReplacements current map of content variables
     * @return The replaced or original String
     */
    private String replaceInString(String input, Map<String, Object> contentVariableReplacements) {
        final List<String> keys = ContentVariableReplacementUtil.getKeys(input);
        for (String key : keys) {
            // If the placeholder key is in the map then replace it
            if (ContentVariableReplacementUtil.hasKey(contentVariableReplacements, key)) {
                String replaceValue = (String) ContentVariableReplacementUtil.getValue(contentVariableReplacements, key);
                input = ContentVariableReplacementUtil.doReplacement(input, key, replaceValue, propertyConfigService.getAction(key));
            }
        }
        return input;
    }

    @Override
    public void destroy() {
        // do nothing
    }

    @Activate
    protected void activate(ContentVariableJsonFilter.Config config) {
        includePatterns = new ArrayList<>();
        excludePatterns = new ArrayList<>();

        boolean invalidInclude = false;
        for (String item : config.includes()) {
            if (StringUtils.isNotBlank(item)) {
                try {
                    includePatterns.add(Pattern.compile(item));
                } catch (PatternSyntaxException e) {
                    invalidInclude = true;
                    LOG.error("Error adding includePattern. Invalid syntax in {}", item);
                }
            }
        }
        for (String item : config.excludes()) {
            if (StringUtils.isNotBlank(item)) {
                try {
                    excludePatterns.add(Pattern.compile(item));
                } catch (PatternSyntaxException e) {
                    LOG.error("Error adding excludePattern. Invalid syntax in {}", item);
                }
            }
        }
        if (invalidInclude && includePatterns.size() == 0) {
            allInvalidIncludes = true;
        }
    }

    @ObjectClassDefinition(name = "ACS AEM Commons - Contextual Content Variable JSON Filter Configuration", description = "JSON Rewriting"
            + " Filter for supporting Content Variable Property replacement in JSON responses. This filter only applies to"
            + " /content/* and *.json requests. Additional filtering options are available below.")
    @interface Config {

        @AttributeDefinition(name = "Include Patterns", description = "Regex patterns to for URL paths to INCLUDE in "
                + "the JSON rewriting.")
        String[] includes() default {".*\\.model\\..*"};

        @AttributeDefinition(name = "Exclude Patterns", description = "Regex patterns to for URL paths to EXCLUDE in "
                + "the JSON rewriting. Exclusions hold priority over inclusions.")
        String[] excludes();
    }
}
