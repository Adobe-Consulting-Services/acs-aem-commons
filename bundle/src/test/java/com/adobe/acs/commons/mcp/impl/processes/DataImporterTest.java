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
package com.adobe.acs.commons.mcp.impl.processes;

import com.adobe.acs.commons.data.Spreadsheet;
import com.adobe.acs.commons.fam.ActionManagerFactory;
import com.adobe.acs.commons.fam.ThrottledTaskRunner;
import com.adobe.acs.commons.fam.impl.ActionManagerFactoryImpl;
import com.adobe.acs.commons.fam.impl.ThrottledTaskRunnerImpl;
import com.adobe.acs.commons.mcp.ControlledProcessManager;
import com.adobe.acs.commons.mcp.impl.ControlledProcessManagerImpl;
import com.adobe.acs.commons.mcp.impl.ProcessInstanceImpl;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import javax.jcr.RepositoryException;
import javax.management.NotCompliantMBeanException;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Test some of the data handling features of the Data Importer tool
 */
public class DataImporterTest {
    private static Spreadsheet importerData;

    @Rule
    public final SlingContext slingContext = new SlingContext(ResourceResolverType.JCR_OAK);

    private DataImporter importer;
    private ActionManagerFactory actionManagerFactory;
    private ProcessInstanceImpl process;
    private ControlledProcessManager cpm;
    private ResourceResolver rr;

    @BeforeClass
    public static void setUp() throws IOException {
        InputStream importerInput = DataImporterTest.class.getResourceAsStream("/com/adobe/acs/commons/mcp/impl/processes/data-importer.xlsx");
        importerData = new Spreadsheet(false, importerInput).buildSpreadsheet();
    }

    @Before
    public void init() throws NotCompliantMBeanException, LoginException, PersistenceException, RepositoryException {
        // Configure resource resolver to never close and pass-through clone requests
        rr = spy(slingContext.resourceResolver());
        doNothing().when(rr).close();
        when(rr.clone(any())).thenReturn(rr);

        // Configure FAM task runner and hotwire it to run things in the main thread
        ThrottledTaskRunner runner = spy(new ThrottledTaskRunnerImpl());
        doAnswer(this::runImmediately).when(runner).scheduleWork(any());
        doAnswer(this::runImmediately).when(runner).scheduleWork(any(), any());
        doAnswer(this::runImmediately).when(runner).scheduleWork(any(), anyInt());
        doAnswer(this::runImmediately).when(runner).scheduleWork(any(), any(), anyInt());
        slingContext.registerInjectActivateService(runner);

        // Set up FAM action manager factory
        actionManagerFactory = new ActionManagerFactoryImpl();
        slingContext.registerInjectActivateService(actionManagerFactory);

        // Register at least one action manager factory so MCP starts
        slingContext.registerInjectActivateService(new DataImporterFactory());

        // Start MCP service
        cpm = new ControlledProcessManagerImpl();
        slingContext.registerInjectActivateService(cpm);

        // Configure process and get it ready to use
        importer = spy(new DataImporter());
        doNothing().when(importer).storeReport(any(), any());
        importer.data = importerData;
        importer.dryRunMode = false;
        process = spy(new ProcessInstanceImpl(cpm, importer, "test"));
        doNothing().when(process).persistStatus(any());
    }

    private Object runImmediately(InvocationOnMock invocation) {
        Runnable r = invocation.getArgument(0);
        r.run();
        return null;
    }

    @Test
    public void assertCreatedNodes() throws LoginException, RepositoryException {
        importer.buildProcess(process, rr);
        process.run(rr);
        assertNotNull("Node1 wasn't created", rr.getResource("/tmp/node1"));
        assertNotNull("Node2 wasn't created", rr.getResource("/tmp/node2"));
    }

    @Test
    public void assertHintedTypes() throws LoginException, RepositoryException {
        importer.buildProcess(process, rr);
        process.run(rr);
        ValueMap values = rr.getResource("/tmp/node1").getValueMap();
        assertEquals((Long) 1234L, values.get("int2", Long.class));
        assertNotNull(values.get("date3", Calendar.class));
        assertNotNull(values.get("date4", Calendar.class));
        assertNotNull(values.get("date5", Calendar.class));
        assertNotNull(values.get("date6", Calendar.class));
    }

    @Test
    public void assertDetectedTypes() throws LoginException, RepositoryException {
        importer.buildProcess(process, rr);
        process.run(rr);
        ValueMap values = rr.getResource("/tmp/node1").getValueMap();
        assertEquals((Long) 123L, values.get("int1", Long.class));
        assertEquals(123.456, values.get("double1", Double.class), 0.001);
        assertEquals("123.456",values.get("double2str", String.class));
        Calendar cal = values.get("date1", Calendar.class);
        assertEquals(1985, cal.get(Calendar.YEAR));
        assertEquals(Calendar.NOVEMBER, cal.get(Calendar.MONTH));
        assertEquals((Long) 26L, new Long(cal.get(Calendar.DAY_OF_MONTH)));
        cal = values.get("date2", Calendar.class);
        assertEquals(1985, cal.get(Calendar.YEAR));
        assertEquals(Calendar.NOVEMBER, cal.get(Calendar.MONTH));
        assertEquals(26, cal.get(Calendar.DAY_OF_MONTH));
        cal = values.get("time", Calendar.class);
        assertEquals(9, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, cal.get(Calendar.MINUTE));
        assertEquals(0, cal.get(Calendar.SECOND));
    }

}
