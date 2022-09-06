package com.adobe.acs.commons.util;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;

@Model(
        adaptables = { SlingHttpServletRequest.class, Resource.class },
        defaultInjectionStrategy = DefaultInjectionStrategy.REQUIRED
)
public class RequireAemModel implements RequireAem {

    @OSGiService
    private RequireAem requireAem;

    @Override
    public Distribution getDistribution() {
        return requireAem.getDistribution();
    }
}
