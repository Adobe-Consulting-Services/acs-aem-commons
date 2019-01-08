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
package com.adobe.acs.commons.mcp.mbean;

import aQute.bnd.annotation.ProviderType;
import com.adobe.granite.jmx.annotation.Description;
import com.adobe.granite.jmx.annotation.Name;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.TabularDataSupport;

/**
 * Provide details about running tasks
 */
@Description("Controlled Process Management")
@ProviderType
@SuppressWarnings("checkstyle:abbreviationaswordinname")
public interface CPMBean {
    @Description("Processes")
    public TabularDataSupport getStatistics() throws OpenDataException;
    
    @Description("Halt process by id")
    void haltProcessById(@Name("ID") @Description("Process ID") String id);

    @Description("Halt process by JCR path")
    void haltProcessByPath(@Name("Path") @Description("Process Path") String path);

    @Description("Halt any current or queued up processes")
    void haltActiveProcesses();
    
    @Description("Remove record of completed processes")
    void purgeCompletedProcesses();
}
