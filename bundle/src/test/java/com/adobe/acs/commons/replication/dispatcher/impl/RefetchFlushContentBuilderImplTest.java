/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */
package com.adobe.acs.commons.replication.dispatcher.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Map;

import javax.jcr.Session;

import com.day.cq.replication.AgentConfig;
import com.day.cq.replication.ContentBuilder;
import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationContent;
import com.day.cq.replication.ReplicationContentFactory;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.ReplicationLog;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.wcm.testing.mock.aem.junit.AemContext;

@RunWith(MockitoJUnitRunner.class)
public class RefetchFlushContentBuilderImplTest {

    @Rule
    public AemContext ctx = new AemContext();

    @Mock
    private ReplicationContentFactory replicationContentFactory;

    @Mock
    private AgentConfig agentConfig;

    @Mock
    private ReplicationAction replicationAction;

    @Mock
    private ReplicationLog replicationLog;

    private final String[] paths = new String[]{"/content/foo", "/content/bar.html"};

    private RefetchFlushContentBuilderImpl refetchFlushContentBuilder;

    @Before
    public void setUp() {
        refetchFlushContentBuilder = new RefetchFlushContentBuilderImpl();

        ctx.registerService(ReplicationContentFactory.class, replicationContentFactory);
        ctx.registerService(AgentConfig.class, agentConfig);
        ctx.registerService(ReplicationAction.class, replicationAction);
        ctx.registerService(ReplicationLog.class, replicationLog);
    }

    @Test
    public void testName() {
        ctx.registerInjectActivateService(refetchFlushContentBuilder);
        ContentBuilder actual = ctx.getService(ContentBuilder.class);

        assertNotNull(actual);
        assertEquals("flush_refetch", actual.getName());
        assertEquals("Dispatcher Flush Re-fetch", actual.getTitle());
    }

    @Test
    public void testActivationCreate() throws Exception {
        ctx.registerInjectActivateService(refetchFlushContentBuilder,
            "extension.pairs", new String[]{ "html=header_include.html&footer_include.html", "htm=header_include.htm&footer_include.htm" },
            "match.paths", "*");

        ContentBuilder actual = ctx.getService(ContentBuilder.class);

        assertNotNull(actual);

        when(agentConfig.getSerializationType()).thenReturn(actual.getName());
        when(replicationAction.getConfig()).thenReturn(agentConfig);
        when(replicationAction.getType()).thenReturn(ReplicationActionType.ACTIVATE);

        for (String path: paths) {
            when(replicationAction.getPath()).thenReturn(path);

            actual.create(ctx.resourceResolver().adaptTo(Session.class), replicationAction, replicationContentFactory, null);

            verify(replicationContentFactory, times(1)).create(eq("text/plain"), any(File.class),
                    eq(true));

            reset(replicationContentFactory);
        }
    }

    @Test
    public void testActivationTypeTestCreate() throws Exception {
        ctx.registerInjectActivateService(refetchFlushContentBuilder);
        RefetchFlushContentBuilderImpl actual = (RefetchFlushContentBuilderImpl) ctx.getService(ContentBuilder.class);

        assertNotNull(actual);

        when(agentConfig.getSerializationType()).thenReturn(refetchFlushContentBuilder.getName());
        when(replicationAction.getConfig()).thenReturn(agentConfig);
        when(replicationAction.getType()).thenReturn(ReplicationActionType.TEST);

        for (String path: paths) {
            when(replicationAction.getPath()).thenReturn(path);
            ReplicationContent content = actual.create(ctx.resourceResolver().adaptTo(Session.class), replicationAction, replicationContentFactory, null);
            assertEquals(ReplicationContent.VOID, content);

            reset(replicationContentFactory);
        }
    }

    @Test
    public void testActivationWrongSerializationTypeCreate() {
        ctx.registerInjectActivateService(refetchFlushContentBuilder);
        RefetchFlushContentBuilderImpl actual = (RefetchFlushContentBuilderImpl) ctx.getService(ContentBuilder.class);

        assertNotNull(actual);

        when(agentConfig.getSerializationType()).thenReturn("WrongType");
        when(replicationAction.getConfig()).thenReturn(agentConfig);
        when(replicationAction.getType()).thenReturn(ReplicationActionType.ACTIVATE);
        when(replicationAction.getLog()).thenReturn(replicationLog);

        for (String path: paths) {
            when(replicationAction.getPath()).thenReturn(path);

            boolean replicationExceptionThrown = false;
            try {
                actual.create(ctx.resourceResolver().adaptTo(Session.class), replicationAction, replicationContentFactory, null);
            } catch(ReplicationException ex) {
                replicationExceptionThrown = true;
            }

            // Exception was thrown because only ACTIVATE is allowed for refetch.
            assertTrue(replicationExceptionThrown);

            reset(replicationContentFactory);
        }
    }

    @Test
    public void testDeactivationCreate() {
        ctx.registerInjectActivateService(refetchFlushContentBuilder);
        RefetchFlushContentBuilderImpl actual = (RefetchFlushContentBuilderImpl) ctx.getService(ContentBuilder.class);

        assertNotNull(actual);

        when(replicationAction.getType()).thenReturn(ReplicationActionType.DEACTIVATE);
        when(replicationAction.getLog()).thenReturn(replicationLog);

        for (String path: paths) {
            when(replicationAction.getPath()).thenReturn(path);

            boolean replicationExceptionThrown = false;
            try {
                actual.create(ctx.resourceResolver().adaptTo(Session.class), replicationAction, replicationContentFactory, null);
            } catch(ReplicationException ex) {
                replicationExceptionThrown = true;
            }

            // Exception was thrown because only ACTIVATE is allowed for refetch.
            assertTrue(replicationExceptionThrown);

            reset(replicationContentFactory);
        }
    }

