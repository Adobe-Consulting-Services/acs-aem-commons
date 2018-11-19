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


import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

import com.adobe.acs.commons.twitter.TwitterClient;
import com.day.cq.commons.inherit.HierarchyNodeInheritanceValueMap;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.webservicesupport.ConfigurationConstants;
import com.day.cq.wcm.webservicesupport.ConfigurationManager;

class TwitterAdapterFactory implements AdapterFactory {

    TwitterAdapterFactory(String httpProxyHost, int httpProxyPort, boolean useSsl) {
        this.httpProxyHost = httpProxyHost;
        this.httpProxyPort = httpProxyPort;
        this.useSsl = useSsl;
        this.factory = new TwitterFactory(buildConfiguration());
    }

    @VisibleForTesting
    TwitterAdapterFactory() {
        this(null, 0, true);
    }

    private static final String CLOUD_SERVICE_NAME = "twitterconnect";

    private static final Logger log = LoggerFactory.getLogger(TwitterAdapterFactory.class);

    private final TwitterFactory factory;

    private final String httpProxyHost;

    private final int httpProxyPort;

    private final boolean useSsl;



    @SuppressWarnings("unchecked")
    @Override
    public <AdapterType> AdapterType getAdapter(Object adaptable, Class<AdapterType> type) {
        TwitterClient client = null;
        if (adaptable instanceof Page) {
            client = createTwitterClientFromPage((Page) adaptable);
        } else if (adaptable instanceof com.day.cq.wcm.webservicesupport.Configuration) {
            client = createTwitterClientFromConfiguration((com.day.cq.wcm.webservicesupport.Configuration) adaptable);
        }

        if (client != null) {
            if (type == TwitterClient.class) {
                return (AdapterType) client;
            } else if (type == Twitter.class) {
                return (AdapterType) client.getTwitter();
            }
        }

        return null;
    }

    private Configuration buildConfiguration() {
        final ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.setUseSSL(useSsl);
        builder.setJSONStoreEnabled(true);
        builder.setApplicationOnlyAuthEnabled(true);
        if (StringUtils.isNotBlank(httpProxyHost) && httpProxyPort > 0) {
            builder.setHttpProxyHost(httpProxyHost);
            builder.setHttpProxyPort(httpProxyPort);
        }
        return builder.build();
    }

    private TwitterClient createTwitterClientFromConfiguration(com.day.cq.wcm.webservicesupport.Configuration config) {
        Resource oauthConfig = config.getContentResource().listChildren().next();
        ValueMap oauthProps = oauthConfig.getValueMap();
        String consumerKey = oauthProps.get("oauth.client.id", String.class);
        String consumerSecret = oauthProps.get("oauth.client.secret", String.class);

        if (consumerKey != null && consumerSecret != null) {
            Twitter twitter = getInstance();
            log.debug("Creating client for key {}.", consumerKey);
            twitter.setOAuthConsumer(consumerKey, consumerSecret);
            try {
                twitter.getOAuth2Token();
                return new TwitterClientImpl(twitter, config);
            } catch (TwitterException e) {
                log.error("Unable to create Twitter client.", e);
                return null;
            }
        } else {
            log.warn("Key or Secret missing for configuration {}", config.getPath());
        }

        return null;
    }

    @VisibleForTesting
    Twitter getInstance() {
        return factory.getInstance();
    }

    private TwitterClient createTwitterClientFromPage(Page page) {
        com.day.cq.wcm.webservicesupport.Configuration config = findTwitterConfiguration(page);
        if (config != null) {
            return createTwitterClientFromConfiguration(config);
        }
        return null;
    }

    private com.day.cq.wcm.webservicesupport.Configuration findTwitterConfiguration(Page page) {
        ConfigurationManager configurationManager = page.getContentResource().getResourceResolver().adaptTo(ConfigurationManager.class);

        final HierarchyNodeInheritanceValueMap pageProperties = new HierarchyNodeInheritanceValueMap(
                page.getContentResource());
        final String[] services = pageProperties.getInherited(ConfigurationConstants.PN_CONFIGURATIONS,
                new String[0]);
        return configurationManager.getConfiguration(
                CLOUD_SERVICE_NAME, services);
    }

}
