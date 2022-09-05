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
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
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
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Arrays;

import static com.adobe.acs.commons.redirects.filter.RedirectFilter.ACS_REDIRECTS_RESOURCE_TYPE;
import static com.adobe.acs.commons.redirects.filter.RedirectFilter.REDIRECT_RULE_RESOURCE_TYPE;
import static com.adobe.acs.commons.redirects.models.RedirectRule.SOURCE_PROPERTY_NAME;
import static org.apache.jackrabbit.JcrConstants.JCR_MIXINTYPES;
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
    private static final String MIX_CREATED = "mix:created";
    private static final String MIX_LAST_MODIFIED = "mix:lastModified";

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        String path = request.getParameter("path");
        Resource storageRoot = request.getResourceResolver().getResource(path);
        log.debug("Updating redirect maps at {}", storageRoot.getPath());
        Map<String, Resource> jcrRules = getRules(storageRoot); // rules stored in crx
        Collection<RedirectRule> xlsRules;
        try (InputStream is = getFile(request)) {
            xlsRules = readEntries(is); // rules read from excel
        }
        if (!xlsRules.isEmpty()) {
            update(storageRoot, xlsRules, jcrRules);
        }
    }

    /**
     * @return redirects keyed by source path
     */
    Map<String, Resource> getRules(Resource resource) {
        Map<String, Resource> rules = new LinkedHashMap<>();
        for (Resource res : resource.getChildren()) {
            if(res.isResourceType(REDIRECT_RULE_RESOURCE_TYPE)){
                String src = res.getValueMap().get(SOURCE_PROPERTY_NAME, String.class);
                rules.put(src, res);
            }
        }
        return rules;
    }

    /**
     *
     * @param root          root resource, e.g. /conf/global/settings/redirects
     * @param xlsRules      redirects read from an Excel spreadhseet
     * @param jcrRedirects  existing redirect nodes keyed by the source path.
     *                      We assume that the source path is unique.
     */
    void update(Resource root, Collection<RedirectRule> xlsRules, Map<String, Resource> jcrRedirects) throws PersistenceException {
        ResourceResolver resolver = root.getResourceResolver();
        for (RedirectRule rule : xlsRules) {
            Map<String, Object> props = new HashMap<>();
            props.put(SOURCE_PROPERTY_NAME, rule.getSource());
            props.put(RedirectRule.TARGET_PROPERTY_NAME, rule.getTarget());
            props.put(RedirectRule.STATUS_CODE_PROPERTY_NAME, String.valueOf(rule.getStatusCode()));
            if (rule.getUntilDate() != null) {
                props.put(RedirectRule.UNTIL_DATE_PROPERTY_NAME, GregorianCalendar.from(rule.getUntilDate()) );
            }
            if(rule.getNote() != null){
                props.put(RedirectRule.NOTE_PROPERTY_NAME, rule.getNote());
            }
            props.put(RedirectRule.CONTEXT_PREFIX_IGNORED, rule.getContextPrefixIgnored());
            props.put(PROPERTY_RESOURCE_TYPE, REDIRECT_RULE_RESOURCE_TYPE);

            Resource redirect = jcrRedirects.get(rule.getSource());
            if(redirect == null){
                // add mix:created, AEM will initialize jcr:created and jcr:createdBy from the current session
                props.put(JCR_MIXINTYPES, MIX_CREATED);
                String nodeName = ResourceUtil.createUniqueChildName(root, "redirect-rule-");
                resolver.create(root, nodeName, props);
            } else {
                // add mix:lastModified so that AEM updates jcr:lastModified and jcr:lastModifiedBy
                ValueMap valueMap = redirect.adaptTo(ModifiableValueMap.class);
                String[] mixins = valueMap.get(JCR_MIXINTYPES, String[].class);
                Collection<String> mset = mixins == null ? new HashSet<>() : new HashSet<>(Arrays.asList(mixins));
                mset.add(MIX_LAST_MODIFIED);
                props.put(JCR_MIXINTYPES, mset.toArray(new String[0]));
                valueMap.putAll(props);
            }

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
                Calendar untilDate = null;
                if (DateUtil.isCellDateFormatted(c4)) {
                    try {
                        Instant instant = DateUtil.getJavaDate(c4.getNumericCellValue()).toInstant();
                        ZonedDateTime zdate = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
                        untilDate = GregorianCalendar.from(zdate);
                    } catch (Exception e) {
                        log.error("cannot set data from {}", c4.toString(), e);
                    }
                }
                Cell c5 = row.getCell(4);
                String note = null;
                if(c5 != null){
                    note = c5.getStringCellValue();
                }
                Cell c6 = row.getCell(5);
                boolean ignoreContextPrefix = (c6 != null && c6.getBooleanCellValue());
                rules.add(new RedirectRule(source, target, statusCode, untilDate, note, ignoreContextPrefix));

                // cell 6 holds jcr:createdBy
            } else {
                first = false;
            }
        }
        log.debug("{} rules read from spreadsheet", rules.size());
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
