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
package com.adobe.acs.commons.mcp.impl;

import com.adobe.acs.commons.mcp.*;
import com.adobe.acs.commons.fam.ActionManager;
import com.adobe.acs.commons.fam.ActionManagerFactory;
import com.adobe.acs.commons.fam.Failure;
import com.adobe.acs.commons.functions.CheckedConsumer;
import com.adobe.acs.commons.mcp.model.ManagedProcess;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import javax.jcr.RepositoryException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularType;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ModifiableValueMapDecorator;
import org.slf4j.LoggerFactory;

/**
 * Abstraction of a Process which runs using FAM and consists of one or more
 * actions.
 */
public class ProcessInstanceImpl implements ProcessInstance {

    transient private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(ProcessInstanceImpl.class);

    transient private ControlledProcessManager manager = null;
    private final List<ActivityDefinition> actions;
    private final ProcessDefinition definition;
    private final ManagedProcess infoBean;
    private final String id;
    private final String path;
    transient private static final Random RANDOM = new Random();

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getPath() {
        return path;
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
        infoBean.setDescription(description);
        id = String.format("%016X", Math.abs(RANDOM.nextLong()));
        path = "/var/acs/mcp/instances/" + id;
    }

    @Override
    public String getName() {
        return definition.getName() != null
                ? (infoBean.getDescription() != null
                ? definition.getName() + ": " + infoBean.getDescription()
                : definition.getName())
                : (infoBean.getDescription() != null
                ? infoBean.getDescription()
                : "No idea");
    }

    @Override
    public void init(ResourceResolver resourceResolver, Map<String, Object> parameterMap) throws RepositoryException {
        ValueMap inputs = new ModifiableValueMapDecorator(parameterMap);
        infoBean.setRequestInputs(inputs);
        definition.parseInputs(inputs);
    }

    @Override
    public ActionManagerFactory getActionManagerFactory() {
        return manager.getActionManagerFactory();
    }

    @Override
    final public ActionManager defineCriticalAction(String name, ResourceResolver rr, CheckedConsumer<ActionManager> builder) throws LoginException {
        return defineAction(name, rr, builder, true);
    }

    @Override
    final public ActionManager defineAction(String name, ResourceResolver rr, CheckedConsumer<ActionManager> builder) throws LoginException {
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
    final public void run(ResourceResolver rr) {
        try {
            definition.buildProcess(this, rr);
            infoBean.setStartTime(System.currentTimeMillis());
            runStep(0);
        } catch (LoginException | RepositoryException ex) {
            LOG.error("Error starting managed process " + getName(), ex);
            recordCancellation();
            halt();
        }
    }

    private void runStep(int step) {
        if (step >= actions.size()) {
            recordCompletion();
            halt();
        } else {
            ActivityDefinition action = actions.get(step);
            if (action.critical) {
                action.manager.onSuccess(rr -> runStep(step + 1));
                action.manager.onFailure((failures, rr) -> {
                    recordErrors(step, failures, rr);
                    recordCancellation();
                    halt();
                });
            } else {
                action.manager.onFailure((failures, rr) -> recordErrors(step, failures, rr));
                action.manager.onFinish(() -> runStep(step + 1));
            }
            action.manager.deferredWithResolver(rr -> action.builder.accept(action.manager));
        }
    }

    private void recordErrors(int step, List<Failure> failures, ResourceResolver rr) {
        //...
    }

    private long getRuntime() {
        long stop = System.currentTimeMillis();
        if (infoBean.getStopTime() > infoBean.getStartTime()) {
            stop = infoBean.getStopTime();
        }
        return stop - infoBean.getStartTime();
    }

    private void recordCompletion() {
        infoBean.setStopTime(System.currentTimeMillis());
    }

    private void recordCancellation() {
        infoBean.setStopTime(System.currentTimeMillis());
    }

    @Override
    public ManagedProcess getInfo() {
        return infoBean;
    }

    @Override
    final public void halt() {
        actions.stream().map(a -> a.manager).forEach(getActionManagerFactory()::purge);
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
                        getRuntime()
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
            statsItemNames = new String[]{"_id", "_taskName", "started", "completed", "successful", "errors", "runtime"};
            statsCompositeType = new CompositeType(
                    "Statics Row",
                    "Single row of statistics",
                    statsItemNames,
                    new String[]{"ID", "Name", "Started", "Completed", "Successful", "Errors", "Runtime"},
                    new OpenType[]{SimpleType.STRING, SimpleType.STRING, SimpleType.INTEGER, SimpleType.LONG, SimpleType.INTEGER, SimpleType.INTEGER, SimpleType.LONG});
            statsTabularType = new TabularType("Statistics", "Collected statistics", statsCompositeType, new String[]{"_id"});
        } catch (OpenDataException ex) {
            LOG.error("Unable to build MBean composite types", ex);
        }
    }

}
