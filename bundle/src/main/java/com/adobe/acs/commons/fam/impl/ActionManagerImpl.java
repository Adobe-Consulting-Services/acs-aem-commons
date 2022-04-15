/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularType;

import com.adobe.acs.commons.fam.ActionManagerConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.fam.ActionManager;
import com.adobe.acs.commons.fam.CancelHandler;
import com.adobe.acs.commons.fam.Failure;
import com.adobe.acs.commons.fam.ThrottledTaskRunner;
import com.adobe.acs.commons.fam.actions.Actions;
import com.adobe.acs.commons.functions.CheckedBiConsumer;
import com.adobe.acs.commons.functions.CheckedBiFunction;
import com.adobe.acs.commons.functions.CheckedConsumer;

/**
 * Manages a pool of reusable resource resolvers and injects them into tasks
 */
@SuppressWarnings("squid:S1192")
class ActionManagerImpl extends CancelHandler implements ActionManager, Serializable {

    private static final long serialVersionUID = 7526472295622776150L;

    private static final transient Logger LOG = LoggerFactory.getLogger(ActionManagerImpl.class);
    // This is a delay of how long an action manager should wait before it can safely assume it really is done and no more work is being added
    // This helps prevent an action manager from closing itself down while the queue is warming up.
    public static final transient int HESITATION_DELAY = 50;
    // The cleanup task will wait this many milliseconds between its polling to see if the queue has been completely processed
    public static final transient int COMPLETION_CHECK_INTERVAL = 100;
    private final AtomicInteger tasksAdded = new AtomicInteger();
    private final AtomicInteger tasksCompleted = new AtomicInteger();
    private final AtomicInteger tasksFilteredOut = new AtomicInteger();
    private final AtomicInteger tasksSuccessful = new AtomicInteger();
    private final AtomicInteger tasksError = new AtomicInteger();
    private final String name;
    private final AtomicLong started = new AtomicLong(0);
    private long finished;
    private int saveInterval;
    private int priority;

    private final transient ResourceResolver baseResolver;
    private final transient List<ReusableResolver> resolvers = Collections.synchronizedList(new ArrayList<>());
    private final transient ThreadLocal<ReusableResolver> currentResolver = new ThreadLocal<>();
    private final transient ThrottledTaskRunner taskRunner;
    private final transient ThreadLocal<String> currentPath;
    private final List<Failure> failures;
    private final transient AtomicBoolean cleanupHandlerRegistered = new AtomicBoolean(false);
    private final transient List<CheckedConsumer<ResourceResolver>> successHandlers = new CopyOnWriteArrayList<>();
    private final transient List<CheckedBiConsumer<List<Failure>, ResourceResolver>> errorHandlers = new CopyOnWriteArrayList<>();
    private final transient List<Runnable> finishHandlers = new CopyOnWriteArrayList<>();

    ActionManagerImpl(String name, ThrottledTaskRunner taskRunner, ResourceResolver resolver, int saveInterval) throws LoginException {
        this(name, taskRunner, resolver, saveInterval, ActionManagerConstants.DEFAULT_ACTION_PRIORITY);
    }

