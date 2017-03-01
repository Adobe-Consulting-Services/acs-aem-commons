/*
 *
 *  * #%L
 *  * ACS AEM Commons Bundle
 *  * %%
 *  * Copyright (C) 2016 Adobe
 *  * %%
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  * #L%
 *
 */
package com.adobe.acs.commons.one2one.impl;

import org.apache.sling.api.resource.Resource;
import org.junit.Test;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import java.util.Date;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class PropertiesTest {

    @Test
    public void lastModified_null_returnNewDate() throws Exception {
        Date result = Properties.lastModified(null);
        assertNotNull(result);
    }

    @Test
    public void lastModified_repositoryExcepton_returnNewDate() throws Exception {
        // given
        Resource resource = mock(Resource.class);
        Node node = mock(Node.class);
        when(resource.adaptTo(Node.class)).thenReturn(node);
        when(node.getProperty(anyString())).thenThrow(new RepositoryException());

        // when
        Date result = Properties.lastModified(resource);

        // then
        assertNotNull(result);
    }

    @Test
    public void lastModified_hasProperty_Date() throws Exception {
        // given
        Resource resource = mock(Resource.class);
        Node node = mock(Node.class);
        when(resource.adaptTo(Node.class)).thenReturn(node);
        Property property = mock(Property.class, RETURNS_DEEP_STUBS.get());
        when(node.getProperty("cq:lastModified")).thenReturn(property);

        // when
        Date result = Properties.lastModified(resource);

        // then
        assertNotNull(result);
        verify(property.getValue().getDate()).getTime();

    }
}