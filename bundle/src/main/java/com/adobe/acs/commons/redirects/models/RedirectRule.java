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

import com.adobe.granite.security.user.util.AuthorizableUtil;
import com.day.cq.tagging.Tag;
import com.day.cq.tagging.TagManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Named;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Model(adaptables = {SlingHttpServletRequest.class, Resource.class}, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class RedirectRule {
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String SOURCE_PROPERTY_NAME = "source";
    public static final String TARGET_PROPERTY_NAME = "target";
    public static final String STATUS_CODE_PROPERTY_NAME = "statusCode";
    public static final String UNTIL_DATE_PROPERTY_NAME = "untilDate";
    public static final String EFFECTIVE_FROM_PROPERTY_NAME = "effectiveFrom";
    public static final String NOTE_PROPERTY_NAME = "note";
    public static final String CONTEXT_PREFIX_IGNORED_PROPERTY_NAME = "contextPrefixIgnored";
    public static final String EVALUATE_URI_PROPERTY_NAME = "evaluateURI";
    public static final String CREATED_PROPERTY_NAME = "jcr:created";
    public static final String CREATED_BY_PROPERTY_NAME = "jcr:createdBy";
    public static final String MODIFIED_PROPERTY_NAME = "jcr:lastModified";
    public static final String MODIFIED_BY_PROPERTY_NAME = "jcr:lastModifiedBy";
    public static final String TAGS_PROPERTY_NAME = "cq:tags";
    public static final String CACHE_CONTROL_HEADER_NAME = "cacheControlHeader";
    public static final String CASE_INSENSITIVE_PROPERTY_NAME = "caseInsensitive";
    public static final String REDIRECT_RESOURCE_REQUEST_ATTRIBUTE = "redirectResource";
    public static final String PRESERVE_QUERY_STRING = "preserveQueryString";
    public static final String SHARD_NAME_PREFIX = "shard-";

    @ValueMapValue
    private String source;

    @ValueMapValue
    private String target;

    @ValueMapValue
    private Integer statusCode;

    @ValueMapValue
    private boolean evaluateURI;

    @ValueMapValue
    private Calendar untilDate;

    @ValueMapValue
    private Calendar effectiveFrom;

    @ValueMapValue
    private String note;

    @ValueMapValue
    private boolean contextPrefixIgnored;

    @ValueMapValue(name = TAGS_PROPERTY_NAME)
    private String[] tagIds;

    @ValueMapValue(name = CREATED_BY_PROPERTY_NAME)
    private String createdBy;

    @ValueMapValue(name = MODIFIED_BY_PROPERTY_NAME)
    private String modifiedBy;

    @ValueMapValue(name = MODIFIED_PROPERTY_NAME)
    private Calendar modified;

    @ValueMapValue(name = CREATED_PROPERTY_NAME)
    private Calendar created;

    @ValueMapValue(name = CACHE_CONTROL_HEADER_NAME)
    private String cacheControlHeader;

    @ValueMapValue(name = CASE_INSENSITIVE_PROPERTY_NAME)
    private boolean caseInsensitive;

    @ValueMapValue(name = PRESERVE_QUERY_STRING)
    private String preserveQueryString;

    @Self
    private Resource resource;

    @RequestAttribute
    @Named(REDIRECT_RESOURCE_REQUEST_ATTRIBUTE)
    private Resource redirectResource;

    private Pattern ptrn;

    private SubstitutionElement[] substitutions;
    private String defaultCacheControlHeader = null;

    @PostConstruct
    protected void init() {
        if (redirectResource != null) {
            resource = redirectResource;

            source = resource.getValueMap().get(SOURCE_PROPERTY_NAME, String.class);
            target = resource.getValueMap().get(TARGET_PROPERTY_NAME, String.class);
            statusCode = resource.getValueMap().get(STATUS_CODE_PROPERTY_NAME, Integer.class);
            evaluateURI = resource.getValueMap().get(EVALUATE_URI_PROPERTY_NAME, false);
            untilDate = resource.getValueMap().get(UNTIL_DATE_PROPERTY_NAME, Calendar.class);
            effectiveFrom = resource.getValueMap().get(EFFECTIVE_FROM_PROPERTY_NAME, Calendar.class);
            note = resource.getValueMap().get(NOTE_PROPERTY_NAME, String.class);
            contextPrefixIgnored = resource.getValueMap().get(CONTEXT_PREFIX_IGNORED_PROPERTY_NAME, false);
            tagIds = resource.getValueMap().get(TAGS_PROPERTY_NAME, String[].class);
            createdBy = resource.getValueMap().get(CREATED_BY_PROPERTY_NAME, String.class);
            modifiedBy = resource.getValueMap().get(MODIFIED_BY_PROPERTY_NAME, String.class);
            modified = resource.getValueMap().get(MODIFIED_PROPERTY_NAME, Calendar.class);
            created = resource.getValueMap().get(CREATED_PROPERTY_NAME, Calendar.class);
            cacheControlHeader = resource.getValueMap().get(CACHE_CONTROL_HEADER_NAME, String.class);
            caseInsensitive = resource.getValueMap().get(CASE_INSENSITIVE_PROPERTY_NAME, false);
            preserveQueryString = resource.getValueMap().get(PRESERVE_QUERY_STRING, String.class);
        }

        if (StringUtils.isBlank(source) || StringUtils.isBlank(target) || statusCode == null) {
            throw new IllegalArgumentException("RedirectRule must have a non-empty source, target and status code.");
        }

        source = source.trim();
        target = target.trim();

        String regex = source;
        if (regex.endsWith("*")) {
            regex = regex.replaceAll("\\*$", "(.*)");
        }
        ptrn = toRegex(regex, caseInsensitive);
        substitutions = SubstitutionElement.parse(target);

        String cacheControlProperty = CACHE_CONTROL_HEADER_NAME + "_" + getStatusCode();
        defaultCacheControlHeader = resource.getParent().getValueMap().get(cacheControlProperty, String.class);
    }

    public String getSource() {
        return source;
    }

    public String getTarget() {
        return target;
    }

    public String getNote() {
        return note;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public boolean getEvaluateURI() {
        return evaluateURI;
    }

    public boolean isCaseInsensitive() {
        return caseInsensitive;
    }

    public String getCreatedBy() {
        return AuthorizableUtil.getFormattedName(resource.getResourceResolver(), createdBy);
    }

    public String getModifiedBy() {
        return AuthorizableUtil.getFormattedName(resource.getResourceResolver(), modifiedBy);
    }

    public boolean getContextPrefixIgnored() {
        return contextPrefixIgnored;
    }

    public Pattern getRegex() {
        return ptrn;
    }

    public Calendar getCreated() {
        return created;
    }

    public Calendar getModified() {
        return modified;
    }

    public Calendar getUntilDate() {
        return untilDate;
    }

    public Calendar getEffectiveFrom() {
        return effectiveFrom;
    }

    public String[] getTagIds() {
        return tagIds;
    }

    public String getCacheControlHeader() {
        return cacheControlHeader;
    }

    /**
     * @return default Cache-Control header for this redirect inherited from the parent
     */
    public String getDefaultCacheControlHeader() {
        return defaultCacheControlHeader;
    }

    /**
     * used in the redirect-row component to print tags in HTL
     */
    public List<Tag> getTags() {
        TagManager tagMgr = resource.getResourceResolver().adaptTo(TagManager.class);
        if (tagIds == null || tagMgr == null) {
            return Collections.emptyList();
        }
        List<Tag> tags = new ArrayList<>();
        for (String tagId : tagIds) {
            Tag tag = tagMgr.resolve(tagId);
            if (tag != null) {
                tags.add(tag);
            }
        }
        return tags;
    }

    @Override
    public String toString() {
        return String.format("RedirectRule{source='%s', target='%s', statusCode=%s, untilDate=%s, effectiveFrom=%s, note=%s, evaluateURI=%s,"
                        + "contextPrefixIgnored=%s, tags=%s, created=%s, createdBy=%s, modified=%s, modifiedBy=%s, cacheControlHeader=%s}",
                source, target, statusCode, untilDate, effectiveFrom, note, evaluateURI, contextPrefixIgnored,
                Arrays.toString(tagIds), created, createdBy, modified, modifiedBy, cacheControlHeader);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RedirectRule that = (RedirectRule) o;

        return Objects.equals(source, that.source);
    }

    @Override
    public int hashCode() {
        return source != null ? source.hashCode() : 0;
    }

    public String evaluate(Matcher matcher) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < substitutions.length; i++) {
            buf.append(substitutions[i].evaluate(matcher));
        }
        return buf.toString();
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
     * @return whether the rule has expired, i.e. the 'untilDate' property is before the current time
     * ----[effectiveFrom]---[now]---[untilDate]--->
     * @return
     */
    public RedirectState getState() {
        boolean expired = untilDate != null && untilDate.before(Calendar.getInstance());
        boolean pending = effectiveFrom != null && Calendar.getInstance().before(effectiveFrom);
        ;
        boolean invalid = effectiveFrom != null && untilDate != null && effectiveFrom.after(untilDate);

        if (invalid) {
            return RedirectState.INVALID;
        } else if (expired) {
            return RedirectState.EXPIRED;
        } else if (pending) {
            return RedirectState.PENDING;
        } else {
            return RedirectState.ACTIVE;
        }
    }

    /**
     * @return whether the redirect is published
     */
    public boolean isPublished() {
        Calendar lastReplicated = isSharded()
                ? Objects.requireNonNull(Objects.requireNonNull(resource.getParent()).getParent()).getValueMap().get("cq:lastReplicated", Calendar.class)
                : Objects.requireNonNull(resource.getParent()).getValueMap().get("cq:lastReplicated", Calendar.class);
        boolean isPublished = lastReplicated != null;
        boolean modifiedAfterPublication = isPublished
                && ((modified != null && modified.after(lastReplicated)) || (created != null && created.after(lastReplicated)));
        return isPublished && !modifiedAfterPublication;
    }

    public String getPreserveQueryString() {
        return preserveQueryString;
    }

    /**
     * @return whether the resource path contains {@link RedirectRule#SHARD_NAME_PREFIX}
     */
    public boolean isSharded() {
        return resource.getPath().contains(SHARD_NAME_PREFIX);
    }

}
