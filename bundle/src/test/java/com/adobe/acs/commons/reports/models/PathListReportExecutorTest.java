/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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
package com.adobe.acs.commons.reports.models;

import com.adobe.acs.commons.reports.api.ReportException;
import com.adobe.acs.commons.reports.api.ResultsPage;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

public class PathListReportExecutorTest {

    private static final String HANDLEBARS_PARAM = "{{paths}}";
    private static final String PATH_1 = "/this/is/path1";
    private static final String PATH_2 = "/this/is/path/2";
    private static final String WINDOWS_END_LINE = "\r\n";
    private static final String PATH_3 = "/this/is/path/3";
    private static final String PATH_4 = "/this/is/path/4";
    private static final String PATH_5 = "/this/is/path/5";
    private static final List<String> TEST_PATHS = Arrays.asList(PATH_1, PATH_2, PATH_3, PATH_4, PATH_5);
    private static final String LINUX_END_LINE = "\n";
    private static final String PATHS = "paths";

    @Test
    public void testGetDetailsReturnsEmpty() throws ReportException {
        assertEquals(StringUtils.EMPTY, new PathListReportExecutor().getDetails());
    }

    @Test
    public void testGetParameterReturnsEmpty() throws ReportException {
        assertEquals(StringUtils.EMPTY, new PathListReportExecutor().getParameters());
    }

    @Test
    public void testExtractPaths() throws ReportException {
        testExtractedPaths(PATH_1 + WINDOWS_END_LINE + PATH_2 + WINDOWS_END_LINE + PATH_3, new HashMap<>());
        testExtractedPaths(PATH_1 + LINUX_END_LINE + PATH_2 + LINUX_END_LINE + PATH_3, new HashMap<>());
    }

    @Test
    public void testExtractPathsWithParams() throws ReportException {
        HashMap<String, String> params = new HashMap<>();
        params.put(PATHS, PATH_1 + LINUX_END_LINE + PATH_2 + LINUX_END_LINE + PATH_3);
        testExtractedPaths(HANDLEBARS_PARAM, params);

        params = new HashMap<>();
        params.put(PATHS, PATH_1 + WINDOWS_END_LINE + PATH_2 + WINDOWS_END_LINE + PATH_3);
        testExtractedPaths(HANDLEBARS_PARAM, params);
    }

    private void testExtractedPaths(final String pathAreaContent, final HashMap<String, String> params) throws ReportException {
        PathListReportExecutor reportExecutor = spy(new PathListReportExecutor());
        doReturn(params).when(reportExecutor).getParamPatternMap(any());

        PathListReportConfig config = mock(PathListReportConfig.class);
        doReturn(pathAreaContent).when(config).getPathArea();
        reportExecutor.config = config;

        final List<String> expectedThreePaths = TEST_PATHS.subList(0, 3);
        assertEquals(expectedThreePaths, reportExecutor.extractPaths());
    }

    @Test
    public void testGetAllResults() throws ReportException {
        ResultsTestObject resultsTestObject = new ResultsTestObject(50, 0, TEST_PATHS, TEST_PATHS).configure();
        assertEquals(resultsTestObject.getResultsPage(), resultsTestObject.getReportExecutor().getAllResults());
    }

    @Test
    public void testGetResultsAllItemsInFirstPage() throws ReportException {
        ResultsTestObject resultsTestObject = new ResultsTestObject(50, 0, TEST_PATHS, TEST_PATHS).configure();
        assertEquals(resultsTestObject.getResultsPage(), resultsTestObject.getReportExecutor().getResults());
    }

    @Test
    public void testGetResultsFirstPageWhenMoreAvailable() throws ReportException {
        ResultsTestObject resultsTestObject = new ResultsTestObject(2, 0, TEST_PATHS, TEST_PATHS.subList(0, 2)).configure();
        assertEquals(resultsTestObject.getResultsPage(), resultsTestObject.getReportExecutor().getResults());
    }

