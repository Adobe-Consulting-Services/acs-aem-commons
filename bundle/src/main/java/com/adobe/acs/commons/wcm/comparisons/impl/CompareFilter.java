/*
 *
 *  * #%L
 *  * ACS AEM Commons Bundle
 *  * %%
 *  * Copyright (C) 2016 Adobe
 *  * %%
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  * #L%
 *
 */
package com.adobe.acs.commons.wcm.comparisons.impl;


import org.apache.commons.lang3.ArrayUtils;

import java.util.regex.Pattern;

class CompareFilter {

    private final String[] ignoreProperties;
    private final String[] ignoreResources;

    CompareFilter(String[] ignoreProperties, String[] ignoreResources) {
        this.ignoreProperties = ArrayUtils.clone(ignoreProperties);
        this.ignoreResources = ArrayUtils.clone(ignoreResources);
    }

    boolean filterProperty(String name) {
        for (String entry : ignoreProperties) {
            if (Pattern.matches(entry, name)) {
                return true;
            }
        }
        return false;
    }

    boolean filterResource(String name) {
        for (String entry : ignoreResources) {
            if (Pattern.matches(entry, name)) {
                return true;
            }
        }
        return false;
    }
}
