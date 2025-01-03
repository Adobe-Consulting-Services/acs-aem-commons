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
package com.adobe.acs.commons.redirects.servlets;

import com.adobe.acs.commons.redirects.models.ImportLog;
import com.adobe.acs.commons.redirects.servlets.impl.CsvRedirectImporter;
import com.adobe.acs.commons.redirects.servlets.impl.ExcelRedirectImporter;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.jackrabbit.JcrConstants;
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
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Arrays;
import java.util.UUID;

import static com.adobe.acs.commons.redirects.filter.RedirectFilter.ACS_REDIRECTS_RESOURCE_TYPE;
import static com.adobe.acs.commons.redirects.models.RedirectRule.SOURCE_PROPERTY_NAME;
import static com.adobe.acs.commons.redirects.models.Redirects.readRedirects;
import static org.apache.jackrabbit.JcrConstants.JCR_MIXINTYPES;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;
import static org.apache.jackrabbit.JcrConstants.NT_UNSTRUCTURED;

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
    static final String CONTENT_TYPE_CSV = "text/csv";
    static final String CONTENT_TYPE_EXCEL = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private static final Logger log = LoggerFactory.getLogger(ImportRedirectMapServlet.class);
    private static final long serialVersionUID = -3564475196678277711L;
    private static final String MIX_CREATED = "mix:created";
    private static final String MIX_LAST_MODIFIED = "mix:lastModified";
    private static final String AUDIT_LOG_FOLDER = "/var/acs-commons/redirects";
    private static final int SHARD_SIZE = 1000;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");

        String path = request.getParameter("path");
        boolean replace = request.getParameter("replace") != null;
        Resource storageRoot = request.getResourceResolver().getResource(path);
        log.debug("Updating redirect maps at {}", storageRoot.getPath());
        Map<String, Resource> jcrRules;
        if(replace){
            jcrRules = Collections.emptyMap();
            for(Resource ch : storageRoot.getChildren()){
                ch.getResourceResolver().delete(ch);
            }
        } else {
            jcrRules = getRules(storageRoot); // rules stored in crx
        }

        ImportLog auditLog = new ImportLog();
        Collection<Map<String, Object>> rules;

        RequestParameter fileParam = getFileRequestParameter(request);

        String contentType = fileParam.getContentType();
        try (InputStream is = fileParam.getInputStream()) {
            if (CONTENT_TYPE_CSV.equals(contentType)) {
                rules = new CsvRedirectImporter(auditLog).read(is);
            } else if (CONTENT_TYPE_EXCEL.equals(contentType)) {
                rules = new ExcelRedirectImporter(auditLog).read(is);
            } else {
                throw new IOException("Unsupported file type: " + contentType);
            }
        }

        if (!rules.isEmpty()) {
            update(storageRoot, rules, jcrRules);
        }
        persistAuditLog(request.getResourceResolver(), auditLog, response.getWriter());
    }

    /**
     * read redirects stored in AEM
     *
     * @return redirect nodes keyed by source path
     */
    Map<String, Resource> getRules(Resource resource) {
        List<Resource> redirects = readRedirects(resource);
        Map<String, Resource> rulesByPathMap = new LinkedHashMap<>();
        for(Resource res : redirects){
            String src = res.getValueMap().get(SOURCE_PROPERTY_NAME, String.class);
            rulesByPathMap.put(src, res);
        }
        return rulesByPathMap;
    }

    /**
     * Update redirects in AEM
     *
     * @param root         root resource, e.g. /conf/global/settings/redirects
     * @param rules     redirects read from an Excel spreadsheet
     * @param jcrRedirects existing redirect nodes keyed by the source path.
     *                     We assume that the source path is unique.
     */
    void update(Resource root, Collection<Map<String, Object>> rules, Map<String, Resource> jcrRedirects) throws PersistenceException {
        ResourceResolver resolver = root.getResourceResolver();
        long t0 = System.currentTimeMillis();
        if(rules.size() > SHARD_SIZE){
            int count = 0;
            for (Map<String, Object> props : rules) {
                count++;

                String shardName = "shard-" + count / SHARD_SIZE;
                String sourcePath = (String) props.get(SOURCE_PROPERTY_NAME);
                Resource shard = root.getChild(shardName);
                if(shard == null){
                    shard = resolver.create(root, shardName, Collections.singletonMap(JCR_PRIMARYTYPE, NT_UNSTRUCTURED));
                }
                Resource redirect = getOrCreateRedirect(shard, sourcePath, props, jcrRedirects);
                log.debug("rule[{}]: {}", count, redirect.getPath());
                if(count % SHARD_SIZE == 0) resolver.commit();
            }
        } else {
            for (Map<String, Object> props : rules) {
                String sourcePath = (String) props.get(SOURCE_PROPERTY_NAME);
                Resource redirect = getOrCreateRedirect(root, sourcePath, props, jcrRedirects);
                log.debug("rule: {}", redirect.getPath());
            }
            resolver.commit();
        }
        log.debug("{} rules imported in {}ms", rules.size(), System.currentTimeMillis() - t0);
    }

    private Resource getOrCreateRedirect(Resource root, String sourcePath, Map<String, Object> props, Map<String, Resource> jcrRedirects) throws PersistenceException {
        Resource redirect = jcrRedirects.get(sourcePath);
        if (redirect == null) {
            // add mix:created, AEM will initialize jcr:created and jcr:createdBy from the current session
            props.put(JCR_MIXINTYPES, MIX_CREATED);
            String nodeName = ResourceUtil.createUniqueChildName(root, "redirect-rule-");
            redirect = root.getResourceResolver().create(root, nodeName, props);
        } else {
            // add mix:lastModified so that AEM updates jcr:lastModified and jcr:lastModifiedBy
            ValueMap valueMap = redirect.adaptTo(ModifiableValueMap.class);
            if (valueMap == null) {
                throw new PersistenceException("Cannot modify properties of " + redirect.getPath());
            }
            String[] mixins = valueMap.get(JCR_MIXINTYPES, String[].class);
            Collection<String> mset = mixins == null ? new HashSet<>() : new HashSet<>(Arrays.asList(mixins));
            mset.add(MIX_LAST_MODIFIED);
            props.put(JCR_MIXINTYPES, mset.toArray(new String[0]));
            valueMap.putAll(props);
        }
        return redirect;
    }

    /**
     * Save import log in /var/acs-commons/redirects/$UUID
     */
    private void persistAuditLog(ResourceResolver resourceResolver, ImportLog auditLog, PrintWriter out) throws IOException {
        ObjectMapper om = new ObjectMapper();

        ResourceUtil.getOrCreateResource(resourceResolver, AUDIT_LOG_FOLDER,
                Collections.singletonMap(JcrConstants.JCR_PRIMARYTYPE, JcrResourceConstants.NT_SLING_FOLDER), JcrResourceConstants.NT_SLING_FOLDER, false);

        String auditNodePath = AUDIT_LOG_FOLDER + "/" + UUID.randomUUID();
        Resource ntFile = ResourceUtil.getOrCreateResource(resourceResolver, auditNodePath,
                Collections.singletonMap(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FILE), null, false);

        auditLog.setPath(ntFile.getPath());

        String json = om.writeValueAsString(auditLog);
        Map<String, Object> props = new HashMap<>();
        props.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_RESOURCE);
        props.put(JcrConstants.JCR_MIXINTYPES, MIX_CREATED);
        props.put(JcrConstants.JCR_MIMETYPE, "application/json");

        props.put(JcrConstants.JCR_DATA, new ByteArrayInputStream(json.getBytes()));

        ResourceUtil.getOrCreateResource(resourceResolver, auditNodePath + "/" + JcrConstants.JCR_CONTENT,
                props, null, false);

        resourceResolver.commit();

        out.println(json);
    }

    @Deprecated
    public static InputStream getFile(SlingHttpServletRequest request) throws IOException {
        return getFileRequestParameter(request).getInputStream();
    }

    static RequestParameter getFileRequestParameter(SlingHttpServletRequest request) throws IOException {
        for (RequestParameter param : request.getRequestParameterList()) {
            if (!param.isFormField()) {
                return param;
            }
        }
        throw new IOException("No file uploaded");
    }
 }
