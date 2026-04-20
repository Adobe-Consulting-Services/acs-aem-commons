/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
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
package com.adobe.acs.commons.ondeploy.impl;

import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.TabularDataSupport;

import com.adobe.granite.jmx.annotation.Description;
import com.adobe.granite.jmx.annotation.Name;

@Description("ACS AEM Commons - On Deploy Script Executor MBean")
public interface OnDeployExecutorMBean {

    @Description("Scripts")
    TabularDataSupport getScripts() throws OpenDataException;

    @Description("Execute the script, given it's fully-qualified class name.  If force==true, script is executed even if it has previously succeeded.")
    boolean executeScript(@Name(value = "scriptName") String scriptName,
            @Name(value = "force") boolean force);

}
