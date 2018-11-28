/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2018 Adobe
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
package com.adobe.acs.commons.json;

import com.google.gson.JsonObject;
import java.io.IOException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.sling.testing.mock.jcr.MockJcr;
import org.junit.Before;
import org.junit.Test;

import static com.adobe.acs.commons.json.JsonObjectUtil.*;
import static org.junit.Assert.*;

/**
 * Test the JcrJsonAdapter utility functions
 */
public class JcrJsonAdapterTest {
    
    public JcrJsonAdapterTest() {
    }
    
    Session session;

    @Before
    public void setUp() {
        session = MockJcr.newSession();
    }

    /**
     * Test of write method, of class JcrJsonAdapter.
     * @throws javax.jcr.RepositoryException
     */
    @Test
    public void testWrite() throws RepositoryException {
        Node root = JcrUtils.getOrCreateByPath("/test/level1", JcrConstants.NT_UNSTRUCTURED, session);
        root.setProperty("level",1);
        Node l2 = JcrUtils.getOrCreateByPath("/test/level1/l2", JcrConstants.NT_UNSTRUCTURED, session);
        l2.setProperty("level",2);
        Node l3 = JcrUtils.getOrCreateByPath("/test/level1/l2/l3", JcrConstants.NT_UNSTRUCTURED, session);
        l3.setProperty("level",3);
        
        JsonObject jsonObject = toJsonObject(root);
        
        assertNotNull(jsonObject);
        assertEquals(1L, getInteger(jsonObject, "level").longValue());
        assertTrue(jsonObject.has("l2"));
        assertEquals(2L, getInteger(jsonObject.get("l2").getAsJsonObject(), "level").longValue());
        assertTrue(jsonObject.get("l2").getAsJsonObject().has("l3"));
        assertEquals(3L, getInteger(jsonObject.get("l2").getAsJsonObject().get("l3").getAsJsonObject(), "level").longValue());
    }

    /**
     * Test of read method, of class JcrJsonAdapter, which is not supported
     * @throws java.io.IOException
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testRead() throws IOException {
        JcrJsonAdapter instance = new JcrJsonAdapter();
        instance.read(null);
    }
    
}
