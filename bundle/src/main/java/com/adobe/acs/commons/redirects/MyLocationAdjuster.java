package com.adobe.acs.commons.redirects;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.osgi.service.component.annotations.Component;

@Component
public class MyLocationAdjuster implements LocationHeaderAdjuster{
    @Override
    public String adjust(SlingHttpServletRequest request, String location) {

        if(location.startsWith("/content/we-retail/de/")){
            String loc = StringUtils.substringAfter(location,"/content/we-retail/de/");
            return "https://www.we-retail.de/" + loc;
        }
        return location;
    }
}
