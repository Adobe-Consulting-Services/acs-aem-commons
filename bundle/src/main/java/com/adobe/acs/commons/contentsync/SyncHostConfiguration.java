/*-
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2022 Adobe
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
package com.adobe.acs.commons.contentsync;

import com.adobe.granite.crypto.CryptoException;
import com.adobe.granite.crypto.CryptoSupport;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * Adapts to configuration nodes in /var/acs-commons/contentsync/hosts/*
 */
@Model(adaptables = {Resource.class}, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class SyncHostConfiguration {
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @ValueMapValue(injectionStrategy = InjectionStrategy.REQUIRED)
    private String host;

    @ValueMapValue
    private String username;

    @ValueMapValue
    private String password;

    @ValueMapValue
    private String authType;

    @ValueMapValue
    private String agentId;

    @ValueMapValue
    private String accessTokenProviderName;

    @OSGiService
    private CryptoSupport crypto;

    @Self
    private Resource resource;

    public String getHost() {
        return host;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        if(crypto.isProtected(password)){
            try {
                password = crypto.unprotect(password);
            } catch (CryptoException e) {
                log.error("Error while un-protecting password: {}", password, e);
            }
        }
        return password;
    }

    public boolean isOAuthEnabled(){
        return "oauth".equals(authType);
    }

    public String getAccessTokenProviderName(){
        return accessTokenProviderName;
    }

    public String getAgentUserId(){
        return agentId == null ? resource.getResourceResolver().getUserID() : agentId;
    }
}
