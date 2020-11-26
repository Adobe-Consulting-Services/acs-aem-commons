/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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
package com.adobe.acs.commons.replication.dispatcher.refetchflush.impl;

import com.day.cq.replication.AgentConfig;
import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationContent;
import com.day.cq.replication.ReplicationContentFactory;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.ReplicationLog;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import javax.jcr.Session;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RefetchFlushContentBuilderImplTest {
    @Mock
    private ReplicationContentFactory factory;

    @Mock
    private Session session;

    @Mock
    private AgentConfig config;

    @Mock
    private ReplicationAction replicationAction;

    @Mock
    private ReplicationLog replicationLog;

    @InjectMocks
    private RefetchFlushContentBuilderImpl refetchFlushContentBuilder;

    private final String[] paths = new String[]{"/content/foo", "/content/bar.html"};

    @Before
    public void setUp() {
        refetchFlushContentBuilder = new RefetchFlushContentBuilderImpl();
        MockitoAnnotations.initMocks(this);
    }

    @After
    public void tearDown() throws Exception {
        reset(factory);
        reset(session);
        reset(config);
        reset(replicationAction);
    }

    @Test
    public void testName() {
        assertEquals("flush_refetch", refetchFlushContentBuilder.getName());
        assertEquals("Dispatcher Flush Re-fetch", refetchFlushContentBuilder.getTitle());
    }

    @Test
    public void testActivationCreate() throws Exception {
        Map<String, Object> properties = new HashMap<>();
        String[] extPairs = new String[2];
        extPairs[0] = "html=header_include.html&footer_include.html";
        extPairs[1] = "htm=header_include.htm&footer_include.htm";
        properties.put("prop.extension-pairs", extPairs);
        properties.put("prop.match-paths", "*");
        refetchFlushContentBuilder.activate(properties);

        when(config.getSerializationType()).thenReturn(refetchFlushContentBuilder.getName());
        when(replicationAction.getConfig()).thenReturn(config);
        when(replicationAction.getType()).thenReturn(ReplicationActionType.ACTIVATE);

        for (String path: paths) {
            when(replicationAction.getPath()).thenReturn(path);

            refetchFlushContentBuilder.create(session, replicationAction, factory, null);

            verify(factory, times(1)).create(eq("text/plain"), any(File.class),
                    eq(true));

            reset(factory);
        }
    }

    @Test
    public void testActivationTypeTestCreate() throws Exception {
        when(config.getSerializationType()).thenReturn(refetchFlushContentBuilder.getName());
        when(replicationAction.getConfig()).thenReturn(config);
        when(replicationAction.getType()).thenReturn(ReplicationActionType.TEST);

        for (String path: paths) {
            when(replicationAction.getPath()).thenReturn(path);
            ReplicationContent content = refetchFlushContentBuilder.create(session, replicationAction, factory, null);
            assertEquals(ReplicationContent.VOID, content);

            reset(factory);
        }
    }

    @Test
    public void testActivationWrongSerializationTypeCreate() {
        when(config.getSerializationType()).thenReturn("WrongType");
        when(replicationAction.getConfig()).thenReturn(config);
        when(replicationAction.getType()).thenReturn(ReplicationActionType.ACTIVATE);
        when(replicationAction.getLog()).thenReturn(replicationLog);

        for (String path: paths) {
            when(replicationAction.getPath()).thenReturn(path);

            boolean replicationExceptionThrown = false;
            try {
                refetchFlushContentBuilder.create(session, replicationAction, factory, null);
            } catch(ReplicationException ex) {
                replicationExceptionThrown = true;
            }

            // Exception was thrown because only ACTIVATE is allowed for refetch.
            assertTrue(replicationExceptionThrown);

            reset(factory);
        }
    }

    @Test
    public void testDeactivationCreate() {
        when(replicationAction.getConfig()).thenReturn(config);
        when(replicationAction.getType()).thenReturn(ReplicationActionType.DEACTIVATE);
        when(replicationAction.getLog()).thenReturn(replicationLog);

        for (String path: paths) {
            when(replicationAction.getPath()).thenReturn(path);

            boolean replicationExceptionThrown = false;
            try {
                refetchFlushContentBuilder.create(session, replicationAction, factory, null);
            } catch(ReplicationException ex) {
                replicationExceptionThrown = true;
            }

            // Exception was thrown because only ACTIVATE is allowed for refetch.
            assertTrue(replicationExceptionThrown);

            reset(factory);
        }
    }

    @Test
    public void testActivationPathNotMatchedCreate() throws Exception {
        when(config.getSerializationType()).thenReturn("flush_refetch");
        when(replicationAction.getConfig()).thenReturn(config);
        when(replicationAction.getType()).thenReturn(ReplicationActionType.ACTIVATE);

        Map<String, Object> properties = new HashMap<>();
        properties.put("prop.match-paths", new String[]{"/content/dam/.*"});
        refetchFlushContentBuilder.activate(properties);

        for (String path: paths) {
            when(replicationAction.getPath()).thenReturn(path);

            ReplicationContent content = refetchFlushContentBuilder.create(session, replicationAction, factory, null);
            assertEquals(ReplicationContent.VOID, content);

            reset(factory);
        }
    }

    @Test
    public void testActivationWrongRegexCreate() throws Exception {
        when(config.getSerializationType()).thenReturn("flush_refetch");
        when(replicationAction.getConfig()).thenReturn(config);
        when(replicationAction.getType()).thenReturn(ReplicationActionType.ACTIVATE);

        Map<String, Object> properties = new HashMap<>();
        properties.put("prop.match-paths", new String[]{"*.k)"});
        refetchFlushContentBuilder.activate(properties);

        for (String path: paths) {
            when(replicationAction.getPath()).thenReturn(path);

            ReplicationContent content = refetchFlushContentBuilder.create(session, replicationAction, factory, null);
            assertEquals(ReplicationContent.VOID, content);

            reset(factory);
        }
    }

    @Test
    public void testActivationNonPathCreate() throws Exception {
        when(config.getSerializationType()).thenReturn(refetchFlushContentBuilder.getName());
        when(replicationAction.getConfig()).thenReturn(config);
        when(replicationAction.getType()).thenReturn(ReplicationActionType.ACTIVATE);
        when(replicationAction.getPath()).thenReturn("nonpath");

        ReplicationContent content = refetchFlushContentBuilder.create(session, replicationAction, factory, null);
        assertEquals(ReplicationContent.VOID, content);
    }

    @Test
    public void testActivationNoPathCreate() {
        final String[] paths = new String[]{"", null};
        when(replicationAction.getConfig()).thenReturn(config);
        when(replicationAction.getType()).thenReturn(ReplicationActionType.ACTIVATE);
        when(replicationAction.getLog()).thenReturn(replicationLog);

        for (String path: paths) {
            when(replicationAction.getPath()).thenReturn(path);

            boolean replicationExceptionThrown = false;
            try {
                refetchFlushContentBuilder.create(session, replicationAction, factory, null);
            } catch(ReplicationException ex) {
                replicationExceptionThrown = true;
            }

            // Exception was thrown because only ACTIVATE is allowed for refetch.
            assertTrue(replicationExceptionThrown);

            reset(factory);
        }
    }

    @Test
    public void testActivateWithProperties() {
        Map<String, Object> properties = new HashMap<>();
        String lhs = "html";
        String rhs = "header_include.html";
        properties.put("prop.extension-pairs", new String[]{lhs + "=" + rhs});
        properties.put("prop.match-paths", "/content/.*");
        refetchFlushContentBuilder.activate(properties);

        Map<String, String[]> extensionPairs = refetchFlushContentBuilder.getExtensionPairs();
        String[] lhsResult = extensionPairs.get(lhs);
        assertNotNull(lhsResult);
        assertEquals(1, lhsResult.length);
        assertEquals(rhs.split("&")[0], lhsResult[0]);

        String[] pathMatchesResult = refetchFlushContentBuilder.getPathMatches();
        assertNotNull(pathMatchesResult);
        assertEquals(1, pathMatchesResult.length);
        assertEquals(properties.get("prop.match-paths"), pathMatchesResult[0]);

        refetchFlushContentBuilder.deactivate(properties);
        extensionPairs = refetchFlushContentBuilder.getExtensionPairs();
        assertNotNull(extensionPairs);
        assertEquals(0, extensionPairs.size());

        pathMatchesResult = refetchFlushContentBuilder.getPathMatches();
        assertNotNull(pathMatchesResult);
        assertEquals(1, pathMatchesResult.length);
        assertEquals("*", pathMatchesResult[0]);
    }

    @Test
    public void testActivateEmptyProperties() {
        Map<String, Object> properties = new HashMap<>();
        refetchFlushContentBuilder.activate(properties);

        Map<String, String[]> extensionPairs = refetchFlushContentBuilder.getExtensionPairs();
        assertNotNull(extensionPairs);
        assertEquals(0, extensionPairs.size());

        String[] pathMatchesResult = refetchFlushContentBuilder.getPathMatches();
        assertNotNull(pathMatchesResult);
        assertEquals(1, pathMatchesResult.length);
        assertEquals("*", pathMatchesResult[0]);
    }
}
