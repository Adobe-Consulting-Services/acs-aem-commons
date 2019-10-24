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
package com.adobe.acs.commons.mcp.impl;

import com.adobe.acs.commons.fam.ActionManager;
import com.adobe.acs.commons.fam.ActionManagerFactory;
import com.adobe.acs.commons.fam.Failure;
import com.adobe.acs.commons.fam.actions.ActionBatch;
import com.adobe.acs.commons.functions.CheckedConsumer;
import com.adobe.acs.commons.mcp.ControlledProcessManager;
import com.adobe.acs.commons.mcp.ProcessDefinition;
import com.adobe.acs.commons.mcp.ProcessInstance;
import com.adobe.acs.commons.mcp.model.ArchivedProcessFailure;
import com.adobe.acs.commons.mcp.model.ManagedProcess;
import com.adobe.acs.commons.mcp.model.Result;
import com.adobe.acs.commons.mcp.util.DeserializeException;
import com.adobe.acs.commons.mcp.util.ValueMapSerializer;
import com.day.cq.commons.jcr.JcrUtil;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ModifiableValueMapDecorator;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularType;
import java.io.Serializable;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Abstraction of a Process which runs using FAM and consists of one or more actions.
 */
public class ProcessInstanceImpl implements ProcessInstance, Serializable {

    private static final long serialVersionUID = 7526472295622776151L;

    private final ManagedProcess infoBean;
    private final String id;
    private final String path;
    private static final transient org.slf4j.Logger LOG = LoggerFactory.getLogger(ProcessInstanceImpl.class);
    private final transient List<ActivityDefinition> actions;
    public static final transient String BASE_PATH = "/var/acs-commons/mcp/instances";
    private transient ControlledProcessManager manager = null;
    private final transient ProcessDefinition definition;
    private transient boolean completedNormally = false;
    private static final transient Random RANDOM = new SecureRandom();

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public double updateProgress() {
        double sectionWeight = 1.0 / actions.size();
        double progress = actions.stream()
                .map(action -> action.manager)
                .filter(action -> action.getAddedCount() > 0)
                .collect(Collectors.summingDouble(action
                        -> sectionWeight * (double) action.getCompletedCount() / (double) action.getAddedCount()
                ));
        infoBean.setProgress(progress);
        infoBean.setStatus(actions.stream().filter(a -> !a.manager.isComplete()).map(a -> a.name).findFirst().orElse("Please wait..."));
        int countCompleted = actions.stream().collect(Collectors.summingInt(action -> action.manager.getCompletedCount()));
        infoBean.getResult().setTasksCompleted(
                Math.max(infoBean.getResult().getTasksCompleted(), countCompleted)
        );
        infoBean.setReportedErrors(actions.stream().flatMap(a -> a.manager.getFailureList().stream()).map(ArchivedProcessFailure::adapt).collect(Collectors.toList()));

        return progress;
    }

    private static class ActivityDefinition {

        String name;
        ActionManager manager;
        transient CheckedConsumer<ActionManager> builder;
        boolean critical = false;
    }

    public ProcessInstanceImpl(ControlledProcessManager cpm, ProcessDefinition process, String description) {
        manager = cpm;
        infoBean = new ManagedProcess();
        infoBean.setStartTime(-1L);
        infoBean.setStopTime(-1L);
        this.actions = new ArrayList<>();
        this.definition = process;
        infoBean.setName(process.getName());
        infoBean.setDescription(description == null ? "No description" : description);
        infoBean.setResult(new Result());
        id = String.format("%016X", Math.abs(RANDOM.nextLong()));
        path = BASE_PATH + "/" + id;
    }

    @Override
    public String getName() {
        if (definition.getName() != null) {
            if (infoBean.getDescription() != null) {
                return definition.getName() + ": " + infoBean.getDescription();
            } else {
                return definition.getName();
            }
        } else {
            if (infoBean.getDescription() != null) {
                return infoBean.getDescription();
            } else {
                return "No idea";
            }
        }
    }

    @Override
    public void init(ResourceResolver resourceResolver, Map<String, Object> parameterMap) throws DeserializeException, RepositoryException {
        try {
            ValueMap inputs = new ModifiableValueMapDecorator(parameterMap);
            infoBean.setRequestInputs(inputs);
            definition.parseInputs(inputs);
        } catch (DeserializeException | RepositoryException ex) {
            LOG.error("Error starting managed process " + getName(), ex);
            Failure f = new Failure();
            f.setException(ex);
            f.setNodePath(getPath());
            recordErrors(-1, Arrays.asList(f), resourceResolver);
            halt();
            throw ex;
        }
    }

