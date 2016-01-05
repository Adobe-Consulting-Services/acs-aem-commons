/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2014 Adobe
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
package com.adobe.acs.commons.oak;

import com.adobe.granite.jmx.annotation.Description;
import com.adobe.granite.jmx.annotation.Name;

/**
 * OSGi Service interface for managing Ensure Oak Indexes.
 */
public interface EnsureOakIndexManager {

    /**
     * Applies all un-applied Ensure Oak Index definitions.
     * @param force true to re-apply Ensure Oak Indexes that have been marked as applied, false to skip.
     * @return the number of ensure oak indexes processed
     */
    @Description("Ensure all Ensure Oak Index Definitions")
    int ensureAll(@Name(value="force") boolean force);

    /**
     * Applies un-applied Ensure Oak Index definitions whose ensure-definition.path @Property is a parameter.
     * @param force true to re-apply Ensure Oak Indexes that have been marked as applied, false to skip.
     * @param ensureDefinitionsPath the path of the Ensure Oak Index Definitions to apply.
     * @return the number of ensure oak indexes processed
     */
    @Description("Ensure select Ensure Oak Index Definitions")
    int ensure(@Name(value="force") boolean force,
                @Name(value="ensureDefinitionsPath") String ensureDefinitionsPath);
}
