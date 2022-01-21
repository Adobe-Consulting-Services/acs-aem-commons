/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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
package com.adobe.acs.commons.audit_log_search;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;

import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;

import com.adobe.granite.security.user.UserProperties;
import com.adobe.granite.security.user.UserPropertiesManager;
import com.adobe.granite.security.user.UserPropertiesService;

/**
 * Simple POJO for audit log requests. Handles some of the crufty code around
 * loading and generating the query.
 */
public class AuditLogSearchRequest {

    private static final FastDateFormat HTML5_DATETIME_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm", TimeZone.getTimeZone("GMT"));
    private static final FastDateFormat QUERY_DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss", TimeZone.getTimeZone("GMT"));

    private static String getJcrSqlDate(Date date) {
        return QUERY_DATE_FORMAT.format(date) + ".000Z";
    }

    private final String contentRoot;
    private final boolean includeChildren;
    private final String type;
    private final String user;
    private final Date startDate;
    private final Date endDate;
    private final String order;
    private Map<String, String> userNames = new HashMap<String, String>();

    private Map<String, String> userPaths = new HashMap<String, String>();

    /**
     * Constructs a new AuditLogSearchRequest from the SlingHttpServletRequest
     *
     * @param request
     *            yep, that's a request... guess what it does
     * @throws ParseException
     *             an exception occurred parsing the start / end date
     */
    public AuditLogSearchRequest(SlingHttpServletRequest request) throws ParseException {
        contentRoot = request.getParameter("contentRoot");
        includeChildren = "true".equals(request.getParameter("includeChildren"));
        type = request.getParameter("type");
        user = request.getParameter("user");
        startDate = loadDate(request.getParameter("startDate"));
        endDate = loadDate(request.getParameter("endDate"));
        order = request.getParameter("order");
    }

    public String getContentRoot() {
        return contentRoot;
    }

    public Date getEndDate() {
        return Optional.ofNullable(endDate)
                .map(date -> (Date) date.clone())
                .orElse(null);
    }

    public String getOrder() {
        return order;
    }

    public String getQueryParameters() {
        List<String> expressions = new ArrayList<String>();

        if (!StringUtils.isEmpty(type)) {
            expressions.add("[cq:type]='" + StringEscapeUtils.escapeSql(type) + "'");
        }
        if (!StringUtils.isEmpty(user)) {
            expressions.add("[cq:userid]='" + StringEscapeUtils.escapeSql(user) + "'");
        }
        if (StringUtils.isNotEmpty(contentRoot)) {
            if (includeChildren) {
                expressions.add("[cq:path] LIKE '" + StringEscapeUtils.escapeSql(contentRoot) + "%'");
            } else {
                expressions.add("[cq:path]='" + StringEscapeUtils.escapeSql(contentRoot) + "'");
            }
        }
        if (startDate != null) {
            expressions.add("[cq:time] > CAST('" + getJcrSqlDate(startDate) + "' AS DATE)");
        }
        if (endDate != null) {
            expressions.add("[cq:time] < CAST('" + getJcrSqlDate(endDate) + "' AS DATE)");
        }
        String query = StringUtils.join(expressions, " AND ");
        if (!StringUtils.isEmpty(order)) {
            query += " ORDER BY " + order;
        }
        return query;
    }

    public Date getStartDate() {
        return Optional.ofNullable(startDate)
                .map(date -> (Date) date.clone())
                .orElse(null);
    }

    public String getType() {
        return type;
    }

    public String getUser() {
        return user;
    }

    public String getUserName(ResourceResolver resolver, String userId) throws RepositoryException {
        if (!userNames.containsKey(userId)) {
            final UserPropertiesManager upm = resolver.adaptTo(UserPropertiesManager.class);
            UserProperties userProperties = upm.getUserProperties(userId, UserPropertiesService.PRIVATE_PROFILE);
            String name = userId;
            if (userProperties != null && !StringUtils.isEmpty(userProperties.getDisplayName())) {
                name = userProperties.getDisplayName();
            }
            userNames.put(userId, name);
        }
        return userNames.get(userId);
    }

    public String getUserPath(ResourceResolver resolver, String userId)
            throws UnsupportedRepositoryOperationException, RepositoryException {
        if (!userPaths.containsKey(userId)) {
            final UserManager userManager = resolver.adaptTo(UserManager.class);
            final Authorizable usr = userManager.getAuthorizable(userId);
            if (usr != null) {
                userPaths.put(userId, usr.getPath());
            }
        }
        return userPaths.get(userId);
    }

    public boolean isIncludehildren() {
        return includeChildren;
    }

    private Date loadDate(String dateStr) throws ParseException {
        Date date = null;
        if (!StringUtils.isEmpty(dateStr)) {
            date = HTML5_DATETIME_FORMAT.parse(dateStr);
        }
        return date;
    }

    @Override
    public String toString() {
        return "AuditLogSearchRequest [contentRoot=" + contentRoot + ", includeChildren=" + includeChildren + ", type="
                + type + ", user=" + user + ", startDate=" + startDate + ", endDate=" + endDate + ", order=" + order
                + ", userNames=" + userNames + ", userPaths=" + userPaths + "]";
    }

}
