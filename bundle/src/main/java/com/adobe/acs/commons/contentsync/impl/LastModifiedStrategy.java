/*-
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2022 Adobe
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
package com.adobe.acs.commons.contentsync.impl;

import com.adobe.acs.commons.contentsync.CatalogItem;
import com.adobe.acs.commons.contentsync.UpdateStrategy;
import com.adobe.granite.security.user.util.AuthorizableUtil;
import com.day.cq.commons.PathInfo;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestProgressTracker;
import org.apache.sling.api.resource.AbstractResourceVisitor;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.ServletResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.servlet.GenericServlet;
import javax.servlet.Servlet;
import java.lang.reflect.Proxy;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.adobe.acs.commons.contentsync.ContentCatalogJobConsumer.SERVICE_NAME;
import static org.apache.jackrabbit.JcrConstants.JCR_CONTENT;
import static org.apache.jackrabbit.JcrConstants.JCR_MIXINTYPES;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;

@Component
public class LastModifiedStrategy implements UpdateStrategy {
    public static final String DEFAULT_GET_SERVLET = "org.apache.sling.servlets.get.DefaultGetServlet";
    public static final String REDIRECT_SERVLET = "org.apache.sling.servlets.get.impl.RedirectServlet";

    @Reference
    private ServletResolver servletResolver;

    @Reference
    ResourceResolverFactory resourceResolverFactory;

    /**
     * The ContentCatalog class provides methods to fetch and process content catalogs
     * from a remote instance.
     */
    @Override
    public List<CatalogItem> getItems(Map<String, Object> request) throws LoginException {
        String rootPath = (String)request.get("root");
        if (rootPath == null) {
            throw new IllegalArgumentException("root request parameter is required");
        }
        boolean nonRecursive = "false".equals(request.get("recursive"));

        Map<String, Object> AUTH_INFO = Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, SERVICE_NAME);
        try (ResourceResolver resolver = resourceResolverFactory.getServiceResourceResolver(AUTH_INFO)) {

            Resource root = resolver.getResource(rootPath);
            if (root == null) {
                return Collections.emptyList();
            }
            List<CatalogItem> items = new ArrayList<>();
            if (nonRecursive) {
                JsonObjectBuilder json = Json.createObjectBuilder();
                writeMetadata(json, root);
                items.add(new CatalogItem(json.build()));
            } else {
                new AbstractResourceVisitor() {
                    @Override
                    public void visit(Resource res) {
                        if (!accepts(res)) {
                            return;
                        }
                        JsonObjectBuilder json = Json.createObjectBuilder();
                        writeMetadata(json, res);
                        items.add(new CatalogItem(json.build()));
                    }
                }.accept(root);
            }
            return items;
        }
    }

    /**
     * Checks if the remote resource is modified compared to the local resource.
     *
     * @param remoteResource the remote catalog item
     * @param localResource  the local resource
     * @return true if the remote resource is modified, false otherwise
     */
    @Override
    public boolean isModified(CatalogItem remoteResource, Resource localResource) {
        LastModifiedInfo remoteLastModified = getLastModified(remoteResource);
        LastModifiedInfo localLastModified = getLastModified(localResource);

        return remoteLastModified.getLastModified() > localLastModified.getLastModified();
    }

    /**
     * Generates a message indicating the modification status of the resource.
     *
     * @param remoteResource the remote catalog item
     * @param localResource  the local resource
     * @return a message indicating the modification status
     */
    @Override
    @SuppressWarnings("squid:S2583")
    public String getMessage(CatalogItem remoteResource, Resource localResource) {
        LastModifiedInfo remoteLastModified = getLastModified(remoteResource);
        LastModifiedInfo localLastModified = getLastModified(localResource);

        boolean modified = remoteLastModified.getLastModified() > localLastModified.getLastModified();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy, h:mm:ss a");
        StringBuilder msg = new StringBuilder();
        if (localResource == null) {
            msg.append("resource does not exist");
        } else {
            msg.append(modified ? "resource modified ... " : "replacing ... ");
            if (localLastModified.getLastModified() > 0) {
                msg.append('\n');
                msg.append("\tlocal lastModified: " + dateFormat.format(localLastModified.getLastModified()) + " by " + localLastModified.getLastModifiedBy());
            }
            if (remoteLastModified.getLastModified() > 0) {
                msg.append('\n');
                msg.append("\tremote lastModified: " + dateFormat.format(remoteLastModified.getLastModified()) + " by " + remoteLastModified.getLastModifiedBy());
            }
        }
        return msg.toString();
    }

    /**
     * Determines whether to write the resource in the catalog json.
     * <p>
     * For example, implementations can return only dam:Asset nodes,
     * or any nt:unstructured nodes, etc.
     *
     * @param resource the resource to check
     * @return whether to write the resource in the catalog
     */
    boolean accepts(Resource resource) {
        if (
            // don't drill down into jcr:content. The entire content will be grabbed by jcr:content.infinity.json
                resource.getPath().contains("/" + JCR_CONTENT)
                        // ignore rep:policy, rep:cugPolicy, rep:restrictions and such
                        || resource.getPath().contains("/rep:")
        ) {
            return false;
        }
        return true;
    }

    /**
     * Returns the render servlet for the given urlPath .
     *
     * @param urlPath the json export url,. e.g. <code>/content/wknd/page/jcr:content.infinity.json</code>
     * @return the render servlet, e.g. <code>org.apache.sling.servlets.get.DefaultGetServlet</code>
     */
    String getJsonRendererServlet(ResourceResolver resourceResolver, String urlPath) {
        Resource resource = resourceResolver.resolve(urlPath);
        Servlet s = servletResolver.resolveServlet(
                createFakeRequest(resource, urlPath)
        );
        String servletName = null;
        if (s instanceof GenericServlet) {
            GenericServlet genericServlet = (GenericServlet) s;
            servletName = genericServlet.getServletName();
        }
        // Sling Redirect Servlet handles json exports by forwarding to DefaultGetServlet. So do we.
        if (REDIRECT_SERVLET.equals(servletName)) {
            servletName = DEFAULT_GET_SERVLET;
        }
        return servletName;
    }

    /**
     * Creates a fake SlingHttpServletRequest to pass to {@link  ServletResolver#resolveServlet(SlingHttpServletRequest)} .
     *
     * This trick is fine, as {@link  ServletResolver#resolveServlet(SlingHttpServletRequest)} only needs
     * RequestPathInfo and request method and this data can be passed via a mocked request.
     *
     * @param resource  the .resource to export
     * @param urlPath   the .json url to export the resource, e.g. /content/wknd/page/jcr:content.infinity.json
     * @return fake http GET request
     */
    SlingHttpServletRequest createFakeRequest(Resource resource, String urlPath){
        RequestProgressTracker progressTracker = (RequestProgressTracker) Proxy.newProxyInstance(
                RequestProgressTracker.class.getClassLoader(),
                new Class[]{RequestProgressTracker.class},
                (proxy, method, args) -> null);

        SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) Proxy.newProxyInstance(
                SlingHttpServletRequest.class.getClassLoader(),
                new Class[]{SlingHttpServletRequest.class},
                (proxy, method, args) -> {
                    String methodName = method.getName();
                    switch (methodName) {
                        case "getRequestProgressTracker":
                            return progressTracker;
                        case "getResource":
                            return resource;
                        case "getMethod":
                            return "GET";
                        case "getRequestPathInfo":
                            return new PathInfo(urlPath);
                        default:
                            throw new UnsupportedOperationException(methodName);
                    }
                });
        return slingRequest;
    }

    void writeMetadata(JsonObjectBuilder jw, Resource res) {
        jw.add("path", res.getPath());
        jw.add(JCR_PRIMARYTYPE, res.getValueMap().get(JCR_PRIMARYTYPE, String.class));

        String[] mixins = res.getValueMap().get(JCR_MIXINTYPES, String[].class);
        if (mixins != null) {
            JsonArrayBuilder mx = Json.createArrayBuilder();
            for (String mixin : mixins) {
                mx.add(mixin);
            }
            jw.add(JCR_MIXINTYPES, mx.build());
        }

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

        String renderServlet = getJsonRendererServlet(res.getResourceResolver(), exportUri);
        // check if the resource is rendered by DefaultGetServlet, i.e. is exportable
        // if it isn't, go one level up and try the parent
        if (!DEFAULT_GET_SERVLET.equals(renderServlet)) {
            contentResource = contentResource.getParent();
            exportUri = contentResource.getPath() + ".infinity.json";
        }

        // last try: if the rendering servlet is still not DefaultGetServlet then
        // put a flag in the output.
        renderServlet = getJsonRendererServlet(res.getResourceResolver(), exportUri);
        if (!DEFAULT_GET_SERVLET.equals(renderServlet)) {
            jw.add("renderServlet", renderServlet);
        }
        jw.add("exportUri", exportUri);

        LastModifiedInfo lastModified = getLastModified(res);

        if (lastModified.getLastModified() > 0L) {
            jw.add("lastModified", lastModified.getLastModified());
        }
        if (lastModified.getLastModifiedBy() != null) {
            jw.add("lastModifiedBy", lastModified.getLastModifiedBy());
        }
    }

    private LastModifiedInfo getLastModified(CatalogItem item) {
        long lastModified = item.getLong("lastModified");
        String lastModifiedBy = item.getString("lastModifiedBy");
        return new LastModifiedInfo(lastModified, lastModifiedBy);
    }

    @SuppressWarnings("squid:S1144")
    private LastModifiedInfo getLastModified(Resource targetResource) {
        long lastModified = 0L;
        String lastModifiedBy = null;
        if (targetResource != null) {
            Resource contentResource = targetResource.getChild(JCR_CONTENT);
            if (contentResource == null) {
                contentResource = targetResource;
            }
            ValueMap vm = contentResource.getValueMap();
            Calendar c = (Calendar) vm.get("cq:lastModified", (Class) Calendar.class);
            if (c == null) {
                c = (Calendar) vm.get("jcr:lastModified", (Class) Calendar.class);
            }
            if (c != null) {
                lastModified = c.getTime().getTime();
            }
            String modifiedBy = (String) vm.get("cq:lastModifiedBy", (Class) String.class);
            if (modifiedBy == null) {
                modifiedBy = (String) vm.get("jcr:lastModifiedBy", (Class) String.class);
            }
            lastModifiedBy = AuthorizableUtil.getFormattedName(targetResource.getResourceResolver(), modifiedBy);
        }
        return new LastModifiedInfo(lastModified, lastModifiedBy);
    }

    private static class LastModifiedInfo {
        private final long lastModified;
        private final String lastModifiedBy;

        public LastModifiedInfo(long lastModified, String lastModifiedBy) {
            this.lastModified = lastModified;
            this.lastModifiedBy = lastModifiedBy;
        }

        public long getLastModified() {
            return lastModified;
        }

        public String getLastModifiedBy() {
            return lastModifiedBy;
        }
    }
}
