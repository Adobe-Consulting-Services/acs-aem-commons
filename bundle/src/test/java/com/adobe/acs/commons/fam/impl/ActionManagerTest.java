/*
 * Copyright 2017 Adobe.
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
package com.adobe.acs.commons.fam.impl;

import com.adobe.acs.commons.fam.ThrottledTaskRunner;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import static org.junit.Assert.*;
import org.junit.Test;
import static org.mockito.Mockito.*;

/**
 * Basic tests covering simple Action Manager operations
 */
public class ActionManagerTest {
    private ThrottledTaskRunner getTaskRunner() {
        ThrottledTaskRunner taskRunner = mock(ThrottledTaskRunner.class);
        doAnswer(i -> {
            ((Runnable) i.getArguments()[0]).run();
            return null;
        }).when(taskRunner).scheduleWork(any());
        doAnswer(i -> {
            ((Runnable) i.getArguments()[0]).run();
            return null;
        }).when(taskRunner).scheduleWork(any(), any());
        return taskRunner;
    }

    @Test
    public void statsCounterTest() throws LoginException, Exception {
        ResourceResolver rr = mock(ResourceResolver.class);
        when(rr.clone(any())).thenReturn(rr);
        ActionManagerImpl manager = new ActionManagerImpl("test", getTaskRunner(), rr, 1);
        assertEquals(0, manager.getAddedCount());
        manager.withResolver(resolver->{});
        assertEquals(1, manager.getAddedCount());
        assertEquals(1, manager.getCompletedCount());
        manager.withResolver(resolver->{});
        assertEquals(2, manager.getAddedCount());
        assertEquals(2, manager.getCompletedCount());
        assertEquals(0, manager.getErrorCount());
        assertEquals(0, manager.getRemainingCount());
        assertTrue(manager.isComplete());
    }
}
