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
package com.adobe.acs.commons.redirects.filter;

import com.adobe.granite.jmx.annotation.Description;

import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.TabularData;
import java.util.Collection;

@Description("ACS Redirect Manager MBean")
public interface RedirectFilterMBean {

    @Description("Invalidate all cached rules")
    void invalidateAll();

    @Description("Loaded redirect rules")
    TabularData getRedirectRules(String storagePath) throws OpenDataException;

    @Description("Known redirect configurations")
    Collection<String> getRedirectConfigurations();

    @Description("Configuration bucket to store redirects")
    String getBucket();

    @Description("Node name to store redirect configurations")
    String getConfigName();
}