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
package com.adobe.acs.commons.fam;

import aQute.bnd.annotation.ProviderType;
import com.adobe.acs.commons.fam.mbean.ActionManagerMBean;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;

/*
 * In addition to the mbean methods, the implementation factory object also provides a method to create a new ActionManager
 */
@ProviderType
public interface ActionManagerFactory extends ActionManagerMBean {

    /**
     * Creates an ActionManager instead with the provided name and JCR context provided bu the resourceResolver.
     * @param name the name of the ActionManager. This method guarantee uniqueness of the action manager name.
     * @param resourceResolver the resourceResolver used to perform
     * @param saveInterval the number of changed that must incur on the resourceResolver before commit() is called (in support of batch saves)
     * @return the created ActionManager
     * @throws LoginException
     */
    public ActionManager createTaskManager(String name, ResourceResolver resourceResolver, int saveInterval) throws LoginException;

    /**
     * Creates an ActionManager instead with the provided name and JCR context provided bu the resourceResolver.
     * @param name the name of the ActionManager. This method guarantee uniqueness of the action manager name.
     * @param resourceResolver the resourceResolver used to perform
     * @param saveInterval the number of changed that must incur on the resourceResolver before commit() is called (in support of batch saves)
     * @param priority the priority of execution for the tasks in this action manager
     * @return the created ActionManager
     * @throws LoginException
     */
    public ActionManager createTaskManager(String name, ResourceResolver resourceResolver, int saveInterval, int priority) throws LoginException;

    /**
     * Gets the named ActionManager from the ActionManagerFactory.
     * The name corresponds to the name provided in ActionManagerFactory.createTaskManager(..)
     * @param name the name of the ActionManager to get
     * @return the ActionManager
     */
    public ActionManager getActionManager(String name);

    /**
     * Checks if the ActionManagerFactory has a registered ActionManager with the provided name.
     * @param name the ActionManager name
     * @return true if an ActionManager is registered w the provided name, false otherwise.
     */
    public boolean hasActionManager(String name);

    /**
     * Remove a specific manager by its object reference
     * @param manager 
     */
    public void purge(ActionManager manager);
}