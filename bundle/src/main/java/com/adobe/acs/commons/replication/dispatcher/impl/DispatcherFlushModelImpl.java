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

package com.adobe.acs.commons.replication.dispatcher.impl;

import com.adobe.acs.commons.replication.dispatcher.DispatcherFlushFilter;
import com.adobe.acs.commons.replication.dispatcher.DispatcherFlusher;
import com.adobe.acs.commons.replication.dispatcher.DispatcherFlusherModel;
import com.day.cq.replication.Agent;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Model(
        adaptables = SlingHttpServletRequest.class,
        adapters = DispatcherFlusherModel.class,
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class DispatcherFlushModelImpl implements DispatcherFlusherModel {

    @Self
    private SlingHttpServletRequest request;

    @OSGiService
    private DispatcherFlusher dispatcherFlusher;

    @ValueMapValue
    @Default(values = "")
    private String replicationActionType;

    @ValueMapValue
    private List<String> paths;

    @Override
    public String getActionType() {
        return replicationActionType;
    }

    @Override
    public Collection<String> getPaths() {
        return paths != null ? paths : Collections.emptyList();
    }

    @Override
    public Collection<Agent> getAgents() {
        return Arrays.asList(dispatcherFlusher.getAgents(DispatcherFlushFilter.HIERARCHICAL));
    }


    @Override
    public boolean isFailure() {
        return StringUtils.equalsIgnoreCase("/replication-error", request.getRequestPathInfo().getSuffix());
    }

    @Override
    public List<String> getResults() {
        return Arrays.asList(StringUtils.split(request.getRequestPathInfo().getSuffix(), "/"));
    }

    @Override
    public boolean isReady() {
        return StringUtils.isNotBlank(replicationActionType) && !paths.isEmpty() & !getAgents().isEmpty();
    }
}
