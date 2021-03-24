package com.adobe.acs.commons.redirects.models;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RedirectRules {
    private Map<String, RedirectRule> pathRules;
    private Map<Pattern, RedirectRule> patternRules;

    public RedirectRules(Map<String, RedirectRule> pathRules, Map<Pattern, RedirectRule> patternRules) {
        this.pathRules = pathRules;
        this.patternRules = patternRules;
    }

    public Map<String, RedirectRule> getPathRules() {
        return pathRules;
    }

    public Map<Pattern, RedirectRule> getPatternRules() {
        return patternRules;
    }

    public RedirectMatch match(String requestPath) {
        RedirectMatch match = null;
        RedirectRule rule = getPathRules().get(requestPath);
        if (rule != null) {
            match = new RedirectMatch(rule, null);
        } else {
            for (Map.Entry<Pattern, RedirectRule> entry : getPatternRules().entrySet()) {
                Matcher m = entry.getKey().matcher(requestPath);
                if (m.matches()) {
                    match = new RedirectMatch(entry.getValue(), m);
                    break;
                }
            }
        }
        return match;

    }

}
