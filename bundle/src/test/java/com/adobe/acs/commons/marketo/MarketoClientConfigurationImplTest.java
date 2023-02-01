/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */
package com.adobe.acs.commons.marketo;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.adobe.acs.commons.marketo.impl.MarketoClientConfigurationImpl;

@RunWith(Parameterized.class)
public class MarketoClientConfigurationImplTest {

    private static final String CLIENT_ID = "client123";
    private static final String CLIENT_SECRET = "secret456";
    private static final String MUNCHKIN_ID = "123-456-789";

    private static Object[] testData(String endpointHost, String serverInstance, String expectedEndpointHost,
            String expectedServerInstance) {
        return new Object[] {
                new MarketoClientConfigurationImpl(CLIENT_ID, CLIENT_SECRET, endpointHost, MUNCHKIN_ID, serverInstance),
                expectedEndpointHost, expectedServerInstance };
    }

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                testData("123-456-789.mktorest.com", "//app-abc123.marketo.com",
                        "123-456-789.mktorest.com", "app-abc123.marketo.com"),
                testData("https://123-456-789.mktorest.com", "https://app-abc123.marketo.com",
                        "123-456-789.mktorest.com", "app-abc123.marketo.com"),
                testData("http://123-456-789.mktorest.com", "http://app-abc123.marketo.com",
                        "123-456-789.mktorest.com", "app-abc123.marketo.com"),
                testData("http://123-456-789.mktorest.com:80/some/thing.jpg",
                        "http://app-abc123.marketo.com:440//some/thing.jpg",
                        "123-456-789.mktorest.com", "app-abc123.marketo.com")
        });
    }

    private final MarketoClientConfiguration config;
    private final String expectedEndpointHost;
    private final String expectedServerInstance;

    public MarketoClientConfigurationImplTest(MarketoClientConfiguration config, String expectedEndpointHost,
            String expectedServerInstance) {
        this.config = config;
        this.expectedEndpointHost = expectedEndpointHost;
        this.expectedServerInstance = expectedServerInstance;
    }

    @Test
    public void testSimpleProperties() {
        assertEquals(CLIENT_ID, config.getClientId());
        assertEquals(CLIENT_SECRET, config.getClientSecret());
        assertEquals(MUNCHKIN_ID, config.getMunchkinId());
    }

    @Test
    public void testEndpointHost() {
        assertEquals(expectedEndpointHost, config.getEndpointHost());
    }

    @Test
    public void testServerInstance() {
        assertEquals(expectedServerInstance, config.getServerInstance());
    }

}
