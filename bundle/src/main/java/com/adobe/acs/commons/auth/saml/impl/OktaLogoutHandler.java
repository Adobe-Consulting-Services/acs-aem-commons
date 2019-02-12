/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
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
package com.adobe.acs.commons.auth.saml.impl;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.auth.core.spi.AuthenticationHandler;
import org.apache.sling.auth.core.spi.AuthenticationInfo;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.Constants;

/**
 * When using the SAML 2.0 Template from Okta, traditional Single Log Out (SLO) support
 * is not available. Logout is instead supported through a special URL.
 * 
 * Details are here: https://community.okta.com/community/okta/blog/2014/01/31/logout-and-redirect-to-an-url
 */
@Component(label = "ACS AEM Commons - Okta Logout Handler",
        description = "Specific Authentication Handler to handle logout to Okta SSO Provider which, in some configurations, does not support traditional Single Logout",
        metatype = true, policy = ConfigurationPolicy.REQUIRE)
@Service
@Properties({
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "ACS AEM Commons Okta Logout Handler", propertyPrivate = true),
    @Property(name = Constants.SERVICE_RANKING, intValue = 5003, propertyPrivate = false),
    @Property(name = AuthenticationHandler.PATH_PROPERTY, value = "/", propertyPrivate = false)
})
public class OktaLogoutHandler implements AuthenticationHandler {

    @Property
    private static final String PROP_OKTA_HOST_NAME = "okta.host.name";

    @Property
    private static final String PROP_FROM_URI = "from.uri";

    private String redirectLocation;

    @Activate
    protected void activate(Map<String, Object> props) {
        String oktaHostName = PropertiesUtil.toString(props.get(PROP_OKTA_HOST_NAME), null);
        if (oktaHostName == null) {
            throw new IllegalArgumentException("Okta Host Name must be provided");
        }
        String fromUri = PropertiesUtil.toString(props.get(PROP_FROM_URI), null);
        StringBuilder builder = new StringBuilder("https://");
        builder.append(oktaHostName);
        builder.append("/login/signout");
        if (fromUri != null) {
            builder.append("?fromURI=").append(fromUri);
        }
        this.redirectLocation = builder.toString();
    }

    @Override
    public AuthenticationInfo extractCredentials(HttpServletRequest request, HttpServletResponse response) {
        return null;
    }

    @Override
    public boolean requestCredentials(HttpServletRequest request, HttpServletResponse response) throws IOException {
        return false;
    }

    @Override
    public void dropCredentials(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendRedirect(redirectLocation);
    }

}
