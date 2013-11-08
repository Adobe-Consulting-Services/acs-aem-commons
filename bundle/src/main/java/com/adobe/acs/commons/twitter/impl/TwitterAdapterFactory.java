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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
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

@Component
@Service
@Properties({
        @Property(name = AdapterFactory.ADAPTABLE_CLASSES, value = { "com.day.cq.wcm.api.Page",
                "com.day.cq.wcm.webservicesupport.Configuration" }),
        @Property(name = AdapterFactory.ADAPTER_CLASSES, value = { "twitter4j.Twitter",
                "com.adobe.acs.commons.twitter.TwitterClient" }) })
public class TwitterAdapterFactory implements AdapterFactory {

    private static final String CLOUD_SERVICE_NAME = "twitterconnect";

    private static final Logger log = LoggerFactory.getLogger(TwitterAdapterFactory.class);

    @Reference
    private ConfigurationManager configurationManager;

    private TwitterFactory factory;

    @SuppressWarnings("unchecked")
    @Override
    public <AdapterType> AdapterType getAdapter(Object adaptable, Class<AdapterType> type) {
        TwitterClient client = null;
        if (adaptable instanceof Page) {
            client = createTwitterClient((Page) adaptable);
        } else if (adaptable instanceof com.day.cq.wcm.webservicesupport.Configuration) {
            client = createTwitterClient((com.day.cq.wcm.webservicesupport.Configuration) adaptable);
        }

        if (type == TwitterClient.class) {
            return (AdapterType) client;
        } else if (type == Twitter.class) {
            return (AdapterType) client.getTwitter();
        } else {
            return null;
        }

    }

    private Configuration buildConfiguration() {
        final ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.setUseSSL(true);
        builder.setApplicationOnlyAuthEnabled(true);
        return builder.build();
    }

    private TwitterClient createTwitterClient(com.day.cq.wcm.webservicesupport.Configuration config) {
        Resource oauthConfig = config.getContentResource().listChildren().next();
        ValueMap oauthProps = ResourceUtil.getValueMap(oauthConfig);
        String consumerKey = oauthProps.get("oauth.client.id", String.class);
        String consumerSecret = oauthProps.get("oauth.client.secret", String.class);

        if (consumerKey != null && consumerSecret != null) {
            Twitter t = factory.getInstance();
            log.debug("Creating client for key {}.", consumerKey);
            t.setOAuthConsumer(consumerKey, consumerSecret);
            try {
                t.getOAuth2Token();
                return new TwitterClientImpl(t, config);
            } catch (TwitterException e) {
                log.error("Unable to create Twitter client.", e);
                return null;
            }
        } else {
            log.warn("Key or Secret missing for configuration {}", config.getPath());
        }

        return null;
    }

    private TwitterClient createTwitterClient(Page page) {
        com.day.cq.wcm.webservicesupport.Configuration config = findTwitterConfiguration(page);
        if (config != null) {
            return createTwitterClient(config);
        }
        return null;
    }

    private com.day.cq.wcm.webservicesupport.Configuration findTwitterConfiguration(Page page) {
        final HierarchyNodeInheritanceValueMap pageProperties = new HierarchyNodeInheritanceValueMap(
                page.getContentResource());
        final String[] services = pageProperties.getInherited(ConfigurationConstants.PN_CONFIGURATIONS,
                new String[0]);
        final com.day.cq.wcm.webservicesupport.Configuration cfg = configurationManager.getConfiguration(
                CLOUD_SERVICE_NAME, services);
        return cfg;
    }

    @Activate
    protected void activate() {
        this.factory = new TwitterFactory(buildConfiguration());
    }

}
