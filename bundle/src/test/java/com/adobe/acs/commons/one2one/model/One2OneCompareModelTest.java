package com.adobe.acs.commons.one2one.model;

import com.adobe.acs.commons.version.Evolution;
import com.adobe.acs.commons.version.EvolutionAnalyser;
import com.adobe.acs.commons.version.EvolutionContext;
import com.google.common.collect.Lists;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Date;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class One2OneCompareModelTest {

    @Mock
    private ResourceResolver resolver;

    @Mock
    private EvolutionAnalyser analyser;

    One2OneCompareModel underTest;

    @Test
    public void constructor_shouldInitializeWithParams() throws Exception {
        // given
        final String pathA = "/path/a";
        final String versionA = "1.0";
        final String pathB = "/path/b";
        final String versionB = "2.0";

        construct(pathA, versionA, pathB, versionB);

        // then
        assertThat(underTest.getResourcePathA(), is(pathA));
        assertThat(underTest.getResourcePathB(), is(pathB));
        assertThat(underTest.getVersionA(), is(versionA));
        assertThat(underTest.getVersionB(), is(versionB));
    }

    @Test
    public void activate_noPath_shouldNotInitializeLists() throws Exception {
        // given
        construct(null, null, null, null);

        // when
        underTest.activate();

        // then
        assertTrue(underTest.evolutionsA.isEmpty());
        assertThat(underTest.evolutionsB, is(underTest.evolutionsA));
    }

    @Test
    public void activate_pathA_shouldNotInitializeSecontList() throws Exception {
        // given
        construct("/path/a", "", null, "");

        final Evolution evolutionA = mock(Evolution.class);
        List<Evolution> evolutionsA = Lists.newArrayList(evolutionA);
        prepareAnalyzer("/path/a", evolutionsA);

        // when
        underTest.activate();

        // then
        assertThat(underTest.evolutionsA.first().get(), is(evolutionA));
        assertThat(underTest.evolutionsB, is(underTest.evolutionsA));
    }

    @Test
    public void activate_pathA_pathB_shouldInitializeLists() throws Exception {
        // given
        construct("/path/a", "", "/path/b", "");

        final Evolution evolutionA = mock(Evolution.class);
        List<Evolution> evolutionsA = Lists.newArrayList(evolutionA);
        prepareAnalyzer("/path/a", evolutionsA);

        final Evolution evolutionB = mock(Evolution.class);
        List<Evolution> evolutionsB = Lists.newArrayList(evolutionB);
        prepareAnalyzer("/path/b", evolutionsB);

        // when
        underTest.activate();

        // then
        assertThat(underTest.evolutionsA.first().get(), is(evolutionA));
        assertThat(underTest.evolutionsB.first().get(), is(evolutionB));
    }

    @Test
    public void getResourcePathA_null_shouldReturnNull() throws Exception {
        construct(null, null, null, null);
        assertNull(underTest.getResourcePathA());
    }

    @Test
    public void getResourcePathA_validPath_shoulReturnPath() throws Exception {
        final String pathA = "/path/a";
        construct(pathA, null, null, null);
        assertThat(underTest.getResourcePathA(), is(pathA));
    }

    @Test
    public void getResourcePathB_null_shouldReturnEmptyString() throws Exception {
        construct(null, null, null, null);
        assertThat(underTest.getResourcePathB(), is(""));
    }

    @Test
    public void getResourcePathB_validPath_shoulReturnPath() throws Exception {
        final String pathB = "/path/b";
        construct(null, null, pathB, null);
        assertThat(underTest.getResourcePathB(), is(pathB));
    }

    @Test
    public void getVersionSelectionsA_noVersions_shouldReturnEmptyList() throws Exception {
        construct("/path/a", null, null, null);
        underTest.activate();

        List<VersionSelection> versionSelections = underTest.getVersionSelectionsA();
        assertTrue(versionSelections.isEmpty());
    }

    @Test
    public void getVersionSelectionsA_versions_shouldReturnList() throws Exception {
        construct("/path/a", null, null, null);

        List<Evolution> evolutionsA = mockEvolutions("1.0", "2.0");
        prepareAnalyzer("/path/a", evolutionsA);

        underTest.activate();

        List<VersionSelection> versionSelections = underTest.getVersionSelectionsA();
        assertThat(versionSelections.size(), is(2));
        assertThat(versionSelections.get(0).getName(), is("1.0"));
        assertThat(versionSelections.get(1).getName(), is("2.0"));
    }

    @Test
    public void getVersionSelectionsB_noVersions_shouldReturnEmptyList() throws Exception {
        construct("/path/a", null, "/path/b", null);
        underTest.activate();

        List<VersionSelection> versionSelections = underTest.getVersionSelectionsB();

        assertTrue(versionSelections.isEmpty());
    }

    @Test
    public void getVersionSelectionsB_versions_shouldReturnList() throws Exception {
        construct("/path/a", null, "/path/b", null);
        List<Evolution> evolutionsB = mockEvolutions("1.0", "2.0");
        prepareAnalyzer("/path/b", evolutionsB);
        underTest.activate();

        List<VersionSelection> versionSelections = underTest.getVersionSelectionsB();

        assertThat(versionSelections.size(), is(2));
        assertThat(versionSelections.get(0).getName(), is("1.0"));
        assertThat(versionSelections.get(1).getName(), is("2.0"));
    }

    @Test
    public void getVersionSelectionsB_noPathB_shouldReturnListFromA() throws Exception {
        construct("/path/a", null, null, null);
        List<Evolution> evolutionsA = mockEvolutions("1.0", "2.0");
        prepareAnalyzer("/path/a", evolutionsA);
        underTest.activate();

        List<VersionSelection> versionSelections = underTest.getVersionSelectionsB();

        assertThat(versionSelections.size(), is(2));
        assertThat(versionSelections.get(0).getName(), is("1.0"));
        assertThat(versionSelections.get(1).getName(), is("2.0"));
    }

    @Test
    public void getEvolutionsA_noEvolutions_returnNull() throws Exception {
        construct("/path/a", null, null, null);
        underTest.activate();

        final Evolution evolutionA = underTest.getEvolutionA();

        assertNull(evolutionA);
    }

    @Test
    public void getEvolutionsA_evolutionsNoVersion_returnFirst() throws Exception {
        construct("/path/a", null, null, null);
        List<Evolution> evolutionsA = mockEvolutions("1.0", "2.0");
        prepareAnalyzer("/path/a", evolutionsA);
        underTest.activate();

        final Evolution evolutionA = underTest.getEvolutionA();

        assertNotNull(evolutionA);
        assertThat(evolutionA.getVersionName(), is("1.0"));
    }

    @Test
    public void getEvolutionsA_wrongVersion_returnNull() throws Exception {
        construct("/path/a", "3.0", null, null);
        List<Evolution> evolutionsA = mockEvolutions("1.0", "2.0");
        prepareAnalyzer("/path/a", evolutionsA);
        underTest.activate();

        final Evolution evolutionA = underTest.getEvolutionA();

        assertNull(evolutionA);
    }

    @Test
    public void getEvolutionsA_evolutionsAndVersion_returnVersion() throws Exception {
        construct("/path/a", "2.0", null, null);
        List<Evolution> evolutionsA = mockEvolutions("1.0", "2.0");
        prepareAnalyzer("/path/a", evolutionsA);
        underTest.activate();

        final Evolution evolutionA = underTest.getEvolutionA();

        assertNotNull(evolutionA);
        assertThat(evolutionA.getVersionName(), is("2.0"));
    }

    @Test
    public void geEvolutionB_noVersion_returnLast() throws Exception {
        construct("/path/a", "2.0", "/path/b", null);
        List<Evolution> evolutionsA = mockEvolutions("1.0", "2.0");
        prepareAnalyzer("/path/b", evolutionsA);
        underTest.activate();

        final Evolution evolutionB = underTest.getEvolutionB();

        assertNotNull(evolutionB);
        assertThat(evolutionB.getVersionName(), is("2.0"));
    }

    @Test
    public void geEvolutionB_wrongVersion_returnNull() throws Exception {
        construct("/path/a", "2.0", "/path/b", "3.0");
        List<Evolution> evolutionsA = mockEvolutions("1.0", "2.0");
        prepareAnalyzer("/path/b", evolutionsA);
        underTest.activate();

        final Evolution evolutionB = underTest.getEvolutionB();

        assertNull(evolutionB);
    }

    @Test
    public void geEvolutionB_version_returnVersion() throws Exception {
        construct("/path/a", "2.0", "/path/b", "1.0");
        List<Evolution> evolutionsA = mockEvolutions("1.0", "2.0");
        prepareAnalyzer("/path/b", evolutionsA);
        underTest.activate();

        final Evolution evolutionB = underTest.getEvolutionB();

        assertNotNull(evolutionB);
        assertThat(evolutionB.getVersionName(), is("1.0"));
    }

    @Test
    public void getVersionA() throws Exception {
        construct("/path/a", "2.0", "/path/b", "1.0");
        underTest.activate();

        String versionA = underTest.getVersionA();

        assertThat(versionA, is("2.0"));
    }

    @Test
    public void getVersionB() throws Exception {
        construct("/path/a", "2.0", "/path/b", "1.0");
        underTest.activate();

        String versionB = underTest.getVersionB();

        assertThat(versionB, is("1.0"));
    }

    private List<Evolution> mockEvolutions(String versionName1, String versionName2) {
        final Evolution evolution1 = mockEvolution(versionName1);
        final Evolution evolution2 = mockEvolution(versionName2);

        return Lists.newArrayList(evolution1, evolution2);
    }

    private Evolution mockEvolution(String versionName) {
        final Evolution evolution = mock(Evolution.class);
        when(evolution.getVersionName()).thenReturn(versionName);
        when(evolution.getVersionDate()).thenReturn(new Date());
        return evolution;
    }

    private void construct(String pathA, String versionA, String pathB, String versionB) {
        SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
        when(request.getParameter("path")).thenReturn(pathA);
        when(request.getParameter("pathB")).thenReturn(pathB);
        when(request.getParameter("a")).thenReturn(versionA);
        when(request.getParameter("b")).thenReturn(versionB);
        underTest = new One2OneCompareModel(request);
        underTest.resolver = resolver;
        underTest.analyser = analyser;
    }

    private void prepareAnalyzer(String path, List<Evolution> evolutionsA) {
        Resource resource = mock(Resource.class);
        when(resolver.resolve(path)).thenReturn(resource);
        EvolutionContext evolutionContext = mock(EvolutionContext.class);
        when(evolutionContext.getEvolutionItems()).thenReturn(evolutionsA);
        when(analyser.getEvolutionContext(resource)).thenReturn(evolutionContext);
    }
}