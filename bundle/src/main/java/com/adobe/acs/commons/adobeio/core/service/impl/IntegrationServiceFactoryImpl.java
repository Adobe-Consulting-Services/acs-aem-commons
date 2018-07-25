package com.adobe.acs.commons.adobeio.core.service.impl;

import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.adobeio.core.service.IntegrationService;
import com.adobe.acs.commons.adobeio.core.service.IntegrationServiceFactory;
import com.adobe.acs.commons.adobeio.exception.AdobeIOException;
import com.drew.lang.annotations.NotNull;
import com.google.common.collect.Maps;

@Component(immediate = true, service = IntegrationServiceFactory.class)
public class IntegrationServiceFactoryImpl implements IntegrationServiceFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(IntegrationServiceFactoryImpl.class);
    private final Map<String, IntegrationService> integrationServices = Maps.newHashMap();

    // needs to be add... according to http://enroute.osgi.org/services/org.osgi.service.component.html
    @Reference(name = "configurationFactory", cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected synchronized void addIntegrationService(final IntegrationService service) {
        LOGGER.debug("Started Binding");
        if (service != null) {
            synchronized (this.integrationServices) {
                this.integrationServices.put(service.getTenant() + service.getIntegrationID(), service);
            }
        }
    }

    // needs to be remove... according to http://enroute.osgi.org/services/org.osgi.service.component.html
    protected synchronized void removeIntegrationService(final IntegrationService service) {
        LOGGER.debug("Started Unbinding");
        if (service!= null) {
            synchronized (this.integrationServices) {
                this.integrationServices.remove(service.getTenant() + service.getIntegrationID());
            }
        }
    }

    @Override
    public IntegrationService getService(@NotNull final String tenant, @NotNull  final String service) throws AdobeIOException {
        final IntegrationService integrationService = integrationServices.get(tenant + service);
        if (null == integrationService) {
            throw new AdobeIOException("Integration service not found for tenant " + tenant + " and service " + service);
        }
        return integrationService;
    }
}
