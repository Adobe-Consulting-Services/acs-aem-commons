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
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.math.BigDecimal;
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
        mynode.setProperty("bool_prop_true_str", "true");
        mynode.setProperty("bool_prop_false", false);
        mynode.setProperty("bool_prop_false_str", "false");
        mynode.setProperty("date_prop", new GregorianCalendar(2016, 10, 15, 12, 34, 56));
        mynode.setProperty("dbl_prop", 1234.567d);
        mynode.setProperty("dbl_prop_str", "1234.567");
        mynode.setProperty("long_prop", 1234L);
        mynode.setProperty("long_prop_str", "1234");
        mynode.setProperty("ref_prop", "/mynode/myothernode");
        mynode.setProperty("ref_prop_bad_path", "/mynode/boguspath");
        mynode.setProperty("string_prop", "prop val");
        mynode.setProperty("strings_prop", new String[] {"a", "bcd", "e"});
        Node myothernode = mynode.addNode("myothernode");
        myothernode.setProperty("string_prop", "other node prop val");

        resource = resourceResolver.getResource("/mynode");
    }

    @Test
    public void testGetPropertyBoolean() {
        assertTrue(ResourceUtil.getPropertyBoolean(resource, "bool_prop_true"));
        assertTrue(ResourceUtil.getPropertyBoolean(resource, "bool_prop_true_str"));
        assertFalse(ResourceUtil.getPropertyBoolean(resource, "bool_prop_false"));
        assertFalse(ResourceUtil.getPropertyBoolean(resource, "bool_prop_false_str"));
    }

    @Test
    public void testGetPropertyBooleanReturnsFalseWhenPropertyNotFound() {
        assertFalse(ResourceUtil.getPropertyBoolean(resource, "bogus_prop_name"));
    }

    @Test
    public void testGetPropertyCalendar() {
        assertEquals(new GregorianCalendar(2016, 10, 15, 12, 34, 56), ResourceUtil.getPropertyCalendar(resource, "date_prop"));
    }

    @Test
    public void testGetPropertyCalendarReturnsNullIfPropertyNotFound() {
        assertNull(ResourceUtil.getPropertyCalendar(resource, "bogus_prop_name"));
    }

    @Test
    public void testGetPropertyDate() {
        assertEquals(new GregorianCalendar(2016, 10, 15, 12, 34, 56).getTime(), ResourceUtil.getPropertyDate(resource, "date_prop"));
    }

    @Test
    public void testGetPropertyDateReturnsNullIfPropertyNotFound() {
        assertNull(ResourceUtil.getPropertyDate(resource, "bogus_prop_name"));
    }

    @Test
    public void testGetPropertyDecimal() {
        assertEquals(new BigDecimal("1234.567"), ResourceUtil.getPropertyDecimal(resource, "dbl_prop"));
        assertEquals(new BigDecimal("1234.567"), ResourceUtil.getPropertyDecimal(resource, "dbl_prop_str"));
        assertEquals(new BigDecimal("1234"), ResourceUtil.getPropertyDecimal(resource, "long_prop"));
        assertEquals(new BigDecimal("1234"), ResourceUtil.getPropertyDecimal(resource, "long_prop_str"));
    }

    @Test
    public void testGetPropertyDecimalReturnsNullIfPropertyNotFound() {
        assertNull(ResourceUtil.getPropertyDecimal(resource, "bogus_prop_name"));
    }

    @Test
    public void testGetPropertyDouble() {
        assertEquals(new Double(1234.567d), ResourceUtil.getPropertyDouble(resource, "dbl_prop"));
        assertEquals(new Double(1234.567d), ResourceUtil.getPropertyDouble(resource, "dbl_prop_str"));
        assertEquals(new Double(1234L), ResourceUtil.getPropertyDouble(resource, "long_prop"));
        assertEquals(new Double(1234L), ResourceUtil.getPropertyDouble(resource, "long_prop_str"));
    }

    @Test
    public void testGetPropertyDoubleReturnsNullIfPropertyNotFound() {
        assertNull(ResourceUtil.getPropertyDouble(resource, "bogus_prop_name"));
    }

    @Test
    public void testGetPropertyLong() {
        assertEquals(new Long(1234L), ResourceUtil.getPropertyLong(resource, "long_prop"));
        assertEquals(new Long(1234L), ResourceUtil.getPropertyLong(resource, "long_prop_str"));
    }

    @Test
    public void testGetPropertyLongReturnsNullIfPropertyNotFound() {
        assertNull(ResourceUtil.getPropertyLong(resource, "bogus_prop_name"));
    }

    @Test
    public void testGetPropertyReference() {
        Resource reference = ResourceUtil.getPropertyReference(resource, "ref_prop");
        assertNotNull(reference);
        assertEquals("/mynode/myothernode", reference.getPath());
    }

    @Test
    public void testGetPropertyReferenceReturnsNullIfPathNotFound() {
        assertNull(ResourceUtil.getPropertyReference(resource, "ref_prop_bad_path"));
    }

    @Test
    public void testGetPropertyReferenceReturnsNullIfPropertyNotFound() {
        assertNull(ResourceUtil.getPropertyReference(resource, "bogus_prop_name"));
    }

    @Test
    public void testGetPropertyString() {
        assertEquals("prop val", ResourceUtil.getPropertyString(resource, "string_prop"));
    }

    @Test
    public void testGetPropertyStringReturnsNullIfPropertyNotFound() {
        assertNull(ResourceUtil.getPropertyString(resource, "bogus_prop_name"));
    }

    @Test
    public void testGetPropertyStrings() {
        assertEquals(Arrays.asList("a", "bcd", "e"), ResourceUtil.getPropertyStrings(resource, "strings_prop"));
    }

    @Test
    public void testGetPropertyStringsReturnsListForSingleValue() {
        assertEquals(Collections.singletonList("prop val"), ResourceUtil.getPropertyStrings(resource, "string_prop"));
    }

    @Test
    public void testGetPropertyStringsReturnsEmptyListIfPropertyNotFound() {
        assertEquals(new ArrayList<>(), ResourceUtil.getPropertyStrings(resource, "bogus_prop_name"));
    }

}
