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

    public RedirectResourceBuilder setNotes(String note) {
        props.put(NOTE_PROPERTY_NAME, note);
        return this;
    }

    public RedirectResourceBuilder setUntilDate(Calendar calendar) {
        props.put(UNTIL_DATE_PROPERTY_NAME, calendar);
        return this;
    }

    public RedirectResourceBuilder setCreatedBy(String createdBy) {
        props.put(CREATED_BY, createdBy);
        return this;
    }

    public RedirectResourceBuilder setModifiedBy(String modifiedBy) {
        props.put(MODIFIED_BY, modifiedBy);
        return this;
    }

    public RedirectResourceBuilder setTagIds(String[] tagIds) {
        props.put(TAGS, tagIds);
        return this;
    }

    public RedirectResourceBuilder setContextPrefixIgnored(boolean contextPrefixIgnored) {
        props.put(CONTEXT_PREFIX_IGNORED, contextPrefixIgnored);
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

    public Resource build() throws PersistenceException {
        ContentBuilder cb = context.create();
        Resource configResource = ResourceUtil.getOrCreateResource(
                context.resourceResolver(), configPath, REDIRECT_RULE_RESOURCE_TYPE, null, true);
        if(nodeName == null) {
            nodeName = ResourceUtil.createUniqueChildName(configResource, "rule-");
        }
        return cb.resource(configResource, nodeName, props);
    }
}
