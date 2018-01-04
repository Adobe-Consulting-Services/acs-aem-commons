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
package com.adobe.acs.commons.social.linkedin;

import static org.junit.Assert.*;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;
import org.scribe.builder.ServiceBuilder;
import org.scribe.extractors.JsonTokenExtractor;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;
import org.scribe.utils.OAuthEncoder;

public class LinkedInApi20Test {

    @Test
    public void testNoStateDefined() {
        LinkedInApi20 api = new LinkedInApi20();
        String state = api.getState();
        assertEquals(10, state.length());
    }

    @Test
    public void testAuthorizationWithNoScopes() {
        String state = RandomStringUtils.randomAlphabetic(10);
        String key = RandomStringUtils.randomAlphabetic(10);
        String secret = RandomStringUtils.randomAlphabetic(10);
        LinkedInApi20 api = new LinkedInApi20(state);

        OAuthService service = new ServiceBuilder().provider(api).apiKey(key).apiSecret(secret)
                .callback("http://localhost:4502/linkedin").build();

        String expected = "https://www.linkedin.com/uas/oauth2/authorization?response_type=code&client_id=" + key
                + "&state=" + state + "&redirect_uri=" + OAuthEncoder.encode("http://localhost:4502/linkedin");

        assertEquals(expected, service.getAuthorizationUrl(null));
    }

    @Test
    public void testAuthorizationWithScopes() {
        String state = RandomStringUtils.randomAlphabetic(10);
        String key = RandomStringUtils.randomAlphabetic(10);
        String secret = RandomStringUtils.randomAlphabetic(10);
        LinkedInApi20 api = new LinkedInApi20(state);

        OAuthService service = new ServiceBuilder().provider(api).apiKey(key).apiSecret(secret)
                .callback("http://localhost:4502/linkedin").scope("r_basicprofile,r_emailaddress")
                .build();

        String expected = "https://www.linkedin.com/uas/oauth2/authorization?response_type=code&client_id=" + key
                + "&state=" + state + "&redirect_uri=" + OAuthEncoder.encode("http://localhost:4502/linkedin")
                + "&scope=" + "r_basicprofile%2Cr_emailaddress";

        assertEquals(expected, service.getAuthorizationUrl(null));
    }

    @Test
    public void testVerbIsCorrectType() {
        assertEquals(Verb.POST, new LinkedInApi20().getAccessTokenVerb());
    }

    @Test
    public void testExtractorIsCorrectType() {
        assertTrue(new LinkedInApi20().getAccessTokenExtractor() instanceof JsonTokenExtractor);
    }

}
