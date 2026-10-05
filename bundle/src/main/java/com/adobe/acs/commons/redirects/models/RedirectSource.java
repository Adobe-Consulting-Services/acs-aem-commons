/*
 * ACS AEM Commons
 *
 * Copyright (C) 2026 Adobe
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
package com.adobe.acs.commons.redirects.models;

import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a redirect source, which can be either a plain text string or a regex pattern.
 * This is postprocessed from the source property of a redirect rule, based on the match type.
 */
public class RedirectSource {

    private final @NotNull String text;
    private final Pattern pattern;
    private final boolean ignoreContextPrefix;
    private final boolean evaluateURI;
    
    private static final Logger log = LoggerFactory.getLogger(RedirectSource.class);

    public RedirectSource(String input, MatchType matchType, boolean ignoreContextPrefix, boolean caseInsensitive, boolean evaluateURI) {
        this.text = input.trim();
        if (matchType == MatchType.PLAIN_TEXT) {
            this.pattern = null;
        } else if (matchType == MatchType.REGEX) {
            this.pattern = toRegexForced(input, caseInsensitive);
        } else {
            // AUTO_DETECT: existing behaviour
            String regex = input;
            if (regex.endsWith("*")) {
                regex = regex.replaceAll("\\*$", "(.*)");
            }
            this.pattern = toRegex(regex, caseInsensitive);
        }
        this.ignoreContextPrefix = ignoreContextPrefix;
        this.evaluateURI = evaluateURI;
    }

    public @NotNull String getText() {
        return text;
    }

    public Optional<Pattern> getPattern() {
        return Optional.ofNullable(pattern);
    }

    public boolean isIgnoreContextPrefix() {
        return ignoreContextPrefix;
    }

    public boolean isEvaluateURI() {
        return evaluateURI;
    }

    static Pattern toRegex(String src, boolean nc) {
        Pattern ptrn;
        try {
            int flags = 0;
            if (nc) {
                flags = Pattern.CASE_INSENSITIVE;
            }
            ptrn = Pattern.compile(src, flags);
            int groupCount = ptrn.matcher("").groupCount();
            if (groupCount == 0) {
                ptrn = null;
            }
        } catch (PatternSyntaxException e) {
            log.error("invalid regex: {}", src);
            ptrn = null;
        }
        return ptrn;
    }

    /**
     * Compile src as a regex without requiring capturing groups.
     * Used when matchType is {@link MatchType#REGEX}.
     */
    static Pattern toRegexForced(String src, boolean nc) {
        try {
            int flags = nc ? Pattern.CASE_INSENSITIVE : 0;
            return Pattern.compile(src, flags);
        } catch (PatternSyntaxException e) {
            log.error("invalid regex: {}", src);
            return null;
        }
    }

}
