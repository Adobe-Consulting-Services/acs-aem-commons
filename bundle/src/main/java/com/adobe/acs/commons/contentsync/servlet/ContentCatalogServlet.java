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
package com.adobe.acs.commons.contentsync.servlet;

import com.adobe.acs.commons.contentsync.UpdateStrategy;
import com.adobe.acs.commons.contentsync.impl.LastModifiedStrategy;
import com.day.cq.commons.PathInfo;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.AbstractResourceVisitor;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.ServletResolver;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.servlet.GenericServlet;
import javax.servlet.Servlet;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static org.apache.jackrabbit.JcrConstants.JCR_CONTENT;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;

@Component(service = Servlet.class, immediate = true, property = {
        "sling.servlet.extensions=json",
        "sling.servlet.selectors=catalog",
        "sling.servlet.resourceTypes=acs-commons/components/utilities/contentsync",
})
public class ContentCatalogServlet extends SlingSafeMethodsServlet {

    static final String DEFAULT_GET_SERVLET = "org.apache.sling.servlets.get.DefaultGetServlet";
    static final String REDIRECT_SERVLET = "org.apache.sling.servlets.get.impl.RedirectServlet";

    @Reference
    private transient ServletResolver servletResolver;

    public static final String DEFAULT_STRATEGY = LastModifiedStrategy.class.getName();

    private final transient Map<String, UpdateStrategy> updateStrategies = Collections.synchronizedMap(new LinkedHashMap<>());

    @Reference(service = UpdateStrategy.class,
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC)
    protected void bindDeltaStrategy(UpdateStrategy strategy) {
        if (strategy != null) {
            String key = strategy.getClass().getName();
            updateStrategies.put(key, strategy);
        }
    }

    protected void unbindDeltaStrategy(UpdateStrategy strategy) {
        String key = strategy.getClass().getName();
        updateStrategies.remove(key);
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        response.setContentType("application/json");

        String pid = request.getParameter("strategy");
        if (pid == null) {
            pid = DEFAULT_STRATEGY;
        }
        UpdateStrategy updateStrategy = getStrategy(pid);
        try (JsonGenerator jw = Json.createGenerator(response.getWriter())) {
            jw.writeStartObject();
            try {
                String rootPath = request.getParameter("root");
                if (rootPath == null) throw new IllegalArgumentException("root request parameter is required");

                Resource root = request.getResourceResolver().getResource(rootPath);
                jw.writeStartArray("resources");
                if (root != null) {
                    new AbstractResourceVisitor() {
                        @Override
                        public void visit(Resource res) {
                            if (!updateStrategy.accepts(res)) {
                                return;
                            }
                            jw.writeStartObject();
                            writeCommonProperties(jw, res, request);
                            // write strategy-specific metadata
                            updateStrategy.writeMetadata(jw, res);
                            jw.writeEnd();
                        }
                    }.accept(root);
                }
                jw.writeEnd();
            } catch (Exception e) {
                jw.write("error", e.getMessage());
                response.setStatus(SC_INTERNAL_SERVER_ERROR);
            }
            jw.writeEnd();
        }
    }

    void writeCommonProperties(JsonGenerator jw, Resource res, SlingHttpServletRequest request) {
        jw.write("path", res.getPath());
        jw.write(JCR_PRIMARYTYPE, res.getValueMap().get(JCR_PRIMARYTYPE, String.class));

        Resource jcrContent = res.getChild(JCR_CONTENT);
        String exportUri;
        Resource contentResource;
        if (jcrContent != null) {
            contentResource = jcrContent;
            exportUri = jcrContent.getPath() + ".infinity.json";
        } else {
            contentResource = res;
            exportUri = res.getPath() + ".json";
        }

        String renderServlet = getJsonRendererServlet(request, contentResource, exportUri);
        // check if the resource is rendered by DefaultGetServlet, i.e. is exportable
        // if it isn't, go one level up and try the parent
        if(!DEFAULT_GET_SERVLET.equals(renderServlet)){
            contentResource = contentResource.getParent();
            exportUri = contentResource.getPath() + ".infinity.json";
        }

        // last try: if the rendering servlet is still not DefaultGetServlet then
        // put a flag in the output.
        renderServlet = getJsonRendererServlet(request, contentResource, exportUri);
        if(!DEFAULT_GET_SERVLET.equals(renderServlet)) {
            jw.write("renderServlet", renderServlet);
        }
        jw.write("exportUri", exportUri);
    }

    /**
     * 
     *
     * @param resource  the resource
     * @param urlPath   the json export url,. e.g. <code>/content/wknd/page/jcr:content.infinity.json</code>
     * @return  the render servlet, e.g. <code>org.apache.sling.servlets.get.DefaultGetServlet</code>
     */
    String getJsonRendererServlet(SlingHttpServletRequest slingRequest, Resource resource, String urlPath) {
        Servlet s = servletResolver.resolveServlet(new SlingHttpServletRequestWrapper(slingRequest) {
            @Override
            public Resource getResource() {
                return resource;
            }

            @Override
            public String getMethod() {
                return "GET";
            }

            @Override
            public RequestPathInfo getRequestPathInfo() {
                return new PathInfo(urlPath);
            }
        });
        String servletName = null;
        if(s instanceof GenericServlet){
            GenericServlet genericServlet = (GenericServlet)s;
            servletName = genericServlet.getServletName();
        }
        // Sling Redirect Servlet handles json exports by forwarding to DefaultGetServlet. So do we.
        if(REDIRECT_SERVLET.equals(servletName)){
            servletName = DEFAULT_GET_SERVLET;
        }
        return servletName;
    }

    UpdateStrategy getStrategy(String pid) {
        return updateStrategies.get(pid);
    }
}