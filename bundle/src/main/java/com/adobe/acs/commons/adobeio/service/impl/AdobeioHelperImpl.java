/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2018 Adobe
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

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.osgi.services.HttpClientBuilderFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
public class AdobeioHelperImpl implements AdobeioHelper {

    @Reference
    private HttpClientBuilderFactory clientBuilderFactory;

    @Override
    public CloseableHttpClient getHttpClient(int timeoutInMilliSeconds) {
        RequestConfig requestConfig = RequestConfig.copy(RequestConfig.DEFAULT)
            .setSocketTimeout(timeoutInMilliSeconds)
            .setConnectTimeout(timeoutInMilliSeconds)
            .setConnectionRequestTimeout(timeoutInMilliSeconds)
            .build();
        return clientBuilderFactory.newBuilder().setDefaultRequestConfig(requestConfig).build();
    }
}