    @Override
    public ActionManagerFactory getActionManagerFactory() {
        return manager.getActionManagerFactory();
    }

    @Override
    public final ActionManager defineCriticalAction(String name, ResourceResolver rr, CheckedConsumer<ActionManager> builder) throws LoginException {
        return defineAction(name, rr, builder, true);
    }

    @Override
    public final ActionManager defineAction(String name, ResourceResolver rr, CheckedConsumer<ActionManager> builder) throws LoginException {
        return defineAction(name, rr, builder, false);
    }

    private ActionManager defineAction(String name, ResourceResolver rr, CheckedConsumer<ActionManager> builder, boolean isCritical) throws LoginException {
        ActivityDefinition activityDefinition = new ActivityDefinition();
        activityDefinition.builder = builder;
        activityDefinition.name = name;
        activityDefinition.manager = getActionManagerFactory().createTaskManager(getName() + ": " + name, rr, 1);
        activityDefinition.critical = isCritical;
        actions.add(activityDefinition);
        return activityDefinition.manager;
    }

    @Override
    public final void run(ResourceResolver rr) {
        try {
            infoBean.setRequester(rr.getUserID());
            infoBean.setStartTime(System.currentTimeMillis());
            definition.buildProcess(this, rr);
            infoBean.setIsRunning(true);
            runStep(0);
        } catch (LoginException | RepositoryException | RuntimeException ex) {
            LOG.error("Error starting managed process " + getName(), ex);
            Failure f = new Failure();
            f.setException(ex);
            f.setNodePath(getPath());
            asServiceUser(serviceResolver -> {
                persistStatus(serviceResolver);
                recordErrors(-1, Arrays.asList(f), serviceResolver);
            });
            halt();
        }
    }

    private void runStep(int step) {
        if (step >= actions.size()) {
            completedNormally = true;
            halt();
        } else {
            updateProgress();
            updateStatus(step);
            asServiceUser(this::persistStatus);
            ActivityDefinition action = actions.get(step);
            if (action.critical) {
                action.manager.onSuccess(rr -> runStep(step + 1));
                action.manager.onFailure((failures, rr) -> {
                    asServiceUser(service -> recordErrors(step, failures, service));
                    halt();
                });
            } else {
                action.manager.onFailure((failures, rr) -> {
                    asServiceUser(service -> recordErrors(step, failures, service));
                });
                action.manager.onFinish(() -> runStep(step + 1));
            }
            action.manager.deferredWithResolver(rr -> action.builder.accept(action.manager));
        }
    }

    public void recordErrors(int step, List<Failure> failures, ResourceResolver rr) {
        if (failures.isEmpty()) {
            return;
        }
        List<ArchivedProcessFailure> archivedFailures = failures.stream().map(ArchivedProcessFailure::adapt).collect(Collectors.toList());
        infoBean.setReportedErrors(archivedFailures);
        try {
            String errFolder = getPath() + "/jcr:content/failures/step" + (step + 1);
            JcrUtil.createPath(errFolder, "nt:unstructured", rr.adaptTo(Session.class));
            if (rr.hasChanges()) {
                rr.commit();
            }
            rr.refresh();
            ActionManager errorManager = getActionManagerFactory().createTaskManager("Record errors", rr, 1);
            ActionBatch batch = new ActionBatch(errorManager, 50);
            for (int i = 0; i < failures.size(); i++) {
                String errPath = errFolder + "/err" + i;
                Failure failure = failures.get(i);
                batch.add(rr2 -> {
                    Map<String, Object> values = new HashMap<>();
                    ValueMapSerializer.serializeToMap(values, failure);
                    ResourceUtil.getOrCreateResource(rr2, errPath, values, null, false);
                });
            }
            batch.commitBatch();
        } catch (RepositoryException | PersistenceException | LoginException | NullPointerException ex) {
            LOG.error("Unable to record errors", ex);
        }
    }

    private long getRuntime() {
        long stop = System.currentTimeMillis();
        if (infoBean.getStopTime() > infoBean.getStartTime()) {
            stop = infoBean.getStopTime();
        }
        return stop - infoBean.getStartTime();
    }

    private void updateStatus(int step) {
        infoBean.setStatus("Step " + (step + 1) + ": " + actions.get(step).name);
    }

