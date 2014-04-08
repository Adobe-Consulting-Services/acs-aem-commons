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
package com.adobe.acs.commons.config;

import java.util.List;
import java.util.Map;

/**
 * The configuration object that has methods to allow easy retrieving of
 * row/rows of configurations. If the configuration is not available, the search
 * will be done in the parent configuration page and so on.
 * 
 * 
 */
public interface Configuration {

    /**
     * Returns the first available configuration , the key column of which
     * matches the method argument key. If the configuration is not available,
     * the search will be done in the parent configuration page and so on.
     * 
     * @param key
     *            the key checked against the value in column key for each rows
     * @return
     */
    Map<String, String> getRowByKey(String key);

    /**
     * returns all the list of configurations with key column matching the
     * method argument key. If the configuration is not available, the search
     * will be done in the parent configuration page and so on.
     * 
     * @param key
     * @return
     */
    List<Map<String, String>> getRowsByKey(String key);

    /**
     * Returns the first available configuration , for which the columnName
     * column has the value columnValue. If the configuration is not available,
     * the search will be done in the parent configuration page and so on.
     * 
     * @param columnName
     * @param columnValue
     * @return
     */
    Map<String, String> getRowByColumnValue(String columnName,
            String columnValue);

    /**
     * Returns all the list of configurations , for which the columnName column
     * has the value columnValue. If the configuration is not available, the
     * search will be done in the parent configuration page and so on.
     * 
     * @param columnName
     * @param columnValue
     * @return
     */
    List<Map<String, String>> getRowsByColumnValue(String columnName,
            String columnValue);
}
