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
package com.adobe.acs.commons.util;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.GregorianCalendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ResourceUtilTest {
    @Rule
    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_MOCK);

    private Resource resource;

    @Before
    public void setupJcrRepository() throws RepositoryException {
        ResourceResolver resourceResolver = context.resourceResolver();

        Session session = resourceResolver.adaptTo(Session.class);
        Node mynode = session.getRootNode().addNode("mynode");
        mynode.setProperty("bool_prop_true", true);
        mynode.setProperty("bool_prop_false", false);
        mynode.setProperty("date_prop", new GregorianCalendar(2016, 10, 15, 12, 34, 56));
        mynode.setProperty("long_prop", 1234L);
        mynode.setProperty("ref_prop", "/mynode/myothernode");
        mynode.setProperty("ref_prop_bad_path", "/mynode/boguspath");
        mynode.setProperty("string_prop", "prop val");
        mynode.setProperty("strings_prop", new String[] {"a", "bcd", "e"});
        Node myothernode = mynode.addNode("myothernode");
        myothernode.setProperty("string_prop", "other node prop val");

        resource = resourceResolver.getResource("/mynode");
    }

    @Test
    public void testGetProperty() throws RepositoryException {
        Property prop = ResourceUtil.getProperty(resource, "string_prop");

        assertNotNull(prop);
        assertEquals("prop val", prop.getString());
    }

    @Test
    public void testGetPropertyReturnsNullIfPropertyNotFound() throws RepositoryException {
        assertNull(ResourceUtil.getProperty(resource, "bogus_prop_name"));
    }

    @Test
    public void testGetPropertyBoolean() throws RepositoryException {
        assertTrue(ResourceUtil.getPropertyBoolean(resource, "bool_prop_true"));
        assertFalse(ResourceUtil.getPropertyBoolean(resource, "bool_prop_false"));
    }

    @Test
    public void testGetPropertyBooleanReturnsFalseWhenPropertyNotFound() throws RepositoryException {
        assertFalse(ResourceUtil.getPropertyBoolean(resource, "bogus_prop_name"));
    }

    @Test
    public void testGetPropertyDate() throws RepositoryException {
        assertEquals(new GregorianCalendar(2016, 10, 15, 12, 34, 56), ResourceUtil.getPropertyDate(resource, "date_prop"));
    }

    @Test
    public void testGetPropertyDateReturnsNullIfPropertyNotFound() throws RepositoryException {
        assertNull(ResourceUtil.getPropertyDate(resource, "bogus_prop_name"));
    }

    @Test
    public void testGetPropertyLong() throws RepositoryException {
        assertEquals(new Long(1234L), ResourceUtil.getPropertyLong(resource, "long_prop"));
    }

    @Test
    public void testGetPropertyLongReturnsNullIfPropertyNotFound() throws RepositoryException {
        assertNull(ResourceUtil.getPropertyLong(resource, "bogus_prop_name"));
    }

    @Test
    public void testGetPropertyReference() throws RepositoryException {
        Resource reference = ResourceUtil.getPropertyReference(resource, "ref_prop");
        assertNotNull(reference);
        assertEquals("/mynode/myothernode", reference.getPath());
    }

    @Test
    public void testGetPropertyReferenceReturnsNullIfPathNotFound() throws RepositoryException {
        assertNull(ResourceUtil.getPropertyReference(resource, "ref_prop_bad_path"));
    }

    @Test
    public void testGetPropertyReferenceReturnsNullIfPropertyNotFound() throws RepositoryException {
        assertNull(ResourceUtil.getPropertyReference(resource, "bogus_prop_name"));
    }

    @Test
    public void testGetPropertyString() throws RepositoryException {
        assertEquals("prop val", ResourceUtil.getPropertyString(resource, "string_prop"));
    }

    @Test
    public void testGetPropertyStringReturnsNullIfPropertyNotFound() throws RepositoryException {
        assertNull(ResourceUtil.getPropertyString(resource, "bogus_prop_name"));
    }

    @Test
    public void testGetPropertyStrings() throws RepositoryException {
        assertEquals(Arrays.asList("a", "bcd", "e"), ResourceUtil.getPropertyStrings(resource, "strings_prop"));
    }

    @Test
    public void testGetPropertyStringsReturnsListForSingleValue() throws RepositoryException {
        assertEquals(Collections.singletonList("prop val"), ResourceUtil.getPropertyStrings(resource, "string_prop"));
    }

    @Test
    public void testGetPropertyStringsReturnsEmptyListIfPropertyNotFound() throws RepositoryException {
        assertEquals(new ArrayList<>(), ResourceUtil.getPropertyStrings(resource, "bogus_prop_name"));
    }

}
