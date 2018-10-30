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
package com.adobe.acs.commons.wcm.comparisons.impl;

import org.apache.sling.api.resource.Resource;
import org.junit.Test;

import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PageCompareDataLineImplTest {

    @Test
    public void constructWithResource() throws Exception {
        // given
        Resource resource = mockResource("/base/path/relative/path", "resourceName");

        // when
        PageCompareDataLineImpl underTest = new PageCompareDataLineImpl(resource, "/base/path", 1);

        // then
        assertNotNull(underTest);

        assertThat(underTest.getPath(), is("/relative/path"));
        assertThat(underTest.getDepth(), is(1));
        assertThat(underTest.getName(), is("resourceName"));
        assertNull(underTest.getValueString());
    }

    @Test
    public void constructWithProperty() throws Exception {
        // given
        Property property = mockProperty("myValue", "/base/path/relative/path", "propertyName");

        // when
        PageCompareDataLineImpl underTest = new PageCompareDataLineImpl(property, "/base/path", 1);

        // then
        assertNotNull(underTest);

        assertThat(underTest.getPath(), is("/relative/path"));
        assertThat(underTest.getDepth(), is(1));
        assertThat(underTest.getName(), is("propertyName"));
        assertThat(underTest.getValueString(), is("myValue"));
    }

    @Test
    public void getValueStringShort_reduceLengtth() throws Exception {
        // given
        Property property = mockProperty("0123456789012345678901234567890123456_40_toLong", "/base/path/relative/path", "propertyName");

        // when
        PageCompareDataLineImpl underTest = new PageCompareDataLineImpl(property, "/base/path", 1);

        // then
        assertThat(underTest.getValueString(), is("0123456789012345678901234567890123456_40_toLong"));
        assertThat(underTest.getValueStringShort(), is("0123456789012345678901234567890123456_40"));
    }

    @Test
    public void getUniqueName() throws Exception {
        // given
        Property property = mockProperty("myValue", "/base/path/relative/path/jcr:content", "propertyName");

        // when
        PageCompareDataLineImpl underTest = new PageCompareDataLineImpl(property, "/base/path", 1);

        // then
        assertThat(underTest.getUniqueName(), is("relativepath"));
    }

    @Test
    public void equals_notAnInstance_false() throws Exception {
        // given
        Property property = mockProperty("myValue", "/base/path/relative/path/jcr:content", "propertyName");

        // when
        PageCompareDataLineImpl underTest = new PageCompareDataLineImpl(property, "/base/path", 1);
        Object other = "test";

        // then
        assertFalse(underTest.equals(other));
    }

    @Test
    public void equals_samePathNameAndValue_true() throws Exception {
        // given
        Property property = mockProperty("myValue", "/base/path/relative/path/jcr:content", "propertyName");

        // when
        PageCompareDataLineImpl underTest = new PageCompareDataLineImpl(property, "/base/path", 1);
        PageCompareDataLineImpl underTest2 = new PageCompareDataLineImpl(property, "/base/path", 1);

        // then
        assertTrue(underTest.equals(underTest2));
    }

    @Test
    public void equals_differentValue_false() throws Exception {
        // given
        Property property = mockProperty("myValue", "/base/path/relative/path/jcr:content", "propertyName");
        Property property2 = mockProperty("myValue2", "/base/path/relative/path/jcr:content", "propertyName");

        // when
        PageCompareDataLineImpl underTest = new PageCompareDataLineImpl(property, "/base/path", 1);
        PageCompareDataLineImpl underTest2 = new PageCompareDataLineImpl(property2, "/base/path", 1);

        // then
        assertFalse(underTest.equals(underTest2));
    }

    @Test
    public void hashCode_sameValues_sameHashCode() throws Exception {
        // given
        Property property = mockProperty("myValue", "/base/path/relative/path/jcr:content", "propertyName");
        Property property2 = mockProperty("myValue", "/base/path/relative/path/jcr:content", "propertyName");

        // when
        PageCompareDataLineImpl underTest = new PageCompareDataLineImpl(property, "/base/path", 1);
        PageCompareDataLineImpl underTest2 = new PageCompareDataLineImpl(property2, "/base/path", 1);

        // then
        assertEquals(underTest.hashCode(), underTest2.hashCode());
    }

    @Test
    public void equals_sameInstance_true() throws Exception {
        // given
        Property property = mockProperty("myValue", "/base/path/relative/path/jcr:content", "propertyName");

        // when
        PageCompareDataLineImpl underTest = new PageCompareDataLineImpl(property, "/base/path", 1);

        // then
        assertTrue(underTest.equals(underTest));
    }

    private Resource mockResource(String path, String name) {
        Resource resource = mock(Resource.class);
        when(resource.getPath()).thenReturn(path);
        when(resource.getName()).thenReturn(name);
        return resource;
    }

    private Property mockProperty(String value, String path, String name) throws RepositoryException {
        Property property = mock(Property.class);
        Value valueMock = mock(Value.class);
        when(valueMock.getType()).thenReturn(PropertyType.STRING);
        when(valueMock.getString()).thenReturn(value);
        when(property.getValue()).thenReturn(valueMock);
        when(property.getPath()).thenReturn(path);
        when(property.getName()).thenReturn(name);
        return property;
    }

}