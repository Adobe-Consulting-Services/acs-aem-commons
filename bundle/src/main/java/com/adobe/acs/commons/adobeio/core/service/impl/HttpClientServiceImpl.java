package com.adobe.acs.commons.adobeio.core.service.impl;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.Designate;

import com.adobe.acs.commons.adobeio.core.config.HttpClientConfiguration;
import com.adobe.acs.commons.adobeio.core.service.HttpClientService;

@Component(service = {HttpClientService.class},
immediate = true)
@Designate(ocd = HttpClientConfiguration.class)
public class HttpClientServiceImpl implements HttpClientService {

    @Override
    public CloseableHttpClient getHttpClient() {
        return HttpClientBuilder.create().build();
    }
}
