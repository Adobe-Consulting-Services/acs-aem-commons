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

package com.adobe.acs.commons.replication.dispatcher.impl;

import com.adobe.acs.commons.replication.dispatcher.DispatcherFlusher;
import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationOptions;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DispatcherFlushRulesImplTest {
    @Mock
    private DispatcherFlusher dispatcherFlusher;

    @Spy
    private Map<Pattern, String> flushRules = new LinkedHashMap<Pattern, String>();

    @Mock
    private ResourceResolverFactory resourceResolverFactory;

    @InjectMocks
    private DispatcherFlushRulesImpl dispatcherFlushMap = new DispatcherFlushRulesImpl();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(resourceResolverFactory.getAdministrativeResourceResolver(null)).thenReturn(mock(ResourceResolver.class));
    }

    @After
    public void tearDown() throws Exception {
        reset(dispatcherFlusher);
        reset(flushRules);
    }

    @Test
    public void testConfigureReplicationActionType_ACTIVATE() throws Exception {
        final ReplicationActionType expected = ReplicationActionType.ACTIVATE;
        final ReplicationActionType actual = dispatcherFlushMap.configureReplicationActionType("ACTIVATE");

        assertEquals(expected, actual);
    }

    @Test
    public void testConfigureReplicationActionType_DELETE() throws Exception {
        final ReplicationActionType expected = ReplicationActionType.DELETE;
        final ReplicationActionType actual = dispatcherFlushMap.configureReplicationActionType("DELETE");

        assertEquals(expected, actual);
    }

    @Test
    public void testConfigureReplicationActionType_DEACTIVATE() throws Exception {
        final ReplicationActionType expected = ReplicationActionType.DEACTIVATE;
        final ReplicationActionType actual = dispatcherFlushMap.configureReplicationActionType("DEACTIVATE");

        assertEquals(expected, actual);
    }

    @Test
    public void testConfigureReplicationActionType_INHERIT() throws Exception {
        final ReplicationActionType expected = null;
        final ReplicationActionType actual = dispatcherFlushMap.configureReplicationActionType("INHERIT");

        assertEquals(expected, actual);
    }

    @Test
    public void testConfigureFlushRules() throws Exception {
        final Map<String, String> validFlushRules = new LinkedHashMap<String, String>();
        validFlushRules.put("/a/.*", "/b");
        validFlushRules.put("/b/.*", "/c");
        validFlushRules.put("/c/d/.*", "/e/f");

        final Map<Pattern, String> expected = new LinkedHashMap<Pattern, String>();
        expected.put(Pattern.compile("/a/.*"), "/b");
        expected.put(Pattern.compile("/b/.*"), "/c");
        expected.put(Pattern.compile("/c/d/.*"), "/e/f");

        final Map<Pattern, String> actual = dispatcherFlushMap.configureFlushRules(validFlushRules);

        assertEquals(expected.size(), actual.size());

        for(int i = 0; i < expected.size(); i++) {
            assertEquals(expected.keySet().toArray(new Pattern[]{})[i].pattern(),
                    actual.keySet().toArray(new Pattern[]{})[i].pattern());
            assertEquals(expected.values().toArray(new String[]{})[i], actual.values().toArray(new String[]{})[i]);
        }
    }

    @Test
    public void testConfigureFlushRules_Cyclic() throws Exception {
        final Map<String, String> cyclicFlushRules = new LinkedHashMap<String, String>();
        cyclicFlushRules.put("/a/.*", "/b/page");
        cyclicFlushRules.put("/b/.*", "/c/page");
        cyclicFlushRules.put("/c/.*", "/a/page");

        final Map<Pattern, String> actual = dispatcherFlushMap.configureFlushRules(cyclicFlushRules);

        assertEquals(0, actual.size());
    }

    @Test
    public void testPreprocess_notAccepts_ReplicationActionIsNull() throws Exception {
        when(flushRules.size()).thenReturn(9);

        dispatcherFlushMap.preprocess(null, new ReplicationOptions());
        verify(dispatcherFlusher, never()).flush(any(ResourceResolver.class), any(ReplicationActionType.class), anyBoolean(),
                anyString());
    }

    @Test
    public void testPreprocess_notAccepts_ReplicationActionNoFlushRules() throws Exception {
        when(flushRules.size()).thenReturn(0);

        final ReplicationAction replicationAction = mock(ReplicationAction.class);
        when(replicationAction.getPath()).thenReturn("/content/acs-aem-commons");

        dispatcherFlushMap.preprocess(replicationAction, new ReplicationOptions());
        verify(dispatcherFlusher, never()).flush(any(ResourceResolver.class), any(ReplicationActionType.class),
                anyBoolean(), anyString());
    }

    @Test
    public void testPreprocess_notAccepts_ReplicationActionPathEmpty() throws Exception {
        when(flushRules.size()).thenReturn(9);

        final ReplicationAction replicationAction = mock(ReplicationAction.class);
        when(replicationAction.getPath()).thenReturn("");

        dispatcherFlushMap.preprocess(replicationAction, new ReplicationOptions());
        verify(dispatcherFlusher, never()).flush(any(ResourceResolver.class), any(ReplicationActionType.class),
                anyBoolean(), anyString());
    }

    @Test
    public void testPreprocess_notAccepts_ReplicationActionPathNull() throws Exception {
        when(flushRules.size()).thenReturn(9);

        final ReplicationAction replicationAction = mock(ReplicationAction.class);
        when(replicationAction.getPath()).thenReturn(null);

        dispatcherFlushMap.preprocess(replicationAction, new ReplicationOptions());
        verify(dispatcherFlusher, never()).flush(any(ResourceResolver.class), any(ReplicationActionType.class),
                anyBoolean(), anyString());
    }

    @Test
    public void testPreprocess_notAccepts_ReplicationActionTypeInternalPoll() throws Exception {
        when(flushRules.size()).thenReturn(9);

        final ReplicationAction replicationAction = mock(ReplicationAction.class);
        when(replicationAction.getPath()).thenReturn("/content/acs-aem-commons");
        when(replicationAction.getType()).thenReturn(ReplicationActionType.INTERNAL_POLL);

        dispatcherFlushMap.preprocess(replicationAction, new ReplicationOptions());
        verify(dispatcherFlusher, never()).flush(any(ResourceResolver.class), any(ReplicationActionType.class),
                anyBoolean(), anyString());
    }

    @Test
    public void testPreprocess_notAccepts_ReplicationActionTypeTest() throws Exception {
        when(flushRules.size()).thenReturn(9);

        final ReplicationAction replicationAction = mock(ReplicationAction.class);
        when(replicationAction.getPath()).thenReturn("/content/acs-aem-commons");
        when(replicationAction.getType()).thenReturn(ReplicationActionType.TEST);

        dispatcherFlushMap.preprocess(replicationAction, new ReplicationOptions());
        verify(dispatcherFlusher, never()).flush(any(ResourceResolver.class), any(ReplicationActionType.class),
                anyBoolean(), anyString());
    }

    @Test
    public void testPreprocess_notAccepts_NonMatchingPath() throws Exception {
        flushRules.put(Pattern.compile("/content/foo.*"), "/content/target");

        final ReplicationAction replicationAction = mock(ReplicationAction.class);
        when(replicationAction.getPath()).thenReturn("/content/acs-aem-commons");
        when(replicationAction.getType()).thenReturn(ReplicationActionType.ACTIVATE);

        final ReplicationOptions replicationOptions = new ReplicationOptions();
        replicationOptions.setSynchronous(false);

        dispatcherFlushMap.preprocess(replicationAction, replicationOptions);

        verify(dispatcherFlusher, never()).flush(any(ResourceResolver.class), eq(ReplicationActionType.ACTIVATE),
                eq(false),
                eq("/content/acs-aem-commons"));
    }

    @Test
    public void testPreprocess_success() throws Exception {
        flushRules.put(Pattern.compile("/content/acs-aem-commons/.*"), "/content/target");

        final ReplicationAction replicationAction = mock(ReplicationAction.class);
        when(replicationAction.getPath()).thenReturn("/content/acs-aem-commons/page");
        when(replicationAction.getType()).thenReturn(ReplicationActionType.ACTIVATE);

        final ReplicationOptions replicationOptions = new ReplicationOptions();
        replicationOptions.setSynchronous(false);

        dispatcherFlushMap.preprocess(replicationAction, replicationOptions);

        verify(dispatcherFlusher, times(1)).flush(any(ResourceResolver.class), eq(ReplicationActionType.ACTIVATE),
                eq(false),
                eq("/content/target"));
    }
}
