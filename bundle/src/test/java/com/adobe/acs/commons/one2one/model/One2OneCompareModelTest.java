/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
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
package com.adobe.acs.commons.one2one.model;

import com.adobe.acs.commons.one2one.One2OneData;
import com.adobe.acs.commons.one2one.One2OneDataLine;
import com.adobe.acs.commons.one2one.One2OneDataLoader;
import com.adobe.acs.commons.one2one.impl.VersionSelection;
import com.adobe.acs.commons.one2one.lines.Line;
import com.google.common.collect.Lists;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.List;

import static com.adobe.acs.commons.one2one.lines.Line.State.EQUAL;
import static com.adobe.acs.commons.one2one.lines.Line.State.NOT_EQUAL;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;



@RunWith(MockitoJUnitRunner.class)
public class One2OneCompareModelTest {

    @Mock
    private ResourceResolver resolver;

    @Mock
    private One2OneDataLoader loader;

    One2OneCompareModel underTest;

    @Captor
    ArgumentCaptor<Resource> resourceArgumentCaptor;

    @Test
    public void constructor_shouldInitializeWithParams() throws Exception {
        // given
        final String pathA = "/path/a";
        final String versionA = "1.0";
        final String pathB = "/path/b";
        final String versionB = "2.0";

        // when
        construct(pathA, versionA, pathB, versionB);

        // then
        assertThat(underTest.getPathA(), is(pathA));
        assertThat(underTest.getVersionA(), is(versionA));
        assertThat(underTest.getPathB(), is(pathB));
        assertThat(underTest.getVersionB(), is(versionB));
    }

    @Test
    public void activate_noPath_shouldNotInitializeData() throws Exception {
        // given
        construct(null, null, null, null);

        // when
        underTest.activate();

        // then
        assertNull(underTest.getA());
        assertNull(underTest.getB());
    }

    @Test
    public void activate_pathAOnly_copyDataToB() throws Exception {
        // given
        construct("/path/a", "", null, "");

        // when
        underTest.activate();

        // then
        verify(underTest.loader, times(2)).load(resourceArgumentCaptor.capture(), anyString());
        final List<Resource> resources = resourceArgumentCaptor.getAllValues();
        assertThat(resources.size(), is(2));
        assertThat(resources.get(0), is(resources.get(1)));
    }

    @Test
    public void getData_noA_emptyList() throws Exception {
        // given
        construct(null, null, null, null);
        underTest.activate();

        // when
        final List<Line<One2OneDataLine>> data = underTest.getData();

        // then
        assertThat(data.isEmpty(), is(true));
    }

    @Test
    public void getData_A_generateLines_useAAsB() throws Exception {
        // given
        construct("/path/a", null, null, null);
        underTest.activate();

        // when
        final List<Line<One2OneDataLine>> data = underTest.getData();

        // then
        assertThat(data.size(), is(2));
        assertThat(data.get(0).getLeft(), is(data.get(0).getRight()));
        assertThat(data.get(1).getLeft(), is(data.get(1).getRight()));
        assertThat(data.get(0).getState(), is(EQUAL));
        assertThat(data.get(1).getState(), is(EQUAL));
    }

    @Test
    public void getData_aAndB_generateLines() throws Exception {
        // given
        construct("/path/a", null, "/path/b", null);
        underTest.activate();

        // when
        final List<Line<One2OneDataLine>> data = underTest.getData();

        // then
        assertThat(data.size(), is(2));
        assertThat(data.get(0).getLeft(), is(not(data.get(0).getRight())));
        assertThat(data.get(1).getLeft(), is(not(data.get(1).getRight())));
        assertThat(data.get(0).getState(), is(NOT_EQUAL));
        assertThat(data.get(1).getState(), is(NOT_EQUAL));
    }

    @Test
    public void getData_A_generateVersionLists() throws Exception {
        // given
        construct("/path/a", null, "/path/b", null);
        underTest.activate();

        // when
        final List<VersionSelection> versionsA = underTest.getA().getVersions();
        final List<VersionSelection> versionsB = underTest.getB().getVersions();

        // then
        assertThat(versionsA.size(), is(2));
        assertThat(versionsB.size(), is(2));
    }

    @Test
    public void getData_A_repositoryError_getEmptyList() throws Exception {
        // given
        construct("/path/a", null, null, null);

        when(loader.load(any(Resource.class), anyString())).thenThrow(new RepositoryException());
        underTest.activate();

        // when
        final List<Line<One2OneDataLine>> data = underTest.getData();

        // then
        assertThat(data.size(), is(0));
    }


    private void construct(String pathA, String versionA, String pathB, String versionB) throws RepositoryException {
        SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
        when(request.getParameter("path")).thenReturn(pathA);
        when(request.getParameter("pathB")).thenReturn(pathB);
        when(request.getParameter("a")).thenReturn(versionA);
        when(request.getParameter("b")).thenReturn(versionB);

        mockOne2OneData(pathA, versionA);
        mockOne2OneData(pathB, versionB);

        underTest = new One2OneCompareModel(request);
        underTest.resolver = resolver;
        underTest.loader = loader;
    }

    private One2OneData mockOne2OneData(String pathA, String version) throws RepositoryException {
        Resource resource = mock(Resource.class);
        when(resolver.resolve(pathA)).thenReturn(resource);
        One2OneData one2OneData = mock(One2OneData.class);
        final ArrayList<One2OneDataLine> lines = Lists.newArrayList(
                mockOne2OneDataLine("a"),
                mockOne2OneDataLine("b"));
        when(one2OneData.getLines()).thenReturn(lines);
        final ArrayList<VersionSelection> versionSelections = Lists.newArrayList(
                mock(VersionSelection.class),
                mock(VersionSelection.class));
        when(one2OneData.getVersions()).thenReturn(versionSelections);
        when(loader.load(resource, version != null ? version : "latest")).thenReturn(one2OneData);
        return one2OneData;
    }

    private One2OneDataLine mockOne2OneDataLine(String uniqueName) {
        One2OneDataLine one2OneDataLine = mock(One2OneDataLine.class);
        when(one2OneDataLine.getUniqueName()).thenReturn(uniqueName);
        return one2OneDataLine;
    }


}