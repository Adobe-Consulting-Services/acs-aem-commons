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

package com.adobe.acs.commons.oak.impl;

import java.util.List;

/**
 * Internal interface that exposes methods for interacting the EnsureOakIndexes.
 */
public interface AppliableEnsureOakIndex {

    /**
     * Apply this Ensure Oak Index.
     */
    void apply(boolean force);

    /**
     * @return the ensure definition path for this Ensure Oak Index path for this Ensure Oak Index component.
     */
    String getEnsureDefinitionsPath();

    /**
     * @return the oak path to ensure.
     */
    String getOakIndexesPath();

    /**
     * Determines if the index definition has been applied to the system.
     * This does not necessarily mean, that the index has already been created.
     *
     * @return true if the index definition has been applied.
     */
    boolean isApplied();

    /**
     * @return true is Ensure Oak Index is immediate.
     */
    boolean isImmediate();

    /**
     * @return the list of additional properties to ignore
     */
    List<String> getIgnoreProperties();
}
