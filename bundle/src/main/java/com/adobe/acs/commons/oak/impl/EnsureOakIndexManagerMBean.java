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
package com.adobe.acs.commons.oak.impl;

import com.adobe.acs.commons.oak.EnsureOakIndexManager;
import com.adobe.granite.jmx.annotation.Description;

import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.TabularData;

/**
 * JMX MBean for EnsureOakIndexManager.
 */
@Description("ACS AEM Commons - Ensure Oak Index Manager")
public interface EnsureOakIndexManagerMBean extends EnsureOakIndexManager {

    @Description("Ensure Oak Indexes")
    TabularData getEnsureOakIndexes() throws OpenDataException;
}
