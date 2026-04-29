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
package com.adobe.acs.commons.redirects.models;

/**
 * Controls how the source path of a redirect rule is interpreted during matching.
 */
public enum MatchType {
    /**
     * Default behaviour: a trailing {@code *} is replaced with {@code (.*)},
     * and the result is compiled as a regex only when it contains at least one
     * capturing group; otherwise the source is treated as an exact path.
     */
    AUTO_DETECT,

    /**
     * The source is always treated as a literal exact path.
     * No regex compilation is performed, even if the source contains regex metacharacters.
     */
    PLAIN_TEXT,

    /**
     * The source is always compiled as a regular expression.
     * No trailing-{@code *} conversion is applied, and capturing groups are not required.
     * This allows patterns such as {@code /content/path/.*} that redirect to a fixed target.
     */
    REGEX
}
