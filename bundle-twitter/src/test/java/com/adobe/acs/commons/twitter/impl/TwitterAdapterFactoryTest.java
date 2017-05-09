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

import junitx.util.PrivateAccessor;
import org.junit.Test;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.Configuration;

import java.util.Collections;

import static org.junit.Assert.*;

public class TwitterAdapterFactoryTest {

    private TwitterAdapterFactory adapterFactory = new TwitterAdapterFactory();

    @Test
    public void activateWithoutProxy() throws Exception {
        adapterFactory.activate(Collections.emptyMap());;
        TwitterFactory factory = (TwitterFactory) PrivateAccessor.getField(adapterFactory, "factory");
        Twitter twitter = factory.getInstance();
        Configuration configuration = (Configuration) PrivateAccessor.getField(twitter, "conf");
        assertEquals(-1, configuration.getHttpProxyPort());
        assertNull(configuration.getHttpProxyHost());
    }

}