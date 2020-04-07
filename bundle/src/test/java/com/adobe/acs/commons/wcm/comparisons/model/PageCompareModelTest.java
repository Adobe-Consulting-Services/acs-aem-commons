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
package com.adobe.acs.commons.wcm.comparisons.model;

import com.adobe.acs.commons.wcm.comparisons.PageCompareData;
import com.adobe.acs.commons.wcm.comparisons.PageCompareDataLine;
import com.adobe.acs.commons.wcm.comparisons.PageCompareDataLines;
import com.adobe.acs.commons.wcm.comparisons.PageCompareDataLoader;
import com.adobe.acs.commons.wcm.comparisons.VersionSelection;
import com.adobe.acs.commons.wcm.comparisons.VersionService;
import com.adobe.acs.commons.wcm.comparisons.lines.Line;
import com.google.common.collect.Lists;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.List;

import static com.adobe.acs.commons.wcm.comparisons.lines.Line.State.NOT_EQUAL;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class PageCompareModelTest {

    @Mock
    private ResourceResolver resolver;

    @Mock
    private PageCompareDataLoader loader;

    @Mock
    private PageCompareDataLines lines;

    @Mock
    private VersionService versionService;

    PageCompareModel underTest;

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
        final List<Line<PageCompareDataLine>> data = underTest.getData();

        // then
        assertThat(data.isEmpty(), is(true));
    }

    @Test
    public void getData_aAndB_generateLines() throws Exception {
        // given
        construct("/path/a", null, "/path/b", null);
        underTest.activate();

        // when
        final List<Line<PageCompareDataLine>> data = underTest.getData();

        // then
        assertThat(data.size(), is(1));
        assertThat(data.get(0).getLeft(), is(not(data.get(0).getRight())));
        assertThat(data.get(0).getState(), is(NOT_EQUAL));
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
        final List<Line<PageCompareDataLine>> data = underTest.getData();

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

        underTest = new PageCompareModel(request);
        underTest.resolver = resolver;
        underTest.loader = loader;
        underTest.lines = lines;
        underTest.versionService = versionService;
    }

    private PageCompareData mockOne2OneData(String pathA, String version) throws RepositoryException {
        Resource resource = mock(Resource.class);
        when(resolver.resolve(pathA)).thenReturn(resource);
//        when(resource.getResourceResolver()).thenReturn(resolver);

        PageCompareData pageCompareData = mock(PageCompareData.class);
        final List<Line<PageCompareDataLine>> lineResult = Lists.newArrayList(
                mockOne2OneDataLine("a", "b"));
        when(lines.generate(anyCollection(), anyCollection())).thenReturn(lineResult);
        final ArrayList<VersionSelection> versionSelections = Lists.newArrayList(
                mock(VersionSelection.class),
                mock(VersionSelection.class));
        when(pageCompareData.getVersions()).thenReturn(versionSelections);
        when(loader.load(resource, version != null ? version : "latest")).thenReturn(pageCompareData);
        return pageCompareData;
    }

    private Line<PageCompareDataLine> mockOne2OneDataLine(String left, String right) {
        PageCompareDataLine leftLine = mock(PageCompareDataLine.class);
//        when(leftLine.getUniqueName()).thenReturn(left);
        PageCompareDataLine rightLine = mock(PageCompareDataLine.class);
//        when(rightLine.getUniqueName()).thenReturn(right);

        Line<PageCompareDataLine> line = mock(Line.class);
        when(line.getLeft()).thenReturn(leftLine);
        when(line.getRight()).thenReturn(rightLine);
        when(line.getState()).thenReturn(Line.State.NOT_EQUAL);
        return line;
    }


}