/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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

import com.adobe.acs.commons.twitter.TwitterClient;
import com.adobe.cq.commerce.common.ValueMapDecorator;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.webservicesupport.ConfigurationConstants;
import com.day.cq.wcm.webservicesupport.ConfigurationManager;
import junitx.util.PrivateAccessor;
import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.testing.osgi.MockBundleContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.osgi.framework.BundleContext;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.Configuration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TwitterAdapterFactoryTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    public static final String VALID_OAUTH_CLIENT_ID = "abcd";
    public static final String VALID_OAUTH_SECRET = "efgh";
    private ValueMap validOauthValueMap;

    @Mock
    private BundleContext bundleContext;

    private TwitterAdapterFactory registeredFactory;

    @Test
    public void activateWithoutProxy() throws Exception {
        TwitterAdapterFactoryRegisterer registerer = new TwitterAdapterFactoryRegisterer();
        registerer.activate(bundleContext, Collections.emptyMap());
        TwitterAdapterFactory adapterFactory = registeredFactory;
        TwitterFactory factory = (TwitterFactory) PrivateAccessor.getField(adapterFactory, "factory");
        Twitter twitter = factory.getInstance();
        Configuration configuration = (Configuration) PrivateAccessor.getField(twitter, "conf");
        assertEquals(-1, configuration.getHttpProxyPort());
        assertNull(configuration.getHttpProxyHost());
    }

    @Test
    public void activateWithProxyPortWithoutHost() throws Exception {
        TwitterAdapterFactoryRegisterer registerer = new TwitterAdapterFactoryRegisterer();
        registerer.activate(bundleContext, Collections.singletonMap("http.proxy.port", 8080));
        TwitterAdapterFactory adapterFactory = registeredFactory;
        TwitterFactory factory = (TwitterFactory) PrivateAccessor.getField(adapterFactory, "factory");
        Twitter twitter = factory.getInstance();
        Configuration configuration = (Configuration) PrivateAccessor.getField(twitter, "conf");
        assertEquals(-1, configuration.getHttpProxyPort());
        assertNull(configuration.getHttpProxyHost());
    }

    @Test
    public void activateWithProxyHostWithoutPort() throws Exception {
        TwitterAdapterFactoryRegisterer registerer = new TwitterAdapterFactoryRegisterer();
        registerer.activate(bundleContext, Collections.singletonMap("http.proxy.host", "myproxy"));
        TwitterAdapterFactory adapterFactory = registeredFactory;
        TwitterFactory factory = (TwitterFactory) PrivateAccessor.getField(adapterFactory, "factory");
        Twitter twitter = factory.getInstance();
        Configuration configuration = (Configuration) PrivateAccessor.getField(twitter, "conf");
        assertEquals(-1, configuration.getHttpProxyPort());
        assertNull(configuration.getHttpProxyHost());
    }

    @Test
    public void activateWithProxy() throws Exception {
        TwitterAdapterFactoryRegisterer registerer = new TwitterAdapterFactoryRegisterer();
        Map<String, Object> config = new HashMap<>();
        config.put("http.proxy.host", "myproxy");
        config.put("http.proxy.port", 8080);
        registerer.activate(bundleContext, config);
        TwitterAdapterFactory adapterFactory = registeredFactory;
        TwitterFactory factory = (TwitterFactory) PrivateAccessor.getField(adapterFactory, "factory");
        Twitter twitter = factory.getInstance();
        Configuration configuration = (Configuration) PrivateAccessor.getField(twitter, "conf");
        assertEquals(8080, configuration.getHttpProxyPort());
        assertEquals("myproxy", configuration.getHttpProxyHost());
    }

    @Test
    public void testAdaptFromValidConfiguration() throws Exception {
        com.day.cq.wcm.webservicesupport.Configuration config = setupConfiguration(validOauthValueMap);

        Twitter twitter = mock(Twitter.class);
        TwitterAdapterFactory adapterFactory = new TwitterAdapterFactory() {
            @Override
            Twitter getInstance() {
                return twitter;
            }
        };

        TwitterClient client = adapterFactory.getAdapter(config, TwitterClient.class);
        assertNotNull(client);

        Twitter adapterTwitter = adapterFactory.getAdapter(config, Twitter.class);
        assertEquals(twitter, adapterTwitter);

        verify(twitter, times(2)).setOAuthConsumer(VALID_OAUTH_CLIENT_ID, VALID_OAUTH_SECRET);
        verify(twitter, times(2)).getOAuth2Token();
        verifyNoMoreInteractions(twitter);
    }

    @Test
    public void testAdaptFromInvalidConfiguration() throws Exception {
        com.day.cq.wcm.webservicesupport.Configuration config = setupConfiguration(ValueMap.EMPTY);

        TwitterAdapterFactory adapterFactory = new TwitterAdapterFactory();

        TwitterClient client = adapterFactory.getAdapter(config, TwitterClient.class);
        assertNull(client);
    }

    @Test
    public void testAdaptFromValidConfigurationUnableToCreateClient() throws Exception {
        com.day.cq.wcm.webservicesupport.Configuration config = setupConfiguration(validOauthValueMap);

        Twitter twitter = mock(Twitter.class);
        doThrow(new TwitterException("test")).when(twitter).getOAuth2Token();
        TwitterAdapterFactory adapterFactory = new TwitterAdapterFactory() {
            @Override
            Twitter getInstance() {
                return twitter;
            }
        };

        TwitterClient client = adapterFactory.getAdapter(config, TwitterClient.class);
        assertNull(client);

        verify(twitter, times(1)).setOAuthConsumer(VALID_OAUTH_CLIENT_ID, VALID_OAUTH_SECRET);
        verify(twitter, times(1)).getOAuth2Token();
        verifyNoMoreInteractions(twitter);
    }

    @Test
    public void testAdaptFromValidPage() throws Exception {
        Page page = setupPage(validOauthValueMap);

        Twitter twitter = mock(Twitter.class);
        TwitterAdapterFactory adapterFactory = new TwitterAdapterFactory() {
            @Override
            Twitter getInstance() {
                return twitter;
            }
        };

        TwitterClient client = adapterFactory.getAdapter(page, TwitterClient.class);
        assertNotNull(client);

        Twitter adapterTwitter = adapterFactory.getAdapter(page, Twitter.class);
        assertEquals(twitter, adapterTwitter);

        verify(twitter, times(2)).setOAuthConsumer(VALID_OAUTH_CLIENT_ID, VALID_OAUTH_SECRET);
        verify(twitter, times(2)).getOAuth2Token();
        verifyNoMoreInteractions(twitter);
    }

    private Page setupPage(ValueMap configData) {
        com.day.cq.wcm.webservicesupport.Configuration config = setupConfiguration(configData);
        String[] configPath = new String[] { "configpath "};
        Page page = mock(Page.class);
        Resource contentResource = mock(Resource.class);
        when(page.getContentResource()).thenReturn(contentResource);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        when(contentResource.getResourceResolver()).thenReturn(resourceResolver);
        ConfigurationManager configurationManager = mock(ConfigurationManager.class);
        when(resourceResolver.adaptTo(ConfigurationManager.class)).thenReturn(configurationManager);
        when(contentResource.getValueMap()).thenReturn(new ValueMapDecorator(Collections.singletonMap(ConfigurationConstants.PN_CONFIGURATIONS, configPath)));
        when(configurationManager.getConfiguration("twitterconnect", configPath)).thenReturn(config);
        return page;
    }


    @Before
    public void setup() {
        Map<String, Object> validOAuthConfig = new HashMap<>();
        validOAuthConfig.put("oauth.client.id", VALID_OAUTH_CLIENT_ID);
        validOAuthConfig.put("oauth.client.secret", VALID_OAUTH_SECRET);
        this.validOauthValueMap = new ValueMapDecorator(validOAuthConfig);

        when(bundleContext.registerService(eq(AdapterFactory.class), any(AdapterFactory.class), any())).thenAnswer(i -> {
            if (registeredFactory != null) {
                throw new IllegalArgumentException("TwitterAdapterFactory already registered");
            }
            registeredFactory = i.getArgument(1);
            return null;
        });
    }

    private com.day.cq.wcm.webservicesupport.Configuration setupConfiguration(ValueMap configData) {
        com.day.cq.wcm.webservicesupport.Configuration config = mock(com.day.cq.wcm.webservicesupport.Configuration.class);
        Resource contentResource = mock(Resource.class);
        Resource oauthResource = mock(Resource.class);
        when(config.getContentResource()).thenReturn(contentResource);
        when(contentResource.listChildren()).thenAnswer(i -> Collections.singleton(oauthResource).iterator());
        when(oauthResource.getValueMap()).thenReturn(configData);
        return config;
    }

}