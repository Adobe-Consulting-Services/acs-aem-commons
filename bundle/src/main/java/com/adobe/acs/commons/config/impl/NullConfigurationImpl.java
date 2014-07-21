/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2014 Adobe
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
package com.adobe.acs.commons.config.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.adobe.acs.commons.config.Configuration;

public class NullConfigurationImpl implements Configuration {

    @Override
    public final Map<String, String> getRowByKey(String key) {

        return Collections.emptyMap();
    }

    @Override
    public final List<Map<String, String>> getRowsByKey(String key) {

        return Collections.emptyList();
    }

    @Override
    public final Map<String, String> getRowByColumnValue(String columnName,
            String columnValue) {

        return Collections.emptyMap();
    }

    @Override
    public final List<Map<String, String>> getRowsByColumnValue(String columnName,
            String columnValue) {

        return Collections.emptyList();
    }

}
