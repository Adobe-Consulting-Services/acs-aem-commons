package com.adobe.acs.commons.adobeio.core.service.impl;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.adobeio.core.service.EndpointService;
import com.adobe.acs.commons.adobeio.core.service.EndpointServiceFactory;
import com.adobe.acs.commons.adobeio.core.types.Action;
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
