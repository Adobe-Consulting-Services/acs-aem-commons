package com.adobe.acs.commons.adobeio.core.model.impl;

import com.adobe.acs.commons.adobeio.core.internal.Constants;
import com.adobe.acs.commons.adobeio.core.model.EndpointsList;
import com.adobe.acs.commons.adobeio.core.service.EndpointService;
import com.adobe.acs.commons.adobeio.core.service.EndpointServiceFactory;
import com.google.common.collect.Lists;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.osgi.service.component.annotations.Reference;

import javax.annotation.PostConstruct;
import java.util.List;

@SuppressWarnings("PackageAccessibility")
@Model(adaptables = SlingHttpServletRequest.class,
        adapters = EndpointsList.class)
@Exporter(name = Constants.EXPORTER_NAME,
        extensions = Constants.EXPORTER_EXTENSION)
public class EndpointListImpl implements EndpointsList {

    @Reference
    private EndpointServiceFactory configurationFactory;

    private List<EndpointService> endpointServices;

    @PostConstruct
    private void initModel() {
        endpointServices = Lists.newArrayList();
    }

    @Override
    public List<EndpointService> getEndpoints() {
        return endpointServices;
    }
}