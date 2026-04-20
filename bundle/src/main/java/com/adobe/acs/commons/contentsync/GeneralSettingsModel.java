/*-
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2022 Adobe
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
package com.adobe.acs.commons.contentsync;

import com.adobe.acs.commons.contentsync.impl.LastModifiedStrategy;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * Adapts to configuration nodes in /var/acs-commons/contentsync/hosts/*
 */
@Model(adaptables = {Resource.class}, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class GeneralSettingsModel {
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @ValueMapValue
    @Default(intValues = 60000)
    private int soTimeout;

    @ValueMapValue
    @Default(intValues = 5000)
    private int connTimeout;

    @ValueMapValue
    private boolean disableCertCheck;

    @ValueMapValue(name = "event-user-data")
    @Default(values = "changedByPageManagerCopy")
    private String eventUserData;

    @ValueMapValue(name = "update-strategy")
    private String strategyPid;

    @Self
    private Resource resource;

    public int getSocketTimeout() {
        return soTimeout;
    }

    public int getConnectTimeout() {
        return connTimeout;
    }

    public boolean isDisableCertCheck() {
        return disableCertCheck;
    }

    public String getEventUserData() {
        return eventUserData;
    }

    public String getStrategyPid() {
        return strategyPid == null ? LastModifiedStrategy.class.getName() : strategyPid;
    }


}
