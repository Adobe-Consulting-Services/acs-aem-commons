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

package com.adobe.acs.commons.properties.filter;

import com.adobe.acs.commons.properties.PropertyAggregatorService;
import com.adobe.acs.commons.properties.util.TemplateReplacementUtil;
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

@Component(
        service = Filter.class,
        property = {
                Constants.SERVICE_RANKING + ":Integer=2000",
                EngineConstants.SLING_FILTER_SCOPE + "=" + EngineConstants.FILTER_SCOPE_REQUEST,
                EngineConstants.SLING_FILTER_PATTERN + "=/content/.*"
        },
        configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(ocd = TemplatedFilter.Config.class)
public class TemplatedFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(TemplatedFilter.class);

    @Reference
    private PropertyAggregatorService propertyAggregatorService;

    private Map<String, Object> properties;
    private List<Pattern> includePatterns;
    private List<Pattern> excludePatterns;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // do nothing
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        RequestPathInfo currentPathInfo = ((SlingHttpServletRequest) servletRequest).getRequestPathInfo();
        if (StringUtils.equals(currentPathInfo.getExtension(), "json") && shouldProcess(((SlingHttpServletRequest) servletRequest).getPathInfo())) {
            CapturingResponseWrapper capturingResponseWrapper = new CapturingResponseWrapper((SlingHttpServletResponse) servletResponse);
            filterChain.doFilter(servletRequest, capturingResponseWrapper);

            SlingHttpServletRequest slingHttpServletRequest = (SlingHttpServletRequest) servletRequest;

            String currentResponse = capturingResponseWrapper.getCaptureAsString();
            String toReturn = currentResponse;
            try {
                properties = propertyAggregatorService.getProperties(slingHttpServletRequest.getResource());
                if (properties.size() > 0) {
                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode currentTree = objectMapper.readTree(currentResponse);
                    replaceInElements(currentTree);
                    toReturn = currentTree.toString();
                }
            } catch (Exception e) {
                log.error("Exception during JSON property replacement", e);
            } finally {
                servletResponse.getWriter().write(toReturn);
            }
        } else {
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    private boolean shouldProcess(String urlPath) {
        if (includePatterns.isEmpty() && excludePatterns.isEmpty()) {
            log.debug("Include and Exclude patterns are empty, processing all requests");
            return true;
        }
        boolean shouldProcess = false;
        for (Pattern pattern : includePatterns) {
            if (pattern.matcher(urlPath).matches()) {
                log.debug("URL path {} matches INCLUDE pattern {}", urlPath, pattern.toString());
                shouldProcess = true;
                break;
            }
        }
        for (Pattern pattern : excludePatterns) {
            if (pattern.matcher(urlPath).matches()) {
                log.debug("URL path {} matches EXCLUDE pattern {}", urlPath, pattern.toString());
                shouldProcess = false;
                break;
            }
        }
        log.debug("URL path {} is processed: {}", urlPath, shouldProcess);
        return shouldProcess;
    }

    private void replaceInElements(JsonNode node) {
        if (node.isArray()) {
            if (node.get(0) != null && node.get(0).isTextual()) {
                List<String> updated = new LinkedList<>();
                for (JsonNode arrayItem : node) {
                    String current = arrayItem.asText();
                    updated.add(replaceInString(current));
                }
                ((ArrayNode) node).removeAll();
                for (int i = 0; i < updated.size(); i++) {
                    ((ArrayNode) node).insert(i, updated.get(i));
                }
            } else if (node.get(0) != null && node.get(0).isContainerNode()) {
                for (JsonNode arrayItem : node) {
                    replaceInElements(arrayItem);
                }
            }
        } else if (node.isObject()) {
            Iterator<String> fieldNames = node.fieldNames();
            while (fieldNames.hasNext()) {
                String name = fieldNames.next();
                if (node.get(name).isContainerNode()) {
                    replaceInElements(node.get(name));
                } else if (node.get(name).isTextual()) {
                    String current = node.get(name).asText();
                    String replaced = replaceInString(current);
                    if (!StringUtils.equals(current, replaced)) {
                        ((ObjectNode) node).put(name, replaced);
                    }
                }
            }
        }
    }

    private String replaceInString(String input) {
        if (TemplateReplacementUtil.hasPlaceholder(input)) {

            // Get the current placeholder in the string
            String placeholder = TemplateReplacementUtil.getPlaceholder(input);

            // Transform it to the key in the property map
            String key = TemplateReplacementUtil.getKey(placeholder);

            // If the placeholder key is in the map then replace it
            if (properties.containsKey(key)) {
                String replaceValue = (String) properties.get(key);
                input = input.replace(placeholder, replaceValue);
            }
        }
        return input;
    }

    @Override
    public void destroy() {

    }

    @Activate
    protected void activate(Config config) {
        includePatterns = new ArrayList<>();
        excludePatterns = new ArrayList<>();

        for (String item : config.includes()) {
            if (StringUtils.isBlank(item)) {
                continue;
            }
            try {
                includePatterns.add(Pattern.compile(item));
            } catch (PatternSyntaxException e) {
                log.error("Error adding includePattern. Invalid syntax in {}", item);
            }
        }
        for (String item : config.excludes()) {
            if (StringUtils.isBlank(item)) {
                continue;
            }
            try {
                excludePatterns.add(Pattern.compile(item));
            } catch (PatternSyntaxException e) {
                log.error("Error adding excludePattern. Invalid syntax in {}", item);
            }
        }
    }

    @ObjectClassDefinition(name = "ACS AEM Commons - Templated Filter Configuration", description = "JSON Rewriting" +
            " Filter for supporting Templated Property replacement in JSON responses. This filter only applies to" +
            " /content/* and *.json requests. Additional filtering options are available below.")
    @interface Config {

        @AttributeDefinition(name = "Include Patterns", description = "Regex patterns to for URL paths to INCLUDE in " +
                "the JSON rewriting.")
        String[] includes() default {"(.*).model.(.*)"};

        @AttributeDefinition(name = "Exclude Patterns", description = "Regex patterns to for URL paths to EXCLUDE in " +
                "the JSON rewriting. Exclusions hold priority over inclusions.")
        String[] excludes();
    }
}