    @Test
    public void testGetResultsLastPageWhenMoreAvailable() throws ReportException {
        ResultsTestObject resultsTestObject = new ResultsTestObject(2, 2, TEST_PATHS, TEST_PATHS.subList(4, 5)).configure();
        assertEquals(resultsTestObject.getResultsPage(), resultsTestObject.getReportExecutor().getResults());
    }

    @Test
    public void testGetResultsMiddlePageWhenMoreAvailable() throws ReportException {
        ResultsTestObject resultsTestObject = new ResultsTestObject(2, 1, TEST_PATHS, TEST_PATHS.subList(2, 4)).configure();
        assertEquals(resultsTestObject.getResultsPage(), resultsTestObject.getReportExecutor().getResults());
    }

    @Test
    public void testGetResourcesNullOrEmptyList() {
        PathListReportExecutor executor = new PathListReportExecutor();
        assertEquals(Collections.EMPTY_LIST, executor.getResources(null));
        assertEquals(Collections.EMPTY_LIST, executor.getResources(new ArrayList<>()));
    }

    @Test
    public void testGetResourcesReturnEmptyListWhenResourceNotFound() {
        PathListReportExecutor executor = new PathListReportExecutor();
        executor.resourceResolver = mock(ResourceResolver.class);
        assertEquals(Collections.EMPTY_LIST, executor.getResources(TEST_PATHS));
    }

    @Test
    public void testGetResources() {
        PathListReportExecutor executor = new PathListReportExecutor();
        final ResourceResolver rr = mock(ResourceResolver.class);

        ArrayList<Object> expectedResult = new ArrayList<>();
        for (String path : TEST_PATHS) {
            final Resource resource = mock(Resource.class);
            doReturn(resource).when(rr).getResource(path);
            expectedResult.add(resource);
        }
        executor.resourceResolver = rr;

        assertEquals(expectedResult, executor.getResources(TEST_PATHS));
    }

    @Test
    public void testSetPageReturnsZeroWhenNonPositiveNumberProvided() {
        PathListReportExecutor executor = new PathListReportExecutor();

        executor.setPage(0);
        assertEquals(0, executor.currentPage);
        executor.setPage(-2);
        assertEquals(0, executor.currentPage);
        executor.setPage(-5);
        assertEquals(0, executor.currentPage);
        executor.setPage(-100);
        assertEquals(0, executor.currentPage);
    }

    private class ResultsTestObject {
        private final List<String> providedPaths;
        private final List<String> expectedPaths;
        private ResultsPage resultsPage;
        private PathListReportExecutor reportExecutor;
        private int pageSize;
        private final int expectedCurrentPage;

        ResultsTestObject(final int pageSize, final int expectedCurrentPage,
                          final List<String> providedPaths, final List<String> expectedPaths) {
            this.pageSize = pageSize;
            this.expectedCurrentPage = expectedCurrentPage;
            this.providedPaths = providedPaths;
            this.expectedPaths = expectedPaths;
        }


        ResultsPage getResultsPage() {
            return resultsPage;
        }

        PathListReportExecutor getReportExecutor() {
            return reportExecutor;
        }

        ResultsTestObject configure() {
            PathListReportConfig config = mock(PathListReportConfig.class);
            doReturn(pageSize).when(config).getPageSize();

            List<Object> expectedResources = new ArrayList<>();

            final ResourceResolver rr = mock(ResourceResolver.class);
            for (String path : expectedPaths) {
                final Resource resource = mock(Resource.class);
                doReturn(resource).when(rr).getResource(path);
                expectedResources.add(resource);
            }

            resultsPage = new ResultsPage(expectedResources, pageSize, expectedCurrentPage);

            reportExecutor = new PathListReportExecutor() {
                @Override
                List<String> extractPaths() {
                    return providedPaths;
                }
            };

            reportExecutor.config = config;
            reportExecutor.currentPage = expectedCurrentPage;
            reportExecutor.resourceResolver = rr;

            return this;
        }
    }
}
