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
package com.adobe.acs.commons.version.impl;

import static org.mockito.Mockito.when;

import java.util.Collections;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.version.Version;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.mockito.Mockito;

import com.day.cq.commons.jcr.JcrConstants;

public final class FrozenResourceMock {

    public static final String TESTED_KEY = JcrConstants.JCR_TITLE;

    private final Version version = Mockito.mock(Version.class);

    private final Node node = Mockito.mock(Version.class);

    private final Resource resource = Mockito.mock(Resource.class);

    private final Property property = Mockito.mock(Property.class);

    private final Value value = Mockito.mock(Value.class);

    private final ValueMap valueMap;

    public FrozenResourceMock(final String resourcePath, final String versionName, final String propertyValue) throws RepositoryException {
    	valueMap = new ValueMapDecorator(Collections.singletonMap(TESTED_KEY, propertyValue));

        when(version.getName()).thenReturn(versionName);
        when(version.getFrozenNode()).thenReturn(node);

        when(node.getPath()).thenReturn(resourcePath);
        when(node.getProperty(TESTED_KEY)).thenReturn(property);
        when(node.getName()).thenReturn(resourcePath);

        when(resource.getValueMap()).thenReturn(valueMap);
        when(resource.adaptTo(Node.class)).thenReturn(node);
        when(resource.getChildren()).thenReturn(Collections.emptyList());

        when(property.getPath()).thenReturn(resourcePath + "/" + TESTED_KEY);
        when(property.getParent()).thenReturn(node);
        when(property.isMultiple()).thenReturn(false);
        when(property.getValue()).thenReturn(value);

        when(value.getString()).thenReturn(propertyValue);
        when(value.getType()).thenReturn(PropertyType.STRING);
    }

	public Version getVersion() {
		return version;
	}

	public Resource getResource() {
		return resource;
	}

	public ValueMap getValueMap() {
		return valueMap;
	}

	public Node getNode() {
		return node;
	}
}