    private void setStatusCompleted() {
        infoBean.setStatus("Completed");
        infoBean.setProgress(1.0D);
    }

    private void setStatusAborted() {
        infoBean.setStatus("Aborted");
    }

    public void asServiceUser(CheckedConsumer<ResourceResolver> action) {
        try (ResourceResolver rr = manager.getServiceResourceResolver()){
            action.accept(rr);
            if (rr.hasChanges()) {
                rr.commit();
            }
        } catch (Exception ex) {
            LOG.error("Error while performing JCR operations", ex);
        }
    }

    public void persistStatus(ResourceResolver rr) throws PersistenceException {
        try {
            Map<String, Object> props = new HashMap<>();
            props.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FOLDER);
            ResourceUtil.getOrCreateResource(rr, BASE_PATH, props, null, true);
            props.put(JcrConstants.JCR_PRIMARYTYPE, "cq:Page");
            ResourceUtil.getOrCreateResource(rr, getPath(), props, null, true);
            ModifiableValueMap jcrContent = ResourceUtil.getOrCreateResource(rr, getPath() + "/jcr:content", ProcessInstance.RESOURCE_TYPE, null, false).adaptTo(ModifiableValueMap.class);
            jcrContent.put("jcr:primaryType", "cq:PageContent");
            jcrContent.put("jcr:title", getName());
            ValueMapSerializer.serializeToMap(jcrContent, infoBean);
            ModifiableValueMap resultNode = ResourceUtil.getOrCreateResource(rr, getPath() + "/jcr:content/result", ProcessInstance.RESOURCE_TYPE + "/result", null, false).adaptTo(ModifiableValueMap.class);
            resultNode.put("jcr:primaryType", JcrConstants.NT_UNSTRUCTURED);
            ValueMapSerializer.serializeToMap(resultNode, infoBean.getResult());
            rr.commit();
            rr.refresh();
        } catch (NullPointerException ex) {
            throw new PersistenceException("Null encountered when persisting status", ex);
        }
    }

    @Override
    public ManagedProcess getInfo() {
        return infoBean;
    }

    @Override
    public final void halt() {
        updateProgress();
        infoBean.setStopTime(System.currentTimeMillis());
        infoBean.getResult().setRuntime(infoBean.getStopTime() - infoBean.getStartTime());
        infoBean.setIsRunning(false);
        if (completedNormally) {
            setStatusCompleted();
        } else {
            setStatusAborted();
        }
        asServiceUser(rr -> {
            persistStatus(rr);
            definition.storeReport(this, rr);
        });
        actions.stream().map(a -> a.manager).forEach(getActionManagerFactory()::purge);
        manager.purgeCompletedProcesses();
    }

    public static TabularType getStaticsTableType() {
        return statsTabularType;
    }

    @Override
    public CompositeData getStatistics() {
        try {
            return new CompositeDataSupport(statsCompositeType, statsItemNames,
                    new Object[]{
                        id,
                        getName(),
                        actions.size(),
                        actions.stream().filter(a -> a.manager.isComplete()).collect(Collectors.counting()),
                        actions.stream().map(a -> a.manager.getSuccessCount()).collect(Collectors.summingInt(Integer::intValue)),
                        actions.stream().map(a -> a.manager.getErrorCount()).collect(Collectors.summingInt(Integer::intValue)),
                        getRuntime(),
                        updateProgress()
                    }
            );
        } catch (OpenDataException ex) {
            LOG.error("Error building output summary", ex);
            return null;
        }
    }

    private static String[] statsItemNames;
    private static CompositeType statsCompositeType;
    private static TabularType statsTabularType;

    static {
        try {
            statsItemNames = new String[]{"_id", "_taskName", "started", "completed", "successful", "errors", "runtime", "pct_complete"};
            statsCompositeType = new CompositeType(
                    "Statics Row",
                    "Single row of statistics",
                    statsItemNames,
                    new String[]{"ID", "Name", "Started", "Completed", "Successful", "Errors", "Runtime", "Percent complete"},
                    new OpenType[]{SimpleType.STRING, SimpleType.STRING, SimpleType.INTEGER, SimpleType.LONG, SimpleType.INTEGER, SimpleType.INTEGER, SimpleType.LONG, SimpleType.DOUBLE});
            statsTabularType = new TabularType("Statistics", "Collected statistics", statsCompositeType, new String[]{"_id"});
        } catch (OpenDataException ex) {
            LOG.error("Unable to build MBean composite types", ex);
        }
    }
}
