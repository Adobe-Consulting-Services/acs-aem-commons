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

import com.adobe.acs.commons.util.impl.SecureRandomStringUtils;
import org.scribe.builder.api.DefaultApi20;
import org.scribe.extractors.AccessTokenExtractor;
import org.scribe.extractors.JsonTokenExtractor;
import org.scribe.model.OAuthConfig;
import org.scribe.model.OAuthConstants;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuth20ServiceImpl;
import org.scribe.oauth.OAuthService;
import org.scribe.utils.OAuthEncoder;
import org.scribe.utils.Preconditions;

import aQute.bnd.annotation.ProviderType;

/**
 * Scribe API class for the LinkedIn OAuth 2.0 endpoint.
 *
 * Based on https://github.com/fernandezpablo85/scribe-java/pull/402, but heavily modified
 * so as not to require changes to Scribe and to be compatible with Scribe 1.3.0, shipped 
 * with CQ 5.6.x.
 */
@ProviderType
public final class LinkedInApi20 extends DefaultApi20 {
    private static final String ACCESS_TOKEN_URL = "https://www.linkedin.com/uas/oauth2/accessToken";

    private static final String AUTHORIZE_URL = "https://www.linkedin.com/uas/oauth2/authorization?response_type=code&client_id=%s&state=%s&redirect_uri=%s";

    private static final String SCOPED_AUTHORIZE_URL = AUTHORIZE_URL + "&scope=%s";

    private static final int DEFAULT_STATE_LENGTH = 10;

    private final String state;

    /**
     * Create an API instance with a random state and no scopes.
     */
    public LinkedInApi20() {
        this(SecureRandomStringUtils.randomAlphanumeric(DEFAULT_STATE_LENGTH));
    }

    /**
     * Create an API instance with the specified state and permissions.
     *
     * @param state the state to use for CSRF protection
     */
    public LinkedInApi20(String state) {
        this.state = state;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OAuthService createService(OAuthConfig config) {
        return new LinkedIn20Service(this, config);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAccessTokenEndpoint() {
        return ACCESS_TOKEN_URL;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AccessTokenExtractor getAccessTokenExtractor() {
        return new JsonTokenExtractor();
    }

    /**
     * {@inheritDoc}
     */
    public Verb getAccessTokenVerb() {
        return Verb.POST;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAuthorizationUrl(OAuthConfig config) {
        Preconditions.checkValidUrl(config.getCallback(),
                "Must provide a valid url as callback. LinkedIn does not support Out Of Band Auth.");
        // Append scope if present
        if (config.hasScope()) {
            return String.format(SCOPED_AUTHORIZE_URL, config.getApiKey(), OAuthEncoder.encode(state),
                    OAuthEncoder.encode(config.getCallback()), OAuthEncoder.encode(config.getScope()));
        } else {
            return String.format(AUTHORIZE_URL, config.getApiKey(), OAuthEncoder.encode(state),
                    OAuthEncoder.encode(config.getCallback()));
        }
    }
    
    /**
     * Obtain the state value configured for this API instance.
     * 
     * @return the configured state value
     */
    public String getState() {
        return state;
    }

    private static final class LinkedIn20Service extends OAuth20ServiceImpl {

        private LinkedInApi20 api;
        private OAuthConfig config;

        private LinkedIn20Service(LinkedInApi20 api, OAuthConfig config) {
            super(api, config);
            this.api = api;
            this.config = config;
        }

        /**
         * {@inheritDoc}
         * 
         * LinkedIn uses the query parameter 'oauth2_access_token' rather than 'access_token'.
         */
        @Override
        public void signRequest(Token accessToken, OAuthRequest request) {
            request.addQuerystringParameter("oauth2_access_token", accessToken.getToken());
        }

        /**
         * {@inheritDoc}
         * 
         * LinkedIn request the additional 'grant_type' parameter be set.
         */
        public Token getAccessToken(Token requestToken, Verifier verifier) {
            OAuthRequest request = new OAuthRequest(api.getAccessTokenVerb(), api.getAccessTokenEndpoint());
            request.addQuerystringParameter(OAuthConstants.CLIENT_ID, config.getApiKey());
            request.addQuerystringParameter(OAuthConstants.CLIENT_SECRET, config.getApiSecret());
            request.addQuerystringParameter(OAuthConstants.CODE, verifier.getValue());
            request.addQuerystringParameter(OAuthConstants.REDIRECT_URI, config.getCallback());
            request.addQuerystringParameter("grant_type", "authorization_code");

            if (config.hasScope()) {
                request.addQuerystringParameter(OAuthConstants.SCOPE, config.getScope());
            }
            Response response = request.send();
            return api.getAccessTokenExtractor().extract(response.getBody());
        }

    }

}
