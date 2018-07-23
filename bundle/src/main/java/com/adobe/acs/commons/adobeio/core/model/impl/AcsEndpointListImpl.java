package com.adobe.acs.commons.adobeio.core.model.impl;

import com.adobe.acs.commons.adobeio.core.internal.Constants;
import com.adobe.acs.commons.adobeio.core.model.AcsEndpointsList;
import com.adobe.acs.commons.adobeio.core.service.ACSEndpointService;
import com.adobe.acs.commons.adobeio.core.service.ACSEndpointServiceFactory;
import com.google.common.collect.Lists;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.osgi.service.component.annotations.Reference;

import javax.annotation.PostConstruct;
import java.util.List;

@SuppressWarnings("PackageAccessibility")
@Model(adaptables = SlingHttpServletRequest.class,
        adapters = AcsEndpointsList.class)
@Exporter(name = Constants.EXPORTER_NAME,
        extensions = Constants.EXPORTER_EXTENSION)
public class AcsEndpointListImpl implements AcsEndpointsList {

    @Reference
    private ACSEndpointServiceFactory configurationFactory;

    private List<ACSEndpointService> endpointServices;

    @PostConstruct
    private void initModel() {
        endpointServices = Lists.newArrayList();
    }

    @Override
    public List<ACSEndpointService> getEndpoints() {
        return endpointServices;
    }
}