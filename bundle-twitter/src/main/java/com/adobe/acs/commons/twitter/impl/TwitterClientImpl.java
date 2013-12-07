/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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
package com.adobe.acs.commons.twitter.impl;

import twitter4j.Twitter;

import com.adobe.acs.commons.twitter.TwitterClient;
import com.day.cq.wcm.webservicesupport.Configuration;

public final class TwitterClientImpl implements TwitterClient {

    private final Twitter twitter;

    private final com.day.cq.wcm.webservicesupport.Configuration serviceConfiguration;

    public TwitterClientImpl(Twitter impl, com.day.cq.wcm.webservicesupport.Configuration configuration) {
        this.twitter = impl;
        this.serviceConfiguration = configuration;
    }

    @Override
    public Configuration getServiceConfiguration() {
        return serviceConfiguration;
    }

    @Override
    public Twitter getTwitter() {
        return twitter;
    }


}
