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
package com.adobe.acs.commons.fam;

import com.adobe.acs.commons.functions.Consumer;
import java.util.ArrayList;
import java.util.List;
import javax.jcr.RepositoryException;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * Abstraction of a Process which runs using FAM and consists of one or more
 * actions.
 */
public abstract class ControlledProcess {

    private final String name;
    private final ActionManagerFactory amf;
    private final List<ActivityDefinition> actions;

    private static class ActivityDefinition {
        String name;
        ActionManager manager;
        Consumer<ActionManager> builder;
        boolean critical = false;
    }

    public ControlledProcess(ActionManagerFactory amf, String name) {
        this.amf = amf;
        this.name = name;
        this.actions = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public ActionManagerFactory getActionManagerFactory() {
        return amf;
    }

    public abstract void buildProcess(ResourceResolver rr) throws LoginException, RepositoryException;

    final public ActionManager defineCriticalAction(String name, ResourceResolver rr, Consumer<ActionManager> builder) throws LoginException {
        return defineAction(name, rr, builder, true);
    }

    final public ActionManager defineAction(String name, ResourceResolver rr, Consumer<ActionManager> builder) throws LoginException {
        return defineAction(name, rr, builder, false);
    }

    private ActionManager defineAction(String name, ResourceResolver rr, Consumer<ActionManager> builder, boolean isCritical) throws LoginException {
        ActivityDefinition definition = new ActivityDefinition();
        definition.builder = builder;
        definition.name = name;
        definition.manager = amf.createTaskManager(getName() + ": " + name, rr, 1);
        definition.critical = isCritical;
        actions.add(definition);
        return definition.manager;
    }

    final public void run() {
        runStep(0);
    }

    private void runStep(int step) {
        if (step >= actions.size()) {
            wrapUp();
        } else {
            ActivityDefinition action = actions.get(step);
            if (action.critical) {
                action.manager.onSuccess(rr -> runStep(step + 1));
                action.manager.onFailure((failures, rr) -> haltOnError(step, failures, rr));
            } else {
                action.manager.onFailure((failures, rr) -> recordErrors(step, failures, rr));
                action.manager.onFinish(()->runStep(step+1));
            }
            action.manager.deferredWithResolver(rr->action.builder.accept(action.manager));
        }
    }

    final private void recordErrors(int step, List<Failure> failures, ResourceResolver rr) {
        //...
    }
    
    final private void haltOnError(int step, List<Failure> failures, ResourceResolver rr) {
        //...
    }

    final public void wrapUp() {
        actions.stream().map(a->a.manager).forEach(ActionManager::closeAllResolvers);
    }
}
