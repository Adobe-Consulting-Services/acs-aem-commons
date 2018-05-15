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

import com.adobe.acs.commons.wcm.comparisons.VersionService;
import org.apache.sling.api.resource.Resource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.Version;
import javax.jcr.version.VersionIterator;
import java.util.Iterator;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@RunWith(MockitoJUnitRunner.class)
public class VersionServiceImplTest {

    VersionService underTest = new VersionServiceImpl();

    @Test
    public void lastVersion_oneVersion_returnVersion() throws Exception {
        // given
        VersionIterator versionIterator = mock(VersionIterator.class, withSettings().extraInterfaces(Iterator.class));
        when(versionIterator.hasNext()).thenReturn(true, false);

        Version last = mock(Version.class);
        when(versionIterator.next()).thenReturn(last);

        Resource resource = mockResource(versionIterator);

        // when
        Version result = underTest.lastVersion(resource);

        // then
        assertThat(result, is(last));
    }

    @Test
    public void lastVersion_noElement_returnNull() throws Exception {
        // given
        VersionIterator versionIterator = mock(VersionIterator.class, withSettings().extraInterfaces(Iterator.class));
        when(versionIterator.hasNext()).thenReturn(false);

        Resource resource = mockResource(versionIterator);

        // when
        Version result = underTest.lastVersion(resource);

        // then
        assertNull(result);
    }

    @Test
    public void lastVersion_listOfVersion_returnLast() throws Exception {
        // given
        VersionIterator versionIterator = mock(VersionIterator.class, withSettings().extraInterfaces(Iterator.class));
        when(versionIterator.hasNext()).thenReturn(true, true, true, false);

        Version one = mock(Version.class);
        Version two = mock(Version.class);
        Version three = mock(Version.class);
        when(versionIterator.next()).thenReturn(one, two, three);

        Resource resource = mockResource(versionIterator);

        // when
        Version result = underTest.lastVersion(resource);

        // then
        assertThat(result, is(three));
    }

    @Test
    public void lastVersion_error_returnNull() throws Exception {
        // given
        VersionIterator versionIterator = mock(VersionIterator.class, withSettings().extraInterfaces(Iterator.class));
        when(versionIterator.hasNext()).thenThrow(RepositoryException.class);

        Resource resource = mockResource(versionIterator);

        // when
        Version result = underTest.lastVersion(resource);

        // then
        assertNull(result);
    }

    private Resource mockResource(VersionIterator versionIterator) throws RepositoryException {
        Resource resource = mock(Resource.class, RETURNS_DEEP_STUBS.get());
        Session session = mock(Session.class, RETURNS_DEEP_STUBS.get());
        when(resource.getResourceResolver().adaptTo(Session.class)).thenReturn(session);
        when(session.getWorkspace().getVersionManager().getVersionHistory(anyString()).getAllVersions()).thenReturn(versionIterator);
        return resource;
    }

}