    @Test
    public void testActivationPathNotMatchedCreate() throws Exception {
        ctx.registerInjectActivateService(refetchFlushContentBuilder,
        "match.paths", new String[]{"/content/dam/.*"});
        RefetchFlushContentBuilderImpl actual = (RefetchFlushContentBuilderImpl) ctx.getService(ContentBuilder.class);

        assertNotNull(actual);

        when(agentConfig.getSerializationType()).thenReturn("flush_refetch");
        when(replicationAction.getConfig()).thenReturn(agentConfig);
        when(replicationAction.getType()).thenReturn(ReplicationActionType.ACTIVATE);

        for (String path: paths) {
            when(replicationAction.getPath()).thenReturn(path);

            ReplicationContent content = actual.create(ctx.resourceResolver().adaptTo(Session.class), replicationAction, replicationContentFactory, null);
            assertEquals(ReplicationContent.VOID, content);

            reset(replicationContentFactory);
        }
    }

    @Test
    public void testActivationWrongRegexCreate() throws Exception {
        ctx.registerInjectActivateService(refetchFlushContentBuilder,
        "match.paths", new String[]{"*.k)"});
        RefetchFlushContentBuilderImpl actual = (RefetchFlushContentBuilderImpl) ctx.getService(ContentBuilder.class);

        assertNotNull(actual);

        when(agentConfig.getSerializationType()).thenReturn("flush_refetch");
        when(replicationAction.getConfig()).thenReturn(agentConfig);
        when(replicationAction.getType()).thenReturn(ReplicationActionType.ACTIVATE);

        for (String path: paths) {
            when(replicationAction.getPath()).thenReturn(path);

            ReplicationContent content = actual.create(ctx.resourceResolver().adaptTo(Session.class), replicationAction, replicationContentFactory, null);
            assertEquals(ReplicationContent.VOID, content);

            reset(replicationContentFactory);
        }
    }

    @Test
    public void testActivationNonPathCreate() throws Exception {
        ctx.registerInjectActivateService(refetchFlushContentBuilder);
        RefetchFlushContentBuilderImpl actual = (RefetchFlushContentBuilderImpl) ctx.getService(ContentBuilder.class);

        assertNotNull(actual);

        when(agentConfig.getSerializationType()).thenReturn(actual.getName());
        when(replicationAction.getConfig()).thenReturn(agentConfig);
        when(replicationAction.getType()).thenReturn(ReplicationActionType.ACTIVATE);
        when(replicationAction.getPath()).thenReturn("nonpath");

        ReplicationContent content = actual.create(ctx.resourceResolver().adaptTo(Session.class), replicationAction, replicationContentFactory, null);
        assertEquals(ReplicationContent.VOID, content);
    }

    @Test
    public void testActivationNoPathCreate() {
        ctx.registerInjectActivateService(refetchFlushContentBuilder);
        RefetchFlushContentBuilderImpl actual = (RefetchFlushContentBuilderImpl) ctx.getService(ContentBuilder.class);

        assertNotNull(actual);

        final String[] paths = new String[]{"", null};
        when(replicationAction.getType()).thenReturn(ReplicationActionType.ACTIVATE);
        when(replicationAction.getLog()).thenReturn(replicationLog);

        for (String path: paths) {
            when(replicationAction.getPath()).thenReturn(path);

            boolean replicationExceptionThrown = false;
            try {
                actual.create(ctx.resourceResolver().adaptTo(Session.class), replicationAction, replicationContentFactory, null);
            } catch(ReplicationException ex) {
                replicationExceptionThrown = true;
            }

            // Exception was thrown because only ACTIVATE is allowed for refetch.
            assertTrue(replicationExceptionThrown);

            reset(replicationContentFactory);
        }
    }

    @Test
    public void testActivateWithProperties() {
        String lhs = "html";
        String rhs = "header_include.html";
        ctx.registerInjectActivateService(refetchFlushContentBuilder,
            "extension.pairs", lhs + "=" + rhs,
            "match.paths", "/content/.*");
        RefetchFlushContentBuilderImpl actual = (RefetchFlushContentBuilderImpl) ctx.getService(ContentBuilder.class);

        assertNotNull(actual);

        Map<String, String[]> extensionPairs = actual.getExtensionPairs();
        String[] lhsResult = extensionPairs.get(lhs);
        assertNotNull(lhsResult);
        assertEquals(1, lhsResult.length);
        assertEquals(rhs.split("&")[0], lhsResult[0]);

        String[] pathMatchesResult = actual.getPathMatches();
        assertNotNull(pathMatchesResult);
        assertEquals(1, pathMatchesResult.length);
        assertEquals("/content/.*", pathMatchesResult[0]);
    }

    @Test
    public void testActivateEmptyProperties() {
        ctx.registerInjectActivateService(refetchFlushContentBuilder);
        RefetchFlushContentBuilderImpl actual = (RefetchFlushContentBuilderImpl) ctx.getService(ContentBuilder.class);

        assertNotNull(actual);

        Map<String, String[]> extensionPairs = actual.getExtensionPairs();
        assertNotNull(extensionPairs);
        assertEquals(0, extensionPairs.size());

        String[] pathMatchesResult = actual.getPathMatches();
        assertNotNull(pathMatchesResult);
        assertEquals(1, pathMatchesResult.length);
        assertEquals("*", pathMatchesResult[0]);
    }
}