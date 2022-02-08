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

import com.adobe.acs.commons.redirects.filter.RedirectFilter;
import com.adobe.acs.commons.redirects.filter.RedirectFilterMBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A servlet to create caconfig redirect configurations
 */
@Component(service = Servlet.class, immediate = true, name = "CreateRedirectConfigurationServlet", property = {
        "sling.servlet.label=ACS AEM Commons - Create Redirect Configuration",
        "sling.servlet.methods=POST",
        "sling.servlet.selectors=create",
        "sling.servlet.resourceTypes=" + CreateRedirectConfigurationServlet.REDIRECTS_RESOURCE_PATH
})
public class CreateRedirectConfigurationServlet extends SlingAllMethodsServlet {

    public static final String REDIRECTS_RESOURCE_PATH = "acs-commons/components/utilities/manage-redirects/redirects";

    private static final Logger log = LoggerFactory.getLogger(CreateRedirectConfigurationServlet.class);
    private static final long serialVersionUID = -3564475196678277711L;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL) 
    transient RedirectFilterMBean redirectFilter;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");

        String rootPath = request.getParameter("path");
        if(StringUtils.isEmpty(rootPath) || !rootPath.startsWith("/conf")){
            throw new IllegalArgumentException("root path should be not empty and start with /conf");
        }
        ResourceResolver resolver = request.getResourceResolver();
        Resource root = resolver.getResource(rootPath);
        if(root == null){
            throw new IllegalArgumentException("not found: " + rootPath);
        }

        Map<String, String> rsp = new HashMap<>();
        ObjectMapper om = new ObjectMapper();
        try {
            String bucketName = redirectFilter == null ? RedirectFilter.DEFAULT_CONFIG_BUCKET : redirectFilter.getBucket();
            String configName = redirectFilter == null ? RedirectFilter.DEFAULT_CONFIG_NAME : redirectFilter.getConfigName();
            Resource bucket = root.getChild(bucketName);
            if (bucket == null) {
                bucket = resolver.create(root, bucketName,
                        ImmutableMap.of(JcrConstants.JCR_PRIMARYTYPE, "sling:Folder"));
                log.info("created {}", bucket.getPath());
            }

            Resource config = bucket.getChild(configName);
            if (config == null) {
                config = resolver.create(bucket, configName,
                        ImmutableMap.of(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED,
                                ResourceResolver.PROPERTY_RESOURCE_TYPE, REDIRECTS_RESOURCE_PATH));
                log.info("created {}", config.getPath());
                resolver.commit();
                rsp.put("path", config.getPath());
            } else {
                resolver.revert();
                String msg = "Configuration already exist: " + (rootPath + "/" + bucketName + "/" + configName);
                rsp.put("message", msg);
                response.setStatus(HttpServletResponse.SC_CONFLICT);
            }
        } catch(PersistenceException e){
            rsp.put("message", e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            log.error("failed to create configuration", e);

        }
        om.writeValue(response.getWriter(), rsp);

    }
}
