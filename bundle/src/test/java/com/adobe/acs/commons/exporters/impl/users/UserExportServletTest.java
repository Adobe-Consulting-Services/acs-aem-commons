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
package com.adobe.acs.commons.exporters.impl.users;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class UserExportServletTest {

    @Rule
    public SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);

    UsersExportServlet servlet;

    @Before
    public void setup() throws RepositoryException, Exception {
        
        JackrabbitSession session = (JackrabbitSession) context.resourceResolver().adaptTo(Session.class);
        UserManager um = session.getUserManager();
        
        context.registerAdapter(Resource.class, UserManager.class, um);

        Group allUsers = um.createGroup("allusers");
        Group users = um.createGroup("users");
        allUsers.addMember(users);

        User bob = um.createUser("bob", "bobspassword");
        User alice = um.createUser("alice", "alicespassword");
        User charly = um.createUser("charly", "charlyspassword");

        users.addMember(bob);
        users.addMember(alice);
        allUsers.addMember(charly);

        session.save();
        servlet = new UsersExportServlet();
    }

    @Test
    public void testWithNoParameterProvidedInRequest() throws Exception {
        servlet.doGet(context.request(), context.response());
        assertEquals(context.response().getStatus(), 200);
        String output = context.response().getOutputAsString();

        CSVParser parser = CSVParser.parse(output, CSVFormat.DEFAULT.withHeader());
        assertAllUsersPresent(parser.getRecords(), "alice","bob","charly","admin","anonymous");
    }
    
    @Test
    public void testWithGroupDirectFilter() throws Exception {

        // Build parameters
        JsonObject params = buildParameterObject("direct", "users");

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("params", params);
        
        context.request().setParameterMap(parameters);
        servlet.doGet(context.request(), context.response());
        
        assertEquals(context.response().getStatus(), 200);
        String output = context.response().getOutputAsString();

        CSVParser parser = CSVParser.parse(output, CSVFormat.DEFAULT.withHeader());
        assertAllUsersPresent(parser.getRecords(), "alice","bob");
    }
    
    @Test
    public void testWithGroupIndirectFilter() throws Exception {

        // Build parameters
        JsonObject params = buildParameterObject("indirect", "allusers");

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("params", params);
        
        context.request().setParameterMap(parameters);
        servlet.doGet(context.request(), context.response());
        
        assertEquals(context.response().getStatus(), 200);
        String output = context.response().getOutputAsString();

        CSVParser parser = CSVParser.parse(output, CSVFormat.DEFAULT.withHeader());
        assertAllUsersPresent(parser.getRecords(), "alice","bob");
    }
    
    @Test
    public void testWithGroupBothFIlter() throws Exception {

        // Build parameters
        JsonObject params = buildParameterObject("", "allusers");

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("params", params);
        
        context.request().setParameterMap(parameters);
        servlet.doGet(context.request(), context.response());
        
        assertEquals(context.response().getStatus(), 200);
        String output = context.response().getOutputAsString();

        CSVParser parser = CSVParser.parse(output, CSVFormat.DEFAULT.withHeader());
        assertAllUsersPresent(parser.getRecords(), "alice","bob","charly");
    }
    
    
    

    /**
     * Build the JSON parameter structure
     * @param groupFilter
     * @param group
     * @return
     */
    public static JsonObject buildParameterObject(String groupFilter,String group) {
        JsonObject params = new JsonObject();
        params.addProperty("groupFilter", groupFilter);
        JsonArray groups = new JsonArray();
        groups.add(new JsonPrimitive(group));
        params.add("groups", groups);

        JsonArray customProperties = new JsonArray();
        JsonObject o = new JsonObject();
        o.addProperty("relPropertyPath", "abc");

        customProperties.add(o);
        params.add("customProperties", customProperties);
        
        return params;
    }
    
    
    void assertAllUsersPresent(List<CSVRecord> records, String...users) {
        assertEquals(records.size(), users.length);
        
        Set<String> presentUserIds = new HashSet<>();
        for (CSVRecord item : records) {
            presentUserIds.add(item.get("User ID"));
        }
        
        for (String id : users) {
            assertTrue(presentUserIds.contains(id));
        }
        
    }
    
}
