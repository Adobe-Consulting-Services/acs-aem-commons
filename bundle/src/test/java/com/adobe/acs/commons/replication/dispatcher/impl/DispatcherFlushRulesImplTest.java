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

import com.adobe.acs.commons.replication.dispatcher.DispatcherFlushFilter;
import com.adobe.acs.commons.replication.dispatcher.DispatcherFlusher;
import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationOptions;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class DispatcherFlushRulesImplTest {
    @Mock
    private DispatcherFlusher dispatcherFlusher;

    @Spy
    private Map<Pattern, String[]> hierarchicalFlushRules = new LinkedHashMap<Pattern, String[]>();

    @Spy
    private Map<Pattern, String[]> resourceOnlyFlushRules = new LinkedHashMap<Pattern, String[]>();

    @Mock
    private ResourceResolverFactory resourceResolverFactory;

    @InjectMocks
    private DispatcherFlushRulesImpl dispatcherFlushRules = new DispatcherFlushRulesImpl();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(resourceResolverFactory.getAdministrativeResourceResolver(null)).thenReturn(mock(ResourceResolver.class));
    }

    @After
    public void tearDown() throws Exception {
        reset(dispatcherFlusher, resourceResolverFactory, hierarchicalFlushRules, resourceOnlyFlushRules);
    }

    @Test
    public void testConfigureReplicationActionType_Activate() throws Exception {
        final ReplicationActionType expected = ReplicationActionType.ACTIVATE;
        final ReplicationActionType actual = dispatcherFlushRules.configureReplicationActionType("ACTIVATE");

        assertEquals(expected, actual);
    }

    @Test
    public void testConfigureReplicationActionType_Delete() throws Exception {
        final ReplicationActionType expected = ReplicationActionType.DELETE;
        final ReplicationActionType actual = dispatcherFlushRules.configureReplicationActionType("DELETE");

        assertEquals(expected, actual);
    }

    @Test
    public void testConfigureReplicationActionType_Deactivate() throws Exception {
        final ReplicationActionType expected = ReplicationActionType.DEACTIVATE;
        final ReplicationActionType actual = dispatcherFlushRules.configureReplicationActionType("DEACTIVATE");

        assertEquals(expected, actual);
    }

    @Test
    public void testConfigureReplicationActionType_Inherit() throws Exception {
        final ReplicationActionType expected = null;
        final ReplicationActionType actual = dispatcherFlushRules.configureReplicationActionType("INHERIT");

        assertEquals(expected, actual);
    }

    @Test
    public void testConfigureFlushRules() throws Exception {
        final Map<String, String> validFlushRules = new LinkedHashMap<String, String>();
        validFlushRules.put("/a/.*", "/b");
        validFlushRules.put("/b/.*", "/c");
        validFlushRules.put("/c/d/.*", "/e/f");
        validFlushRules.put("/c/a/.*", "/e/f&/g/h");

        final Map<Pattern, String[]> expected = new LinkedHashMap<Pattern, String[]>();
        expected.put(Pattern.compile("/a/.*"), new String[] { "/b" });
        expected.put(Pattern.compile("/b/.*"), new String[] { "/c" });
        expected.put(Pattern.compile("/c/d/.*"), new String[] { "/e/f" });
        expected.put(Pattern.compile("/c/a/.*"), new String[] { "/e/f", "/g/h" });

        final Map<Pattern, String[]> actual = dispatcherFlushRules.configureFlushRules(validFlushRules);

        final Pattern[] expectedPatterns = expected.keySet().toArray(new Pattern[0]);
        final Pattern[] actualPatterns = actual.keySet().toArray(new Pattern[0]);
        final String[][] expectedValues = expected.values().toArray(new String[0][0]);
        final String[][] actualValues = actual.values().toArray(new String[0][0]);

        assertEquals(expected.size(), actual.size());

        for(int i = 0; i < expected.size(); i++) {
            assertEquals(expectedPatterns[i].pattern(), actualPatterns[i].pattern());
        }
        assertArrayEquals(expectedValues, actualValues);
    }

    @Test
    public void testPreprocess_notAccepts_ReplicationActionIsNull() throws Exception {
        when(this.hierarchicalFlushRules.size()).thenReturn(9);
        when(this.resourceOnlyFlushRules.size()).thenReturn(9);

        dispatcherFlushRules.preprocess(null, new ReplicationOptions());

        verifyNoInteractions(dispatcherFlusher);
    }


    @Test
    public void testPreprocess_notAccepts_ReplicationOptionsIsNull() throws Exception {
        when(hierarchicalFlushRules.size()).thenReturn(9);

        dispatcherFlushRules.preprocess(new ReplicationAction(ReplicationActionType.ACTIVATE, "/content/foo"), null);

        verifyNoInteractions(dispatcherFlusher);
    }

    @Test
    public void testPreprocess_notAccepts_ReplicationActionNoFlushRules() throws Exception {
        when(this.hierarchicalFlushRules.size()).thenReturn(0);
        when(this.resourceOnlyFlushRules.size()).thenReturn(0);

        final ReplicationAction replicationAction = mock(ReplicationAction.class);
        when(replicationAction.getPath()).thenReturn("/content/acs-aem-commons");

        dispatcherFlushRules.preprocess(replicationAction, new ReplicationOptions());

        verifyNoInteractions(dispatcherFlusher);
    }

    @Test
    public void testPreprocess_notAccepts_ReplicationActionPathEmpty() throws Exception {
        when(this.hierarchicalFlushRules.size()).thenReturn(9);
        when(this.resourceOnlyFlushRules.size()).thenReturn(9);

        final ReplicationAction replicationAction = mock(ReplicationAction.class);
        when(replicationAction.getPath()).thenReturn("");

        dispatcherFlushRules.preprocess(replicationAction, new ReplicationOptions());

        verifyNoInteractions(dispatcherFlusher);
    }

    @Test
    public void testPreprocess_notAccepts_ReplicationActionPathNull() throws Exception {
        when(this.hierarchicalFlushRules.size()).thenReturn(9);
        when(this.resourceOnlyFlushRules.size()).thenReturn(9);

        final ReplicationAction replicationAction = mock(ReplicationAction.class);
        when(replicationAction.getPath()).thenReturn(null);

        dispatcherFlushRules.preprocess(replicationAction, new ReplicationOptions());

        verifyNoInteractions(dispatcherFlusher);
    }

    @Test
    public void testPreprocess_notAccepts_ReplicationActionTypeInternalPoll() throws Exception {
        when(this.hierarchicalFlushRules.size()).thenReturn(9);
        when(this.resourceOnlyFlushRules.size()).thenReturn(9);

        final ReplicationAction replicationAction = mock(ReplicationAction.class);
        when(replicationAction.getPath()).thenReturn("/content/acs-aem-commons");
        when(replicationAction.getType()).thenReturn(ReplicationActionType.INTERNAL_POLL);

        dispatcherFlushRules.preprocess(replicationAction, new ReplicationOptions());

        verifyNoInteractions(dispatcherFlusher);
    }

    @Test
    public void testPreprocess_notAccepts_ReplicationActionTypeTest() throws Exception {
        when(this.hierarchicalFlushRules.size()).thenReturn(9);
        when(this.resourceOnlyFlushRules.size()).thenReturn(9);

        final ReplicationAction replicationAction = mock(ReplicationAction.class);
        when(replicationAction.getPath()).thenReturn("/content/acs-aem-commons");
        when(replicationAction.getType()).thenReturn(ReplicationActionType.TEST);

        dispatcherFlushRules.preprocess(replicationAction, new ReplicationOptions());

        verifyNoInteractions(dispatcherFlusher);
    }

    @Test
    public void testPreprocess_notAccepts_NonMatchingPath() throws Exception {
        this.hierarchicalFlushRules.put(Pattern.compile("/content/foo.*"), new String[] { "/content/target" });

        final ReplicationAction replicationAction = mock(ReplicationAction.class);
        when(replicationAction.getPath()).thenReturn("/content/acs-aem-commons");
        when(replicationAction.getType()).thenReturn(ReplicationActionType.ACTIVATE);

        final ReplicationOptions replicationOptions = new ReplicationOptions();
        replicationOptions.setSynchronous(false);

        dispatcherFlushRules.preprocess(replicationAction, replicationOptions);

        verifyNoInteractions(dispatcherFlusher);
    }


    @Test
    public void testPreprocess_success_hierarchical() throws Exception {
        hierarchicalFlushRules.put(Pattern.compile("/content/acs-aem-commons/.*"), new String[] { "/content/target", "/content/target2" });

        final ReplicationAction replicationAction = mock(ReplicationAction.class);
        when(replicationAction.getPath()).thenReturn("/content/acs-aem-commons/page");
        when(replicationAction.getType()).thenReturn(ReplicationActionType.ACTIVATE);

        final ReplicationOptions replicationOptions = new ReplicationOptions();
        replicationOptions.setSynchronous(false);

        final ArgumentCaptor<DispatcherFlushFilter> agentFilterCaptor = ArgumentCaptor.forClass(DispatcherFlushFilter
                .class);

        dispatcherFlushRules.preprocess(replicationAction, replicationOptions);

        verify(dispatcherFlusher, times(1)).flush(any(), eq(ReplicationActionType.ACTIVATE),
                eq(false),
                agentFilterCaptor.capture(),
                eq("/content/target"));

        assertEquals(DispatcherFlushFilter.FlushType.Hierarchical, agentFilterCaptor.getValue().getFlushType());
        // Private impl class; no access to test for instanceof
        assertEquals("DispatcherFlushRulesFilter", agentFilterCaptor.getValue().getClass().getSimpleName());

        verify(dispatcherFlusher, times(1)).flush(any(), eq(ReplicationActionType.ACTIVATE),
                eq(false),
                agentFilterCaptor.capture(),
                eq("/content/target2"));

        assertEquals(DispatcherFlushFilter.FlushType.Hierarchical, agentFilterCaptor.getValue().getFlushType());
        // Private impl class; no access to test for instanceof
        assertEquals("DispatcherFlushRulesFilter", agentFilterCaptor.getValue().getClass().getSimpleName());

        verifyNoMoreInteractions(dispatcherFlusher);
    }

    @Test
    public void testPreprocess_success_resourceOnly() throws Exception {
        resourceOnlyFlushRules.put(Pattern.compile("/content/acs-aem-commons/.*"), new String[] { "/content/target" });

        final ReplicationAction replicationAction = mock(ReplicationAction.class);
        when(replicationAction.getPath()).thenReturn("/content/acs-aem-commons/page");
        when(replicationAction.getType()).thenReturn(ReplicationActionType.ACTIVATE);

        final ReplicationOptions replicationOptions = new ReplicationOptions();
        replicationOptions.setSynchronous(false);

        final ArgumentCaptor<DispatcherFlushFilter> agentFilterCaptor = ArgumentCaptor.forClass(DispatcherFlushFilter
                .class);

        dispatcherFlushRules.preprocess(replicationAction, replicationOptions);

        verify(dispatcherFlusher, times(1)).flush(any(), eq(ReplicationActionType.ACTIVATE),
                eq(false),
                agentFilterCaptor.capture(),
                eq("/content/target"));

        assertEquals(DispatcherFlushFilter.FlushType.ResourceOnly, agentFilterCaptor.getValue().getFlushType());
        // Private impl class; no access to test for instanceof
        assertEquals("DispatcherFlushRulesFilter", agentFilterCaptor.getValue().getClass().getSimpleName());

        verifyNoMoreInteractions(dispatcherFlusher);
    }

    @Test
    public void testPreprocess_success_hierarchicalAndResourceOnly() throws Exception {
        hierarchicalFlushRules.put(Pattern.compile("/content/.*"), new String[] { "/content/hierarchical" });
        resourceOnlyFlushRules.put(Pattern.compile("/content/.*"), new String[] { "/content/resource-only" });

        final ReplicationAction replicationAction = mock(ReplicationAction.class);
        when(replicationAction.getPath()).thenReturn("/content/acs-aem-commons/page");
        when(replicationAction.getType()).thenReturn(ReplicationActionType.ACTIVATE);

        final ReplicationOptions replicationOptions = new ReplicationOptions();
        replicationOptions.setSynchronous(false);
        replicationOptions.setFilter(new DispatcherFlushFilter());

        final ArgumentCaptor<DispatcherFlushFilter> agentFilterCaptor = ArgumentCaptor.forClass(DispatcherFlushFilter
                .class);

        dispatcherFlushRules.preprocess(replicationAction, replicationOptions);

        verify(dispatcherFlusher, times(1)).flush(any(), eq(ReplicationActionType.ACTIVATE),
                eq(false),
                agentFilterCaptor.capture(),
                eq("/content/hierarchical"));

        assertEquals(DispatcherFlushFilter.FlushType.Hierarchical, agentFilterCaptor.getValue().getFlushType());
        // Private impl class; no access to test for instanceof
        assertEquals("DispatcherFlushRulesFilter", agentFilterCaptor.getValue().getClass().getSimpleName());

        verify(dispatcherFlusher, times(1)).flush(any(), eq(ReplicationActionType.ACTIVATE),
                eq(false),
                agentFilterCaptor.capture(),
                eq("/content/resource-only"));

        assertEquals(DispatcherFlushFilter.FlushType.ResourceOnly, agentFilterCaptor.getValue().getFlushType());
        // Private impl class; no access to test for instanceof
        assertEquals("DispatcherFlushRulesFilter", agentFilterCaptor.getValue().getClass().getSimpleName());

        verifyNoMoreInteractions(dispatcherFlusher);
    }

    @Test
    public void testPreprocess_success_translation1() throws Exception {
        hierarchicalFlushRules.put(Pattern.compile("/content/acs-aem-commons/(.*)"), new String[] { "/content/target/$1" });

        final ReplicationAction replicationAction = mock(ReplicationAction.class);
        when(replicationAction.getPath()).thenReturn("/content/acs-aem-commons/page");
        when(replicationAction.getType()).thenReturn(ReplicationActionType.ACTIVATE);

        final ReplicationOptions replicationOptions = new ReplicationOptions();
        replicationOptions.setSynchronous(false);

        final ArgumentCaptor<DispatcherFlushFilter> agentFilterCaptor = ArgumentCaptor.forClass(DispatcherFlushFilter
                .class);

        dispatcherFlushRules.preprocess(replicationAction, replicationOptions);

        verify(dispatcherFlusher, times(1)).flush(any(), eq(ReplicationActionType.ACTIVATE),
                eq(false),
                agentFilterCaptor.capture(),
                eq("/content/target/page"));

        assertEquals(DispatcherFlushFilter.FlushType.Hierarchical, agentFilterCaptor.getValue().getFlushType());
        // Private impl class; no access to test for instanceof
        assertEquals("DispatcherFlushRulesFilter", agentFilterCaptor.getValue().getClass().getSimpleName());

        verifyNoMoreInteractions(dispatcherFlusher);
    }

    @Test
    public void testPreprocess_success_translation2() throws Exception {
        hierarchicalFlushRules.put(Pattern.compile("/content/acs-aem-commons/(.*)/(.*)"), new String[] { "/content/target/$1/acs-aem-commons/$2" });

        final ReplicationAction replicationAction = mock(ReplicationAction.class);
        when(replicationAction.getPath()).thenReturn("/content/acs-aem-commons/en/page");
        when(replicationAction.getType()).thenReturn(ReplicationActionType.ACTIVATE);

        final ReplicationOptions replicationOptions = new ReplicationOptions();
        replicationOptions.setSynchronous(false);

        final ArgumentCaptor<DispatcherFlushFilter> agentFilterCaptor = ArgumentCaptor.forClass(DispatcherFlushFilter
                .class);

        dispatcherFlushRules.preprocess(replicationAction, replicationOptions);

        verify(dispatcherFlusher, times(1)).flush(any(), eq(ReplicationActionType.ACTIVATE),
                eq(false),
                agentFilterCaptor.capture(),
                eq("/content/target/en/acs-aem-commons/page"));

        assertEquals(DispatcherFlushFilter.FlushType.Hierarchical, agentFilterCaptor.getValue().getFlushType());
        // Private impl class; no access to test for instanceof
        assertEquals("DispatcherFlushRulesFilter", agentFilterCaptor.getValue().getClass().getSimpleName());

        verifyNoMoreInteractions(dispatcherFlusher);
    }


    @Test
    public void testPreprocess_success_resourceonly_translation2() throws Exception {
        resourceOnlyFlushRules.put(Pattern.compile("/content/acs-aem-commons/(.*)/(.*)"), new String[] { "/content/target/$1/acs-aem-commons/$2" });

        final ReplicationAction replicationAction = mock(ReplicationAction.class);
        when(replicationAction.getPath()).thenReturn("/content/acs-aem-commons/en/page");
        when(replicationAction.getType()).thenReturn(ReplicationActionType.ACTIVATE);

        final ReplicationOptions replicationOptions = new ReplicationOptions();
        replicationOptions.setSynchronous(false);

        final ArgumentCaptor<DispatcherFlushFilter> agentFilterCaptor = ArgumentCaptor.forClass(DispatcherFlushFilter
                .class);

        dispatcherFlushRules.preprocess(replicationAction, replicationOptions);

        verify(dispatcherFlusher, times(1)).flush(any(), eq(ReplicationActionType.ACTIVATE),
                eq(false),
                agentFilterCaptor.capture(),
                eq("/content/target/en/acs-aem-commons/page"));

        assertEquals(DispatcherFlushFilter.FlushType.ResourceOnly, agentFilterCaptor.getValue().getFlushType());
        // Private impl class; no access to test for instanceof
        assertEquals("DispatcherFlushRulesFilter", agentFilterCaptor.getValue().getClass().getSimpleName());

        verifyNoMoreInteractions(dispatcherFlusher);
    }
}
