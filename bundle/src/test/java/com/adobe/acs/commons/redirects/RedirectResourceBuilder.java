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
package com.adobe.acs.commons.redirects;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.testing.mock.sling.builder.ContentBuilder;
import org.apache.sling.testing.mock.sling.junit.SlingContext;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import static com.adobe.acs.commons.redirects.filter.RedirectFilter.REDIRECT_RULE_RESOURCE_TYPE;
import static com.adobe.acs.commons.redirects.models.RedirectRule.*;

public class RedirectResourceBuilder {
    public static final String DEFAULT_CONF_PATH = "/conf/global/settings/redirects";

    private final SlingContext context;
    private final String configPath;
    private final Map<String, Object> props;
    private String nodeName;

    public RedirectResourceBuilder(SlingContext context, String configPath) {
        this.context = context;
        this.configPath = configPath;
        this.props = new HashMap<>();
        this.props.put("sling:resourceType", REDIRECT_RULE_RESOURCE_TYPE);
    }

    public RedirectResourceBuilder(SlingContext context) {
        this(context, DEFAULT_CONF_PATH);
    }

    public RedirectResourceBuilder setSource(String source) {
        props.put(SOURCE_PROPERTY_NAME, source);
        return this;
    }

    public RedirectResourceBuilder setTarget(String target) {
        props.put(TARGET_PROPERTY_NAME, target);
        return this;
    }

    public RedirectResourceBuilder setStatusCode(int statusCode) {
        props.put(STATUS_CODE_PROPERTY_NAME, statusCode);
        return this;
    }

    public RedirectResourceBuilder setEvaluateURI(boolean evaluateURI) {
        props.put(EVALUATE_URI_PROPERTY_NAME, evaluateURI);
        return this;
    }

    public RedirectResourceBuilder setNotes(String note) {
        props.put(NOTE_PROPERTY_NAME, note);
        return this;
    }

    public RedirectResourceBuilder setUntilDate(Calendar calendar) {
        props.put(UNTIL_DATE_PROPERTY_NAME, calendar);
        return this;
    }

    public RedirectResourceBuilder setEffectiveFrom(Calendar calendar) {
        props.put(EFFECTIVE_FROM_PROPERTY_NAME, calendar);
        return this;
    }

    public RedirectResourceBuilder setCreatedBy(String createdBy) {
        props.put(CREATED_BY_PROPERTY_NAME, createdBy);
        return this;
    }

    public RedirectResourceBuilder setModifiedBy(String modifiedBy) {
        props.put(MODIFIED_BY_PROPERTY_NAME, modifiedBy);
        return this;
    }

    public RedirectResourceBuilder setModified(Calendar modified) {
        props.put(MODIFIED_PROPERTY_NAME, modified);
        return this;
    }

    public RedirectResourceBuilder setCreated(Calendar created) {
        props.put(CREATED_PROPERTY_NAME, created);
        return this;
    }

    public RedirectResourceBuilder setTagIds(String[] tagIds) {
        props.put(TAGS_PROPERTY_NAME, tagIds);
        return this;
    }

    public RedirectResourceBuilder setContextPrefixIgnored(boolean contextPrefixIgnored) {
        props.put(CONTEXT_PREFIX_IGNORED_PROPERTY_NAME, contextPrefixIgnored);
        return this;
    }

    public RedirectResourceBuilder setCacheControlHeader(String value) {
        props.put(CACHE_CONTROL_HEADER_NAME, value);
        return this;
    }

    public RedirectResourceBuilder setProperty(String key, Object value) {
        props.put(key, value);
        return this;
    }

    public RedirectResourceBuilder setNodeName(String name) {
        this.nodeName = name;
        return this;
    }

    public RedirectResourceBuilder setCaseInsensitive(boolean nc) {
        props.put(CASE_INSENSITIVE_PROPERTY_NAME, nc);
        return this;
    }

    public RedirectResourceBuilder setPreserveQueryString(String value) {
        props.put(PRESERVE_QUERY_STRING, value);
        return this;
    }

    public Resource build() throws PersistenceException {
        ContentBuilder cb = context.create();
        Resource configResource = ResourceUtil.getOrCreateResource(
                context.resourceResolver(), configPath, REDIRECT_RULE_RESOURCE_TYPE, null, true);
        if(nodeName == null) {
            nodeName = ResourceUtil.createUniqueChildName(configResource, "rule");
        }
        return cb.resource(configResource, nodeName, props);
    }
}
