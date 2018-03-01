/**
 * TODO THIS IS A TEMPORARY CLASS FOR DEMO PURPOSES, TO BE DELETED AT A LATER TIME
 */
package com.adobe.acs.commons.remoteassets.impl;

import com.adobe.acs.commons.remoteassets.RemoteAssetsNodeSync;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;

import javax.servlet.ServletException;
import java.io.IOException;


/**
 * TODO THIS IS A TEMPORARY CLASS FOR DEMO PURPOSES, TO BE DELETED AT A LATER TIME
 */
@Component(label = "Test Servlet", description = "Test Servlet", enabled = true, metatype = false)
@Service({javax.servlet.Servlet.class, java.io.Serializable.class})
@Properties({@Property(name = "sling.servlet.paths", value = "/bin/test"),
        @Property(name = "sling.servlet.methods", value = {"GET"})})
public class DEMOAssetSyncServlet extends SlingAllMethodsServlet {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @Reference
    private transient RemoteAssetsNodeSync assetSyncService;

    /**
     * TODO THIS IS A TEMPORARY CLASS FOR DEMO PURPOSES, TO BE DELETED AT A LATER TIME
     */
    @Override
    protected void doGet(final SlingHttpServletRequest request,
                         final SlingHttpServletResponse response) throws ServletException, IOException {
        assetSyncService.syncAssets();

    }

}
