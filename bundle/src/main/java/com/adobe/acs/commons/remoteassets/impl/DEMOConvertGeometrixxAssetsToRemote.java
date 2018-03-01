/*
 * TODO THIS IS A TEMPORARY CLASS FOR DEMO PURPOSES, TO BE DELETED AT A LATER TIME
 */
package com.adobe.acs.commons.remoteassets.impl;

import com.adobe.acs.commons.remoteassets.RemoteAssetsConfig;
import com.adobe.granite.asset.api.Asset;
import com.adobe.granite.asset.api.AssetManager;
import com.adobe.granite.asset.api.Rendition;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Component(label = "Convert all Geometrixx assets to remote assets", description = "Convert all Geometrixx assets to remote assets", enabled = true, metatype = false)
@Service({javax.servlet.Servlet.class, java.io.Serializable.class})
@Properties({@Property(name = "sling.servlet.paths", value = "/bin/geometrixx-remote-assets"),
        @Property(name = "sling.servlet.methods", value = {"GET"})})
public class DEMOConvertGeometrixxAssetsToRemote extends SlingAllMethodsServlet {
    private final Logger log = LoggerFactory.getLogger(DEMOConvertGeometrixxAssetsToRemote.class);

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private DynamicClassLoaderManager dynamicClassLoaderManager;

    @Reference
    private RemoteAssetsConfig remoteAssetsConfig;

    /**
     * TODO THIS IS A TEMPORARY CLASS FOR DEMO PURPOSES, TO BE DELETED AT A LATER TIME
     */
    @Override
    protected void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response) throws ServletException, IOException {
        ResourceResolver resourceResolver = null;
        Session session = null;
        try {
            resourceResolver = RemoteAssets.logIn(resourceResolverFactory);
            session = resourceResolver.adaptTo(Session.class);

            session.getWorkspace().getObservationManager().setUserData(remoteAssetsConfig.getEventUserData());

            AssetManager assetManager = resourceResolver.adaptTo(AssetManager.class);

            Resource resource = resourceResolver.getResource("/content/dam/geometrixx");

            convertFolder(resource);

            session.save();

            response.getWriter().println("All assets under /content/dam/geometrixx have been converted to remote assets");
        } catch (Exception e) {
            throw new ServletException(e);
        } finally {
            if (session != null) {
                try {
                    session.logout();
                } catch (Exception e) {
                    log.warn("Failed session.logout()", e);
                }
            }
            if (resourceResolver != null) {
                try {
                    resourceResolver.close();
                } catch (Exception e) {
                    log.warn("Failed resourceResolver.close()", e);
                }
            }
        }
    }

    /**
     * TODO THIS IS A TEMPORARY CLASS FOR DEMO PURPOSES, TO BE DELETED AT A LATER TIME
     */
    protected void convertFolder(Resource resource) throws RepositoryException {
        log.info("Traversing folder {}", resource.getPath());
        Iterator<Resource> children = resource.listChildren();
        while (children.hasNext()) {
            Resource child = children.next();
            ValueMap props = child.getValueMap();
            if (child.isResourceType("nt:folder") || child.isResourceType("sling:Folder") || child.isResourceType("sling:OrderedFolder")) {
                convertFolder(child);
            } else if (child.isResourceType("dam:Asset")) {
                convertAsset(child.adaptTo(Asset.class));
            } else {
                log.info("Ignoring {}", child.getPath());
            }
        }
    }

    /**
     * TODO THIS IS A TEMPORARY CLASS FOR DEMO PURPOSES, TO BE DELETED AT A LATER TIME
     */
    protected void convertAsset(Asset asset) throws RepositoryException {
        log.info("Converting asset {} to remote", asset.getPath());

        Iterator<? extends Rendition> renditionIterator = asset.listRenditions();
        String mimeType = asset.getRendition("original").getMimeType();
        while (renditionIterator.hasNext()) {
            Rendition assetRendition = renditionIterator.next();

            InputStream inputStream;
            if ("image/png".equals(mimeType)) {
                inputStream = dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream("/remoteassets/AEM_remote_asset.png");
            } else {
                inputStream = dynamicClassLoaderManager.getDynamicClassLoader().getResourceAsStream("/remoteassets/AEM_remote_asset.jpg");
            }

            Map<String, Object> props = new HashMap<>();
            props.put("rendition.mime", assetRendition.getMimeType());
            asset.setRendition(assetRendition.getName(), inputStream, props);

            try {
                inputStream.close();
            } catch (IOException e) {
                log.error("Error closing dummy asset stream", e);
            }
        }

        asset.getChild("jcr:content").adaptTo(Node.class).setProperty("isRemoteAsset", true);
    }

}
