package com.adobe.acs.commons.adobeio.core.service.impl;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.adobeio.core.service.ACSEndpointService;
import com.adobe.acs.commons.adobeio.core.service.ACSEndpointServiceFactory;
import com.adobe.acs.commons.adobeio.core.types.Action;
import com.drew.lang.annotations.NotNull;
import com.google.common.collect.Maps;

@Component(immediate = true, service = ACSEndpointServiceFactory.class)
public class ACSEndpointServiceFactoryImpl implements ACSEndpointServiceFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ACSEndpointServiceFactoryImpl.class);
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final Map<String, ACSEndpointService> endpointServices = Maps.newHashMap();

    @Reference(name = "configurationFactory", cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected synchronized void addAcsEndpointService(final ACSEndpointService config) {
        LOGGER.debug("Started Binding");
        if (config != null) {
            synchronized (this.endpointServices) {
                this.endpointServices.put(config.getId(), config);
            }
        }
    }

    protected synchronized void removeAcsEndpointService(final ACSEndpointService endpointService) {
        LOGGER.debug("Started Unbinding");
        if (endpointService != null) {
            synchronized (this.endpointServices) {
                this.endpointServices.remove(endpointService.getId());
            }
        }
    }

    @Override
    public ACSEndpointService getEndpoint(@NotNull Action action) {

        if ((action != null) && StringUtils.isNotBlank(action.getValue())) {
            return endpointServices.get(action.getValue());
        }
        return null;
    }
}
