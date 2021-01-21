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
package com.adobe.acs.commons.redirects.servlets;

import com.adobe.acs.commons.redirects.models.RedirectRule;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.stream.Collectors;

import static com.adobe.acs.commons.redirects.filter.RedirectFilter.ACS_REDIRECTS_RESOURCE_TYPE;
import static com.adobe.acs.commons.redirects.filter.RedirectFilter.REDIRECT_RULE_RESOURCE_TYPE;
import static com.adobe.acs.commons.redirects.filter.RedirectFilter.getRules;
import static com.adobe.granite.comments.AbstractCommentingProvider.JCR_CREATED_BY;
import static org.apache.jackrabbit.JcrConstants.JCR_CREATED;
import static org.apache.sling.api.resource.ResourceResolver.PROPERTY_RESOURCE_TYPE;

/**
 * A servlet to import redirect rules from an Excel spreadsheet
 *
 */
@Component(service = Servlet.class, immediate = true, name = "ImportRedirectMapServlet", property = {
        "sling.servlet.label=ACS AEM Commons - Import Redirects Servlet",
        "sling.servlet.methods=POST",
        "sling.servlet.selectors=import",
        "sling.servlet.resourceTypes=" + ACS_REDIRECTS_RESOURCE_TYPE
})
public class ImportRedirectMapServlet extends SlingAllMethodsServlet {

    private static final Logger log = LoggerFactory.getLogger(ImportRedirectMapServlet.class);
    private static final long serialVersionUID = -3564475196678277711L;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        String path = request.getParameter("path");
        Resource storageRoot = request.getResourceResolver().getResource(path);
        log.debug("Updating redirect maps at {}", storageRoot.getPath());
        Map<String, RedirectRule> jcrRules = getRules(storageRoot)
                .stream().collect(Collectors.toMap(RedirectRule::getSource, r -> r,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new)); // rules stored in crx
        Map<String, RedirectRule> xlsRules;
        try (InputStream is = getFile(request)) {
            xlsRules = readEntries(is)
                    .stream().collect(Collectors.toMap(RedirectRule::getSource, r -> r,
                            (oldValue, newValue) -> oldValue, LinkedHashMap::new)); // rules read from excel
        }
        ArrayList<RedirectRule> rules = new ArrayList<>();
        for (RedirectRule jcrRule : jcrRules.values()) {
            if (xlsRules.containsKey(jcrRule.getSource())) {
                jcrRule = xlsRules.remove(jcrRule.getSource());
            }
            rules.add(jcrRule);
        }
        rules.addAll(xlsRules.values());
        if (!rules.isEmpty()) {
            update(storageRoot, rules);
        }
    }

    void update(Resource root, Collection<RedirectRule> rules) throws PersistenceException {
        ResourceResolver resolver = root.getResourceResolver();
        for (Resource res : root.getChildren()) {
            if(REDIRECT_RULE_RESOURCE_TYPE.equals(res.getResourceType())) {
                resolver.delete(res);
            }
        }
        int idx = 0;
        for (RedirectRule rule : rules) {
            Map<String, Object> props = new HashMap<>();
            props.put(RedirectRule.SOURCE_PROPERTY_NAME, rule.getSource());
            props.put(RedirectRule.TARGET_PROPERTY_NAME, rule.getTarget());
            props.put(RedirectRule.STATUS_CODE_PROPERTY_NAME, String.valueOf(rule.getStatusCode()));
            if (rule.getUntilDate() != null) {
                props.put(RedirectRule.UNTIL_DATE_PROPERTY_NAME, rule.getUntilDate());
            }
            props.put(PROPERTY_RESOURCE_TYPE, REDIRECT_RULE_RESOURCE_TYPE);
            props.put(JCR_CREATED, Calendar.getInstance());
            props.put(JCR_CREATED_BY, resolver.getUserID());
            resolver.create(root, "redirect-rule-" + (++idx), props);
        }
        resolver.commit();
    }

    Collection<RedirectRule> readEntries(InputStream is)
            throws IOException {
        Collection<RedirectRule> rules = new LinkedHashSet<>();
        Workbook wb = new XSSFWorkbook(is);
        Sheet sheet = wb.getSheetAt(0);
        boolean first = true;
        for (Row row : sheet) {
            if (!first) {
                String source = row.getCell(0).getStringCellValue();
                String target = row.getCell(1).getStringCellValue();
                int statusCode = (int) row.getCell(2).getNumericCellValue();
                Cell c4 = row.getCell(3);
                String untilDate = null;
                if (c4 != null) {
                    try {
                        Instant instant = DateUtil.getJavaDate(c4.getNumericCellValue()).toInstant();
                        ZonedDateTime zdate = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
                        untilDate = RedirectRule.DATE_FORMATTER.format(zdate);
                    } catch (Exception e) {
                        log.error("cannot set data from {}", c4.toString(), e);
                    }
                }
                rules.add(new RedirectRule(source, target, statusCode, untilDate));
            } else {
                first = false;
            }
        }
        return rules;
    }

    public static InputStream getFile(SlingHttpServletRequest request) throws IOException {
        InputStream stream = null;
        for (RequestParameter param : request.getRequestParameterList()) {
            if (!param.isFormField()) {
                stream = param.getInputStream();
                break;
            }
        }
        return stream;
    }
}
