/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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

package com.adobe.acs.commons.i18n.impl;

import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.servlets.HttpConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility for handling HTTP ETags.
 */
public class ETagHelper {

    private static final Logger log = LoggerFactory.getLogger(ETagHelper.class);

    /**
     * Handles HTTP ETag for a given request and an entity with a checksum. Checks if the request
     * contains an If-None-Match header and returns 304 Not Modified if one of the entity tags given
     * in the header match the passed etag. Will return <code>true</code> in this case, in which
     * a response has already been sent and further processing should be aborted.
     *
     * Will set the ETag header with the given etag value.
     *
     * @param request http request object
     * @param response http response object
     * @param etag checksum of the entity to deliver
     * @return <code>true</code> if a response was sent already
     */
    public static boolean handleETag(HttpServletRequest request, HttpServletResponse response, String etag) {
        if (etag != null) {
            // ETag header should always be included: for normal responses and for 304 not modified
            response.setHeader(HttpConstants.HEADER_ETAG, new EntityTag(etag).toString());

            EntityTag[] entityTags = EntityTag.parseHeader(request.getHeader("If-None-Match"));
            if (entityTags != null) {
                for (EntityTag tag : entityTags) {
                    if (tag.isWildcard() || etag.equals(tag.getValue())) {
                        response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static class EntityTag {
        private StringBuilder value = new StringBuilder();
        private boolean weak = false;
        private boolean wildcard = false;

        public EntityTag() {
        }

        public EntityTag(String value) {
            this.value.append(value);
        }

        public EntityTag(String value, boolean weak) {
            this.value.append(value);
            this.weak = weak;
        }

        public String getValue() {
            return value.toString();
        }

        public boolean isWeak() {
            return weak;
        }

        public boolean isWildcard() {
            return wildcard;
        }

        public String toString() {
            if (isWeak()) {
                return "W/\"" + getValue() + '"';
            }
            return '"' + getValue() + '"';
        }

        /**
         * Parsing HTTP entity-tag header (e.g. If-None-Match) according to RFC 2616 section 14.26
         */
        public static EntityTag[] parseHeader(String header) {
            if (header == null) {
                return null;
            }
            List<EntityTag> tags = new ArrayList<EntityTag>();
            EntityTag tag = new EntityTag();

            boolean parseQuotedString = false;

            int i = 0;
            while (i < header.length()) {
                char c = header.charAt(i);
                if (parseQuotedString) {
                    // inside quoted string: "...."
                    if (c == '"') {
                        // end of quoted string; add finalized tag to list
                        tags.add(tag);
                        tag = new EntityTag();
                        parseQuotedString = false;
                    } else {
                        tag.value.append(c);
                    }
                } else {
                    // parsing list: "ab", "cd", "ef"
                    if (c == ' ') {
                        // ignore whitespace
                    } else if (c == '*' && tags.size() == 0) {
                        // special case: * wildcard only (no list)
                        tag.value.append('*');
                        tag.wildcard = true;
                        return new EntityTag[] {tag};
                    } else if (c == 'W') {
                        // weak tag: W/"a"
                        if ((i+2) >= header.length() || header.charAt(i+1) != '/' || header.charAt(i+2) != '"') {
                            log.warn("Could not parse http entity-tag header, incorrect weak marker: " + header);
                            return null;
                        }
                        i += 2;
                        parseQuotedString = true;
                    } else if (c == '"') {
                        // begin of quoted string
                        parseQuotedString = true;
                    } else if (c == ',') {
                        // just skip
                    } else {
                        // error
                        log.warn("Could not parse http entity-tag header, contains unquoted string: " + header);
                        return null;
                    }
                }
                i++;
            }
            if (parseQuotedString) {
                // error
                log.warn("Could not parse http entity-tag header, unclosed quoted string: " + header);
                return null;
            }
            return tags.toArray(new EntityTag[tags.size()]);
        }
    }
}
