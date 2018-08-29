/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2018 Adobe
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
package com.adobe.acs.commons.adobeio.service.impl;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.adobeio.service.EndpointService;
import com.adobe.acs.commons.adobeio.service.EndpointServiceFactory;
import com.adobe.acs.commons.adobeio.types.Action;
import com.drew.lang.annotations.NotNull;
import com.google.common.collect.Maps;

@Component(immediate = true, service = EndpointServiceFactory.class)
public class EndpointServiceFactoryImpl implements EndpointServiceFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointServiceFactoryImpl.class);
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final Map<String, EndpointService> endpointServices = Maps.newHashMap();

    @Reference(name = "configurationFactory", cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected synchronized void addAcsEndpointService(final EndpointService config) {
        LOGGER.debug("Started Binding");
        if (config != null) {
            synchronized (this.endpointServices) {
                this.endpointServices.put(config.getId(), config);
            }
        }
    }

    protected synchronized void removeAcsEndpointService(final EndpointService endpointService) {
        LOGGER.debug("Started Unbinding");
        if (endpointService != null) {
            synchronized (this.endpointServices) {
                this.endpointServices.remove(endpointService.getId());
            }
        }
    }

    @Override
    public EndpointService getEndpoint(@NotNull Action action) {

        if ((action != null) && StringUtils.isNotBlank(action.getValue())) {
            return endpointServices.get(action.getValue());
        }
        return null;
    }
}