    ActionManagerImpl(String name, ThrottledTaskRunner taskRunner, ResourceResolver resolver, int saveInterval, int priority) throws LoginException {
        this.name = name;
        this.taskRunner = taskRunner;
        this.saveInterval = saveInterval;
        baseResolver = resolver.clone(null);
        currentPath = new ThreadLocal<>();
        failures = new ArrayList<>();
        this.priority =  priority;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getAddedCount() {
        return tasksAdded.get();
    }

    @Override
    public int getSuccessCount() {
        return tasksSuccessful.get();
    }

    @Override
    public int getErrorCount() {
        return Math.max(tasksError.get(), failures.size());
    }

    @Override
    public int getCompletedCount() {
        return tasksCompleted.get();
    }

    @Override
    public int getRemainingCount() {
        return getAddedCount() - (getSuccessCount() + tasksError.get());
    }

    @Override
    public List<Failure> getFailureList() {
        return Collections.unmodifiableList(failures);
    }

    @Override
    public void deferredWithResolver(final CheckedConsumer<ResourceResolver> action) {
        deferredWithResolver(action, false);
    }

    @SuppressWarnings("squid:S1181")
    private void deferredWithResolver(
            final CheckedConsumer<ResourceResolver> action,
            final boolean closesResolver) {
        if (!closesResolver) {
            tasksAdded.incrementAndGet();
        }
        taskRunner.scheduleWork(() -> {
            runActionAndLogErrors(action, closesResolver);
        }, this, priority);
    }
    
    @SuppressWarnings("squid:S1181")
    private void runActionAndLogErrors(CheckedConsumer<ResourceResolver> action, Boolean closesResolver) {
        started.compareAndSet(0, System.currentTimeMillis());
        try {
            withResolver(action);
            if (!closesResolver) {
                logCompletetion();
            }
        } catch (Error e) {
            // These are very fatal errors but we should log them if we can
            LOG.error("Fatal uncaught error in action {}", getName(), e);
            if (!closesResolver) {
                logError(new RuntimeException(e));
            }
            throw e;
        } catch (Exception t) {
            // Less fatal errors, but still need to explicitly catch them
            LOG.error("Error in action {}", getName(), t);
            if (!closesResolver) {
                logError(t);
            }
        } catch (Throwable t) {
            // There are some slippery runtime errors (unchecked) which slip through the cracks
            LOG.error("Fatal uncaught error in action {}", getName(), t);
            if (!closesResolver) {
                logError(new RuntimeException(t));
            }
        }
    }

    @Override
    @SuppressWarnings({"squid:S1181", "squid:S1163", "squid:S1143"})
    public void withResolver(CheckedConsumer<ResourceResolver> action) throws Exception {
        Actions.setCurrentActionManager(this);
        ReusableResolver resolver = getResourceResolver();
        resolver.setCurrentItem(currentPath.get());
        try {
            action.accept(resolver.getResolver());
        } catch (Throwable ex) {
            throw ex;
        } finally {
            try {
                resolver.free();
            } catch (PersistenceException ex) {
                logPersistenceException(resolver.getPendingItems(), ex);
                throw ex;
            }
            Actions.setCurrentActionManager(null);
        }
    }

    @Override
    @SuppressWarnings("squid:S3776")
    public int withQueryResults(
            final String queryStatement,
            final String language,
            final CheckedBiConsumer<ResourceResolver, String> callback,
            final CheckedBiFunction<ResourceResolver, String, Boolean>... filters
    )
            throws RepositoryException, PersistenceException, Exception {
        withResolver((ResourceResolver resolver) -> {
            try {
                Session session = resolver.adaptTo(Session.class);
                QueryManager queryManager = session.getWorkspace().getQueryManager();
                Query query = queryManager.createQuery(queryStatement, language);
                QueryResult results = query.execute();
                for (NodeIterator nodeIterator = results.getNodes(); nodeIterator.hasNext();) {
                    final String nodePath = nodeIterator.nextNode().getPath();
                    LOG.info("Processing found result {}", nodePath);
                    deferredWithResolver((ResourceResolver r) -> {
                        currentPath.set(nodePath);
                        if (filters != null) {
                            for (CheckedBiFunction<ResourceResolver, String, Boolean> filter : filters) {
                                if (!filter.apply(r, nodePath)) {
                                    logFilteredOutItem(nodePath);
                                    return;
                                }
                            }
                        }
                        callback.accept(r, nodePath);
                    });
                }
            } catch (RepositoryException ex) {
                LOG.error("Repository exception processing query '{}'", queryStatement, ex);
            }
        });

        return tasksAdded.get();
    }

    @Override
    public void cancel(boolean useForce) {
        super.cancel(useForce);
        if (getErrorCount() > 0) {
            processErrorHandlers();
        }
    }

    @Override
    public void onSuccess(CheckedConsumer<ResourceResolver> successTask) {
        successHandlers.add(successTask);
    }

    @Override
    public void onFailure(CheckedBiConsumer<List<Failure>, ResourceResolver> failureTask) {
        errorHandlers.add(failureTask);
    }

    @Override
    public void onFinish(Runnable finishHandler) {
        finishHandlers.add(finishHandler);
    }

    private void runCompletionTasks() {
        if (getErrorCount() == 0) {
            synchronized (successHandlers) {
                successHandlers.forEach(handler -> {
                    try {
                        this.withResolver(handler);
                    } catch (Exception ex) {
                        LOG.error("Error in success handler for action {}", getName(), ex);
                    }
                });
            }
        } else {
            processErrorHandlers();
        }
        synchronized (finishHandlers) {
            finishHandlers.forEach(Runnable::run);
        }
    }

    private void processErrorHandlers() {
        List<CheckedBiConsumer<List<Failure>, ResourceResolver>> handlerList = new ArrayList();
        synchronized (errorHandlers) {
            handlerList.addAll(errorHandlers);
            errorHandlers.clear();
        }
        handlerList.forEach(handler -> {
            try {
                this.withResolver(res -> handler.accept(this.failures, res));
            } catch (Exception ex) {
                LOG.error("Error in error handler for action {}", getName(), ex);
            }
        });
    }

    @SuppressWarnings("squid:S2142")
    private void performAutomaticCleanup() {
        if (!cleanupHandlerRegistered.getAndSet(true)) {
            taskRunner.scheduleWork(() -> {
                while (!isComplete()) {
                    try {
                        Thread.sleep(COMPLETION_CHECK_INTERVAL);
                    } catch (InterruptedException ex) {
                        logError(ex);
                    }
                }
                runCompletionTasks();
                savePendingChanges();
                closeAllResolvers();
            }, priority);
        }
    }
    
    private void savePendingChanges() {
      for (ReusableResolver resolver : resolvers) {
        try {
          resolver.commit();
        } catch (PersistenceException e) {
          logPersistenceException(resolver.getPendingItems(), e);
        }
      }
    }

    @Override
    public void setCurrentItem(String item) {
        currentPath.set(item);
    }

    private ReusableResolver getResourceResolver() throws LoginException {
        ReusableResolver resolver = currentResolver.get();
        if (resolver == null || !resolver.getResolver().isLive()) {
            resolver = new ReusableResolver(baseResolver.clone(null), saveInterval);
            currentResolver.set(resolver);
            resolvers.add(resolver);
        }
        return resolver;
    }

    private void logCompletetion() {
        tasksCompleted.incrementAndGet();
        tasksSuccessful.incrementAndGet();
        if (isComplete()) {
            finished = System.currentTimeMillis();
            performAutomaticCleanup();
        }
    }

    private void logError(Exception ex) {
        LOG.error("Caught exception in task: {}", ex.getMessage(), ex);
        Failure fail = new Failure();
        fail.setNodePath(currentPath.get());
        fail.setException(ex);
        failures.add(fail);
        tasksCompleted.incrementAndGet();
        tasksError.incrementAndGet();
        if (isComplete()) {
            finished = System.currentTimeMillis();
            performAutomaticCleanup();
        }
    }

    private void logPersistenceException(List<String> items, PersistenceException ex) {
        StringBuilder itemList = new StringBuilder();
        for (String item : items) {
            itemList.append(item).append("; ");
            Failure fail = new Failure();
            fail.setNodePath(item);
            fail.setException(ex);
            failures.add(fail);
            tasksError.incrementAndGet();
            tasksSuccessful.decrementAndGet();
        }
        LOG.error("Persistence error prevented saving changes for: {}" ,itemList, ex);
    }

    private void logFilteredOutItem(String path) {
        tasksFilteredOut.incrementAndGet();
        LOG.info("Filtered out {}", path);
    }

    public long getRuntime() {
        if (isComplete()) {
            return finished - started.get();
        } else if (tasksAdded.get() == 0) {
            return 0;
        } else {
            return System.currentTimeMillis() - started.get();
        }
    }

    public static TabularType getStaticsTableType() {
        return statsTabularType;
    }

    @Override
    @SuppressWarnings("squid:S2142")
    public boolean isComplete() {
        if (tasksCompleted.get() == tasksAdded.get()) {
            try {
                Thread.sleep(HESITATION_DELAY);
            } catch (InterruptedException ex) {
                // no-op
            }
            return tasksCompleted.get() == tasksAdded.get();
        } else {
            return false;
        }
    }

    @Override
    public CompositeData getStatistics() throws OpenDataException {
        return new CompositeDataSupport(statsCompositeType, statsItemNames,
                new Object[]{
                    name,
                    priority,
                    tasksAdded.get(),
                    tasksCompleted.get(),
                    tasksFilteredOut.get(),
                    tasksSuccessful.get(),
                    tasksError.get(),
                    getRuntime()
                }
        );
    }

    @Override
    public synchronized void closeAllResolvers() {
        if (!resolvers.isEmpty()) {
            resolvers.stream()
                    .map(ReusableResolver::getResolver)
                    .filter(ResourceResolver::isLive)
                    .forEach(ResourceResolver::close);
            resolvers.clear();
        }
        baseResolver.close();
    }

    public static TabularType getFailuresTableType() {
        return failureTabularType;
    }

    @Override
    public List<CompositeData> getFailures() throws OpenDataException {
        ArrayList<CompositeData> failureData = new ArrayList<>();
        int count = 0;
        for (Failure fail : failures) {
            if (count > 5000) {
                break;
            }
            failureData.add(new CompositeDataSupport(
                    failureCompositeType,
                    failureItemNames,
                    new Object[]{name, ++count, fail.getNodePath(), fail.getException() == null ? "Unknown" : fail.getException().getMessage()}));
        }
        return failureData;
    }

    private static transient String[] statsItemNames;
    private static transient CompositeType statsCompositeType;
    private static transient TabularType statsTabularType;
    private static transient String[] failureItemNames;
    private static transient CompositeType failureCompositeType;
    private static transient TabularType failureTabularType;

    static {
        try {
            statsItemNames =
                    new String[] { "_taskName", "priority", "started", "completed", "filtered", "successful",
                            "errors", "runtime" };
            statsCompositeType = new CompositeType(
                    "Statics Row",
                    "Single row of statistics",
                    statsItemNames,
                            new String[] { "Name", "Priority", "Started", "Completed", "Filtered", "Successful",
                                    "Errors", "Runtime" }, new OpenType[] { SimpleType.STRING, SimpleType.INTEGER,
                                    SimpleType.INTEGER, SimpleType.INTEGER, SimpleType.INTEGER, SimpleType.INTEGER,
                                    SimpleType.INTEGER, SimpleType.LONG });
            statsTabularType = new TabularType("Statistics", "Collected statistics", statsCompositeType, new String[]{"_taskName"});

            failureItemNames = new String[]{"_taskName", "_count", "item", "error"};
            failureCompositeType = new CompositeType(
                    "Failure",
                    "Failure",
                    failureItemNames,
                    new String[]{"Name", "#", "Item", "Error"},
                    new OpenType[]{SimpleType.STRING, SimpleType.INTEGER, SimpleType.STRING, SimpleType.STRING});
            failureTabularType = new TabularType("Errors", "Collected failures", failureCompositeType, new String[]{"_taskName", "_count"});
        } catch (OpenDataException ex) {
            LOG.error("Unable to build MBean composite types", ex);
        }
    }
}
