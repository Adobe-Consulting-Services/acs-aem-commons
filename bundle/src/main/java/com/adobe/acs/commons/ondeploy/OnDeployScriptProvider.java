/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2018 Adobe
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
package com.adobe.acs.commons.ondeploy;

import aQute.bnd.annotation.ConsumerType;
import com.adobe.acs.commons.ondeploy.scripts.OnDeployScript;

import java.util.List;

/**
 * Provider interface to supply a list of scripts to be run by the OnDeployExecutor.
 */
@ConsumerType
public interface OnDeployScriptProvider {
    /**
     * Get the list of scripts to run.
     *
     * Ideally, all scripts will remain in the list indefinitely, so that a new AEM
     * server can run all scripts from the first to the last to be
     * completely up to date w/no manual intervention.  As a reminder, scripts will
     * run only once, so it is safe to preserve the entire list of scripts.
     *
     * @return List of OnDeployScript instances.
     */
    List<OnDeployScript> getScripts();
}
