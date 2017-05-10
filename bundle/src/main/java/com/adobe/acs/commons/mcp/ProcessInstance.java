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
package com.adobe.acs.commons.mcp;

import com.adobe.acs.commons.fam.ActionManager;
import com.adobe.acs.commons.fam.ActionManagerFactory;
import com.adobe.acs.commons.functions.CheckedConsumer;
import com.adobe.acs.commons.mcp.model.ManagedProcess;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * Abstraction of a Process which runs using FAM and consists of one or more
 * actions.
 */
public interface ProcessInstance {

    String getName();
    
    void init(ControlledProcessManager cpm);
    
    ActionManagerFactory getActionManagerFactory();
 
    ActionManager defineCriticalAction(String name, ResourceResolver rr, CheckedConsumer<ActionManager> builder) throws LoginException;

    ActionManager defineAction(String name, ResourceResolver rr, CheckedConsumer<ActionManager> builder) throws LoginException;
    
    ManagedProcess getInfo();
    
    void run(ResourceResolver rr);

    void terminate();
}
