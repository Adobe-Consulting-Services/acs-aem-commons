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
package com.adobe.acs.commons.fam.impl;

import com.adobe.acs.commons.fam.ActionManager;
import com.adobe.acs.commons.fam.CancelHandler;
import com.adobe.acs.commons.fam.ThrottledTaskRunner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Basic tests covering simple Action Manager operations
 */
public class ActionManagerTest {

    public static void run(Runnable r) {
        try {
            Thread t = new Thread(r);
            t.start();
            t.join();
        } catch (InterruptedException ex) {
            Logger.getLogger(ActionManagerTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static ThrottledTaskRunner getTaskRunner() {
        ThrottledTaskRunner taskRunner = mock(ThrottledTaskRunner.class);
        doAnswer(i -> {
            run((Runnable) i.getArguments()[0]);
            return null;
        }).when(taskRunner).scheduleWork(any(Runnable.class));
        doAnswer(i -> {
            run((Runnable) i.getArguments()[0]);
            return null;
        }).when(taskRunner).scheduleWork(any(Runnable.class),any(CancelHandler.class));
        doAnswer(i -> {
            run((Runnable) i.getArguments()[0]);
            return null;
        }).when(taskRunner).scheduleWork(any(Runnable.class), any(CancelHandler.class), anyInt());
        doAnswer(i -> {
            run((Runnable) i.getArguments()[0]);
            return null;
        }).when(taskRunner).scheduleWork(any(Runnable.class), anyInt());

        return taskRunner;
    }

    static ResourceResolver mockResolver;

    public static ResourceResolver getFreshMockResolver() throws LoginException {
        mockResolver = null;
        return getMockResolver();
    }
    
    public static ResourceResolver getMockResolver() throws LoginException {
        if (mockResolver == null) {
            mockResolver = mock(ResourceResolver.class);
            when(mockResolver.clone(any())).thenReturn(mockResolver);
            when(mockResolver.isLive()).thenReturn(true);
        }
        return mockResolver;
    }
    
    public static ActionManager getActionManager() throws LoginException {
        ResourceResolver rr = getMockResolver();
        return new ActionManagerImpl("test", getTaskRunner(), rr, 1);        
    }
    
    @Test
    public void nullStatsCounterTest() throws LoginException, Exception {
        // Counters don't do tabulate in-thread actions, only deferred actions
        final ResourceResolver rr = getMockResolver();
        ActionManager manager = getActionManager();
        assertEquals(0, manager.getAddedCount());
        manager.withResolver(resolver -> {});
        assertEquals(0, manager.getAddedCount());
        assertEquals(0, manager.getCompletedCount());
        manager.withResolver(resolver -> {});
        assertEquals(0, manager.getAddedCount());
        assertEquals(0, manager.getCompletedCount());
        assertEquals(0, manager.getErrorCount());
        assertEquals(0, manager.getRemainingCount());
        assertTrue(manager.isComplete());
    }

    @Test
    public void deferredStatsCounterTest() throws LoginException, Exception {
        final ResourceResolver rr = getMockResolver();
        ActionManager manager = getActionManager();
        assertEquals(0, manager.getAddedCount());
        manager.deferredWithResolver(resolver -> {
        });
        assertEquals(1, manager.getAddedCount());
        assertEquals(1, manager.getCompletedCount());
        manager.deferredWithResolver(resolver -> {
        });
        assertEquals(2, manager.getAddedCount());
        assertEquals(2, manager.getSuccessCount());
        assertEquals(2, manager.getCompletedCount());
        assertEquals(0, manager.getErrorCount());
        assertEquals(0, manager.getRemainingCount());
        assertTrue(manager.isComplete());
    }
    
    @Test
    public void deferredStatsCounterErrorTest() throws LoginException, Exception {
        final ResourceResolver rr = getMockResolver();
        ActionManager manager = getActionManager();
        assertEquals(0, manager.getAddedCount());
        manager.deferredWithResolver(resolver -> {
            throw new Exception("Bad things");
        });
        assertEquals(1, manager.getAddedCount());
        assertEquals(1, manager.getCompletedCount());
        manager.deferredWithResolver(resolver -> {
            throw new Exception("Bad things");
        });
        assertEquals(2, manager.getAddedCount());
        assertEquals(2, manager.getCompletedCount());
        assertEquals(2, manager.getErrorCount());
        manager.deferredWithResolver(resolver -> {
            throw new NullPointerException("Bad things");
        });
        assertEquals(3, manager.getAddedCount());
        assertEquals(3, manager.getCompletedCount());
        assertEquals(3, manager.getErrorCount());
        
        manager.deferredWithResolver(resolver -> {
            throw new PersistenceException("Bad things");
        });
        assertEquals(4, manager.getAddedCount());
        assertEquals(4, manager.getCompletedCount());
        assertEquals(4, manager.getErrorCount());
        assertNotNull(manager.getFailureList());
        assertEquals(4, manager.getFailures().size());
        assertEquals(0, manager.getSuccessCount());
        assertEquals(0, manager.getRemainingCount());
        assertTrue(manager.isComplete());
    }    

    @Test
    public void closeAllResolversTest() throws LoginException, Exception {
        final ResourceResolver rr = getMockResolver();
        ActionManager manager = getActionManager();
        manager.deferredWithResolver(resolver -> {
        });
        manager.deferredWithResolver(resolver -> {
        });
        manager.deferredWithResolver(resolver -> {
        });
        manager.deferredWithResolver(resolver -> {
        });
        manager.closeAllResolvers();
        verify(rr, atLeast(5)).close();
    }
}
