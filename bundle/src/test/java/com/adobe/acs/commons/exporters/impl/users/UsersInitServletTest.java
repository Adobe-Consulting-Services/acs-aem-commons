package com.adobe.acs.commons.exporters.impl.users;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Session;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class UsersInitServletTest {

    Logger LOG = LoggerFactory.getLogger(UsersInitServletTest.class);
    
    @Rule
    public SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);

    
    
    @Before
    public void setup() throws Exception {
        JackrabbitSession session = (JackrabbitSession) context.resourceResolver().adaptTo(Session.class);
        UserManager um = session.getUserManager();
        context.registerAdapter(Resource.class, UserManager.class, um);
        
        um.createGroup("allusers");
        um.createGroup("users");
        
        Map<String,Object> props = new HashMap<>();
        props.put(Constants.CUSTOM_PROPERTIES, "custom_prop");
        props.put(Constants.GROUPS, "somegroup");
        props.put(Constants.GROUP_FILTER, "direct");
        context.build().resource("/report",props).commit();
        
        context.request().setResource(context.resourceResolver().getResource("/report"));
        
        
        
    }
    
    
    @Test
    public void testGroupExistence() throws Exception {
        UsersInitServlet servlet = new UsersInitServlet();
        servlet.doGet(context.request(), context.response());
        LOG.info (context.response().getOutputAsString());
        
        JsonObject json = new JsonParser().parse(context.response().getOutputAsString()).getAsJsonObject();
        JsonObject options = json.get("options").getAsJsonObject();
        JsonArray groups = options.get("groups").getAsJsonArray();
        assertEquals(2,groups.size());
        JSONAssert.assertEquals("[allusers, users]", groups.toString(),false);
    }
    
    
}
