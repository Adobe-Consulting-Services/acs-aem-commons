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
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jcr.RepositoryException;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * Abstraction of a Process which runs using FAM and consists of one or more
 * actions.
 */
public class ProcessInstanceImpl implements ProcessInstance {
    private ControlledProcessManager manager = null;
    private final List<ActivityDefinition> actions;
    private final ProcessDefinition definition;
    private final ManagedProcess infoBean;
    private final String id;
    private final String path;
    private static final Random rng = new Random();

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
        CheckedConsumer<ActionManager> builder;
        boolean critical = false;
    }

    public ProcessInstanceImpl(ProcessDefinition process, String description) {
        infoBean = new ManagedProcess();
        this.actions = new ArrayList<>();
        this.definition = process;
        infoBean.setDescription(description);
        id = String.format("%016X", Math.abs(rng.nextLong()));
        path = "/var/acs/mcp/instances/"+id;
    }

    public String getName() {
        return definition.getName() + ": " + infoBean.getDescription();
    }
    
    @Override
    public void init(ControlledProcessManager cpm) {
        manager = cpm;
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
            runStep(0);
        } catch (LoginException | RepositoryException ex) {
            Logger.getLogger(ProcessInstanceImpl.class.getName()).log(Level.SEVERE, null, ex);
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
                action.manager.onFinish(()->runStep(step+1));
            }
            action.manager.deferredWithResolver(rr->action.builder.accept(action.manager));
        }
    }

    private void recordErrors(int step, List<Failure> failures, ResourceResolver rr) {
        //...
    }
    

    private void recordCompletion() {
    }

    private void recordCancellation() {
    }

    @Override
    public ManagedProcess getInfo() {
        return infoBean;
    }    
    
    @Override
    final public void halt() {
        actions.stream().map(a->a.manager).forEach(getActionManagerFactory()::purge);
    }
}
