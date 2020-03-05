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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.junit.MockitoJUnitRunner;

import com.adobe.acs.commons.wcm.comparisons.PageCompareData;
import com.adobe.acs.commons.wcm.comparisons.PageCompareDataLine;
import com.adobe.acs.commons.wcm.comparisons.VersionSelection;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@RunWith(MockitoJUnitRunner.class)
public final class PageCompareDataImplTest {

    @Test
    public void shouldInitialize() throws RepositoryException {
        // given
        final String path = "/my/path";
        final String versionName = "latest";

        final Resource resource = mockResource(path, versionName, new Date());

        // when
        final PageCompareData pageCompareData = new PageCompareDataImpl(resource, versionName);

        // then
        assertThat(pageCompareData.getVersions(), not(Collections.<VersionSelection>emptyList()));
    }

    @Test
    public void getResource_shouldReturnValue() throws RepositoryException {
        // given
        final Resource resource = mockResource("/my/path", "latest", new Date());

        // when
        final PageCompareData pageCompareData = new PageCompareDataImpl(resource, "latest");

        // then
        assertThat(pageCompareData.getResource(), is(resource));
    }

    @Test
    public void getVersion() throws RepositoryException {
        // given
        final String versionName = "latest";

        final Resource resource = mockResource("/my/path", versionName, new Date());

        // when
        final PageCompareData pageCompareData = new PageCompareDataImpl(resource, versionName);

        // then
        assertThat(pageCompareData.getVersion(), is(versionName));
    }

    @Test
    public void getVersionDate() throws RepositoryException {
        // given
        final Date date = new Date();
        final Resource resource = mockResource("/my/path", "latest", date);

        // when
        final PageCompareData pageCompareData = new PageCompareDataImpl(resource, "latest");

        // then
        assertThat(pageCompareData.getVersionDate(), is(date));
    }

    @Test
    public void getPath() throws RepositoryException {
        // given
        final String path = "/my/path";
        final Resource resource = mockResource(path, "latest", new Date());

        // when
        final PageCompareData pageCompareData = new PageCompareDataImpl(resource, "latest");

        // then
        assertThat(pageCompareData.getPath(), is(path));
    }

    @Test
    public void getVersions() throws RepositoryException {
        // given
        final Resource resource = mockResource("/my/path", "latest", new Date());

        // when
        final PageCompareData pageCompareData = new PageCompareDataImpl(resource, "latest");

        // then
        final List<VersionSelection> versions = pageCompareData.getVersions();
        assertThat(versions, not(Collections.<VersionSelection>emptyList()));
        assertThat(versions.get(1).getName(), is("latest"));
    }

    @Test
    public void getLines() throws RepositoryException {
        // given
        final Resource resource = mockResource("/my/path", "latest", new Date());

        final Set<String> valueMapKeys = Sets.newHashSet("a", "b");
        final Property a = mockProperty("a", "/my/path/a", "value a");
        when(resource.adaptTo(Node.class).getProperty("a")).thenReturn(a);
        final Property b = mockProperty("b", "/my/path/b", "value b");
        when(resource.adaptTo(Node.class).getProperty("b")).thenReturn(b);
        when(resource.getValueMap().keySet()).thenReturn(valueMapKeys);

        // when
        final PageCompareData pageCompareData = new PageCompareDataImpl(resource, "latest");

        // then
        final List<PageCompareDataLine> lines = pageCompareData.getLines();
        assertThat(lines, not(Collections.<PageCompareDataLine>emptyList()));
        assertThat(lines.get(0).getName(), is("a"));
        assertThat(lines.get(1).getName(), is("b"));
    }

    @Test
    public void getRecursiveLines() throws RepositoryException {
        // given
        final Resource resource = mockResource("/my/path", "latest", new Date());

        final Set<String> valueMapKeys = Sets.newHashSet("a", "b");
        final Property a = mockProperty("a", "/my/path/a", "value a");
        when(resource.adaptTo(Node.class).getProperty("a")).thenReturn(a);
        final Property b = mockProperty("b", "/my/path/b", "value b");
        when(resource.adaptTo(Node.class).getProperty("b")).thenReturn(b);
        when(resource.getValueMap().keySet()).thenReturn(valueMapKeys);

        final Resource childC = mockResource("/my/path/c", "latest", new Date());
        when(childC.getName()).thenReturn("c");
        final Resource childD = mockResource("/my/path/d", "latest", new Date());
        when(childD.getName()).thenReturn("d");

        final Property e = mockProperty("e", "/my/path/d/e", "value e");
        when(childD.adaptTo(Node.class).getProperty("e")).thenReturn(e);
        when(childD.getValueMap().keySet()).thenReturn(Sets.newHashSet("e"));


        final List<Resource> children = Lists.newArrayList(childC, childD);
        when(resource.getChildren()).thenReturn(children);

        // when
        final PageCompareData pageCompareData = new PageCompareDataImpl(resource, "latest");

        // then
        final List<PageCompareDataLine> lines = pageCompareData.getLines();
        assertThat(lines, not(Collections.<PageCompareDataLine>emptyList()));
        assertThat(lines.get(0).getName(), is("a"));
        assertThat(lines.get(1).getName(), is("b"));
        assertThat(lines.get(2).getName(), is("c"));
        assertThat(lines.get(3).getName(), is("d"));
        assertThat(lines.get(4).getName(), is("e"));
    }

    private static Property mockProperty(final String name, final String path, final String value) throws RepositoryException {
        final Property property = mock(Property.class);
        when(property.getName()).thenReturn(name);
        when(property.getPath()).thenReturn(path);
        final Value valueMock = mock(Value.class);
        when(valueMock.getType()).thenReturn(PropertyType.STRING);
        when(valueMock.getString()).thenReturn(value);
        when(property.getValue()).thenReturn(valueMock);
        return property;
    }


    public static Resource mockResource(final String path, final String versionName, final Date date) throws RepositoryException {
        final Resource resource = mock(Resource.class);
        when(resource.getPath()).thenReturn(path);
        when(resource.getValueMap()).thenReturn(mock(ValueMap.class));
        when(resource.getChildren()).thenReturn(Lists.newArrayList());

        final Node node = mock(Node.class);
        when(resource.adaptTo(Node.class)).thenReturn(node);

        final ResourceResolver resourceResolver = mock(ResourceResolver.class);
        when(resource.getResourceResolver()).thenReturn(resourceResolver);
        when(resourceResolver.resolve(path)).thenReturn(resource);

        final Session session = mock(Session.class, Answers.RETURNS_DEEP_STUBS);
        when(resourceResolver.adaptTo(Session.class)).thenReturn(session);

        final VersionManager versionManager = mock(VersionManager.class);
        when(session.getWorkspace().getVersionManager()).thenReturn(versionManager);

        final VersionHistory history = mock(VersionHistory.class);
        when(versionManager.getVersionHistory(path)).thenReturn(history);

        final Version version = mock(Version.class, Answers.RETURNS_DEEP_STUBS);
        when(version.getName()).thenReturn(versionName);
        final GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        when(version.getCreated()).thenReturn(calendar);
        when(version.getFrozenNode().getPath()).thenReturn(path);


        final VersionIterator versionIterator = mock(VersionIterator.class);
        when(versionIterator.hasNext()).thenReturn(true, false);
        when(versionIterator.nextVersion()).thenReturn(version);
        when(history.getAllVersions()).thenReturn(versionIterator);

        return resource;
    }

}
