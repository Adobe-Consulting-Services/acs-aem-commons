/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
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

package com.adobe.acs.commons.analysis.jcrchecksum.impl.options;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class CustomChecksumGeneratorOptions extends AbstractChecksumGeneratorOptions {
    private static final Logger log = LoggerFactory.getLogger(DefaultChecksumGeneratorOptions.class);

    public CustomChecksumGeneratorOptions() {

    }

    @Override
    public void addIncludedNodeTypes(String[] arr) {
        super.addIncludedNodeTypes(arr);
    }

    @Override
    public void addExcludedNodeTypes(String[] arr) {
        super.addExcludedNodeTypes(arr);
    }

    @Override
    public void addExcludedProperties(String[] arr) {
        super.addExcludedProperties(arr);
    }

    @Override
    public void addSortedProperties(String[] arr) {
        super.addSortedProperties(arr);
    }


    public void addIncludedNodeTypes(Collection<String> col) {
        if (col != null) {
            super.addIncludedNodeTypes(col.toArray(new String[col.size()]));
        }
    }

    public void addExcludedNodeTypes(Collection<String> col) {
        if (col != null) {
            super.addExcludedNodeTypes(col.toArray(new String[col.size()]));
        }
    }

    public void addExcludedProperties(Collection<String> col) {
        if (col != null) {
            super.addExcludedProperties(col.toArray(new String[col.size()]));
        }
    }

    public void addSortedProperties(Collection<String> col) {
        if (col != null) {
            super.addSortedProperties(col.toArray(new String[col.size()]));
        }
    }

}