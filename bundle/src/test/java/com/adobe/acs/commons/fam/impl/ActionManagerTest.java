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
import com.adobe.acs.commons.mcp.form.AbstractResourceImpl;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Basic tests covering simple Action Manager operations
 */
public class ActionManagerTest {

    private static final Logger LOG = LoggerFactory.getLogger(ActionManagerTest.class);

    public static void run(Runnable r) {
        try {
            Thread t = new Thread(r);
            t.start();
            t.join();
        } catch (InterruptedException ex) {
            LOG.error("interrupted exception", ex);
        }
    }

    public static ThrottledTaskRunner getTaskRunner() {
        ThrottledTaskRunner taskRunner = mock(ThrottledTaskRunner.class);
//        doAnswer(i -> {
//            run((Runnable) i.getArguments()[0]);
//            return null;
//        }).when(taskRunner).scheduleWork(any(Runnable.class));
//        doAnswer(i -> {
//            run((Runnable) i.getArguments()[0]);
//            return null;
//        }).when(taskRunner).scheduleWork(any(Runnable.class),any(CancelHandler.class));
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

    public static ResourceResolver getFreshMockResolver() throws LoginException, PersistenceException {
        mockResolver = null;
        return getMockResolver();
    }

    public static ResourceResolver getMockResolver() throws LoginException, PersistenceException {
        if (mockResolver == null) {
            mockResolver = mock(ResourceResolver.class);
            when(mockResolver.clone(any())).thenReturn(mockResolver);
            when(mockResolver.isLive()).thenReturn(true);
            when(mockResolver.hasChanges()).thenReturn(true);
            when(mockResolver.create(any(), any(), any())).then((InvocationOnMock invocation) -> {
                Resource parent = invocation.getArgument(0);
                String name = invocation.getArgument(1);
                Map<String,Object> properties = invocation.getArgument(2);
                ResourceMetadata metadata = new ResourceMetadata();
                metadata.putAll(properties);

                String path = parent.getPath() + "/" + name;
                Resource res = new AbstractResourceImpl(path, null, null, metadata);
                when(mockResolver.getResource(path)).thenReturn(res);
                return res;
            });
        }
        return mockResolver;
    }

    public static ActionManager getActionManager() throws LoginException, PersistenceException {
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
    
    @Test
    public void pendingCommitsAreFlushedTest() throws Exception {
      final int saveInterval = 10;
      final int taskCount = 17;   // Make sure taskCount is _not_ a multiple of saveInterval, otherwise the issue may be masked.
      final int expectedCommitCount = 2;  // TaskCount divided by saveInterval and rounded _up_.
      
      final ResourceResolver rr = getFreshMockResolver();

      // We need a task runner that uses only one thread, so we won't get a bunch of thread-local resolver clones.
      Queue<Runnable> taskQueue = new LinkedList<>();
      ThrottledTaskRunner runner = mock(ThrottledTaskRunner.class);
      Answer<Void> answer = i -> {
        Runnable r = i.getArgument(0);
        taskQueue.add(r);
        return null;
      };
      doAnswer(answer).when(runner).scheduleWork(any(Runnable.class));
      doAnswer(answer).when(runner).scheduleWork(any(Runnable.class),any(CancelHandler.class));
      doAnswer(answer).when(runner).scheduleWork(any(Runnable.class), any(CancelHandler.class), anyInt());
      doAnswer(answer).when(runner).scheduleWork(any(Runnable.class), anyInt());

      ActionManager manager = new ActionManagerImpl("test", runner, rr, saveInterval);
      for (int i = 0; i < taskCount; i++) {
        manager.deferredWithResolver(resolver -> {});
      }
      // Simulate execution of the tasks in the background. The tasks may add new tasks at the end of the queue.
      while (!taskQueue.isEmpty()) {
        Runnable r = taskQueue.remove();
        r.run();
      }
      
      InOrder inOrder = inOrder(rr);
      inOrder.verify(rr, times(expectedCommitCount)).commit();
      inOrder.verify(rr, times(2)).close();   // We expect one call for the one background resolver opened, and one for the base resolver.
      inOrder.verifyNoMoreInteractions();
    }
}
