/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
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
package com.adobe.acs.commons.redirects.models;

import com.adobe.acs.commons.redirects.filter.RedirectFilter;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A collection of redirect rules
 */
public class RedirectConfiguration {

    private boolean nonRegexRequestURIRules = false;
    /**
     * path rules keyed by source, e.g. path1 -> path2.
     * This makes lookup by path a O(1) operation
     */
    private Map<String, RedirectRule> pathRules;
    /**
     * regex rules keyed by their regex pattern.
     */
    private Map<Pattern, RedirectRule> patternRules;
    private String path;
    private String name;

    public static final RedirectConfiguration EMPTY = new RedirectConfiguration();

    private RedirectConfiguration(){
        pathRules = new LinkedHashMap<>();
        patternRules = new LinkedHashMap<>();
    }

    public RedirectConfiguration(Resource resource, String storageSuffix) {
        pathRules = new LinkedHashMap<>();
        patternRules = new LinkedHashMap<>();
        path = resource.getPath();
        name = path.replace("/" + storageSuffix, "");
        Collection<RedirectRule> rules = RedirectFilter.getRules(resource);
        for (RedirectRule rule : rules) {
            if (rule.getRegex() != null) {
                patternRules.put(rule.getRegex(), rule);
            } else {
                // request URI rules are keyed without normalizing
                if(rule.getEvaluateURI()){
                    nonRegexRequestURIRules = true;
                    pathRules.put(rule.getSource(), rule);
                } else {
                    pathRules.put(normalizePath(rule.getSource()), rule);
                }
            }
        }
    }

    /**
     * @return resource path without .html extension
     */

    public static String normalizePath(String resourcePath) {
        int sep = resourcePath.lastIndexOf('.');
        if (sep != -1 && !resourcePath.startsWith("/content/dam/")) {
            // strip off .html extension and query string if present
            resourcePath = resourcePath.replaceAll("\\.html(\\?.*)?$", "");
        }
        return resourcePath;
    }

    static String determinePathToEvaluate(String path, boolean evaluateURI, SlingHttpServletRequest request) {
        return (evaluateURI && request != null) ? request.getRequestURI() : path;
    }

    public Map<String, RedirectRule> getPathRules() {
        return pathRules;
    }

    public Map<Pattern, RedirectRule> getPatternRules() {
        return patternRules;
    }

    public String getPath() {
        return path;
    }

    public String getName() {
        return name;
    }

    /**
     * Match a request path to a redirect configuration
     * Performs two tries:
     * <ol>
     *     <li>Match by exact path. This is O(1) lookup in a hashtable keyed by path</li>
     *     <li>Match by a regular expression. This is O(N) linear lookup in a list of rules keyed by their regex patterns</li>
     * </ol>
     *
     * @param requestPath   the request to match
     * @return  match or null
     * @see #match(String, String, SlingHttpServletRequest)
     */
    public RedirectMatch match(String requestPath) {
        return match(requestPath, "", null);
    }

    /**
     * Match a request path to a redirect configuration
     * Performs two tries:
     * <ol>
     *     <li>Match by exact path. This is O(1) lookup in a hashtable keyed by path</li>
     *     <li>Match by a regular expression. This is O(N) linear lookup in a list of rules keyed by their regex patterns</li>
     * </ol>
     *
     * @param resourcePath   the request to match
     * @param contextPrefix the optional context prefix to take into account
     * @param request the current sling request
     * @return  match or null
     */
    public RedirectMatch match(String resourcePath, String contextPrefix, SlingHttpServletRequest request) {
        String normalizedPath = normalizePath(resourcePath);
        RedirectMatch match = null;
        RedirectRule rule = getPathRule(normalizedPath, contextPrefix);
        if(rule == null && hasNonRegexRequestURIRules()){
            // there are request URI rules. Check is any mathes
            String pathToEvaluate = determinePathToEvaluate(normalizedPath, true, request);
            rule = getPathRule(pathToEvaluate, contextPrefix);
        }
        if (rule != null) {
            match = new RedirectMatch(rule, null);
        } else {
            for (Map.Entry<Pattern, RedirectRule> entry : getPatternRules().entrySet()) {
                boolean evaluateURI = entry.getValue().getEvaluateURI();
                String pathToEvaluate = determinePathToEvaluate(normalizedPath, evaluateURI, request);
                Matcher m = getRuleMatch(entry.getKey(), pathToEvaluate, contextPrefix);
                if (m.matches()) {
                    match = new RedirectMatch(entry.getValue(), m);
                    break;
                }
            }
        }
        return match;
    }

    /**
     * Utility method that gets the pattern rule taking an optional context prefix into account
     * @param rulePattern the regex pattern to match the path
     * @param pathToEvaluate the path to evaluate for redirects
     * @param contextPrefix the optional context prefix
     * @return the matcher associated with the rule
     */
    private Matcher getRuleMatch(Pattern rulePattern, String pathToEvaluate, String contextPrefix) {
        if("".equals(contextPrefix)) {
            return rulePattern.matcher(pathToEvaluate);
        } else {
            //we add the context prefix to the pattern since a pattern might be too broad otherwise,
            //i.e. "/(.*)" will match anything
            if(!rulePattern.toString().startsWith(contextPrefix)) {
                rulePattern = RedirectRule.toRegex(contextPrefix + rulePattern.toString());
            }
            Matcher matcher = rulePattern.matcher(pathToEvaluate);
            if(!matcher.matches()) {
                if (pathToEvaluate.startsWith(contextPrefix)) {
                    matcher = rulePattern.matcher(pathToEvaluate.replace(contextPrefix, ""));
                } else {
                    matcher = rulePattern.matcher(contextPrefix + pathToEvaluate);
                }
            }
            return matcher;
        }
    }

    /**
     * Utility method that gets the path rule taking an optional context prefix into account
     * @param normalizedPath the normalized path
     * @param contextPrefix the optional context prefix
     * @return the associated rule for the path or null
     */
    private RedirectRule getPathRule(String normalizedPath, String contextPrefix) {
        if("".equals(contextPrefix)) {
            return getPathRules().get(normalizedPath);
        } else {
            RedirectRule rule = getPathRules().get(normalizedPath);
            if(rule == null) {
                if(normalizedPath.startsWith(contextPrefix)) {
                    rule = getPathRules().get(normalizedPath.replace(contextPrefix, ""));
                } else {
                    rule = getPathRules().get(contextPrefix + normalizedPath);
                }
            }
            return rule;
        }
    }

    private boolean hasNonRegexRequestURIRules() {
        return this.nonRegexRequestURIRules;
    }

}
