/*
 * ACS AEM Commons
 *
 * Copyright (C) 2024 Konrad Windszus
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
package com.adobe.acs.commons.http.impl;

import java.io.IOException;
import java.security.KeyStore;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.net.ssl.X509TrustManager;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingIOException;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.serviceusermapping.ServiceUserMapped;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.adobe.granite.crypto.CryptoException;
import com.adobe.granite.crypto.CryptoSupport;
import com.adobe.granite.keystore.KeyStoreService;

@Component(service=AemKeyStoreFactory.class)
public class AemKeyStoreFactory {

    private static final String SUB_SERVICE_NAME = "key-store-factory";

    private static final Map<String, Object> SERVICE_USER = Map.of(ResourceResolverFactory.SUBSERVICE,
            SUB_SERVICE_NAME);

    /** Defer starting the service until service user mapping is available. */
    @Reference(target = "(|(" + ServiceUserMapped.SUBSERVICENAME + "=" + SUB_SERVICE_NAME + ")(!("
            + ServiceUserMapped.SUBSERVICENAME + "=*)))")
    private ServiceUserMapped serviceUserMapped;

    private final ResourceResolverFactory resolverFactory;
    private final KeyStoreService keyStoreService;
    private final CryptoSupport cryptoSupport;

    @Activate()
    public AemKeyStoreFactory(@Reference ResourceResolverFactory resolverFactory,
            @Reference KeyStoreService keyStoreService,
            @Reference CryptoSupport cryptoSupport) {
        this.resolverFactory = resolverFactory;
        this.keyStoreService = keyStoreService;
        this.cryptoSupport = cryptoSupport;
    }

    /** @return the global AEM trust store
     * @throws LoginException
     * @see <a href=
     *      "https://experienceleague.adobe.com/docs/experience-manager-learn/foundation/security/call-internal-apis-having-private-certificate.html?lang=en">Call
     *      internal APIs having private certificates</a> */
    public @NotNull X509TrustManager getTrustManager() throws LoginException {
        try (final var serviceResolver = getKeyStoreResourceResolver()) {
            return (X509TrustManager) keyStoreService.getTrustManager(serviceResolver);
        }
    }

    /** @return the global AEM trust store
     * @throws LoginException
     * @see <a href=
     *      "https://experienceleague.adobe.com/docs/experience-manager-learn/foundation/security/call-internal-apis-having-private-certificate.html?lang=en">Call
     *      internal APIs having private certificates</a> */
    public @NotNull KeyStore getTrustStore() throws LoginException {
        try (final var serviceResolver = getKeyStoreResourceResolver()) {
            var aemTrustStore = keyStoreService.getTrustStore(serviceResolver);
            return aemTrustStore;
        }
    }

    public @NotNull KeyStore getKeyStore(@NotNull final String userId) throws LoginException {
        try (final var serviceResolver = getKeyStoreResourceResolver()) {
            // using the password set for the user Id's keystore to decrypt the entry
            return keyStoreService.getKeyStore(serviceResolver, userId);
        }
    }

    public @NotNull char[] getKeyStorePassword(@NotNull final String userId) throws LoginException {
        try (final var serviceResolver = getKeyStoreResourceResolver()) {
            User user = retrieveUser(serviceResolver, userId);
            String path = getKeyStorePathForUser(user, "store.p12");
            return extractStorePassword(serviceResolver, path, cryptoSupport);
        }
    }

    private @NotNull ResourceResolver getKeyStoreResourceResolver() throws LoginException {
        return this.resolverFactory.getServiceResourceResolver(SERVICE_USER);
    }

    // the following methods are extracted from com.adobe.granite.keystore.internal.KeyStoreServiceImpl, because there is no public method
    // for retrieving the keystore's password
    private static User retrieveUser(ResourceResolver resolver, String userId)
            throws IllegalArgumentException, SlingIOException {
        UserManager userManager = (UserManager) resolver.adaptTo(UserManager.class);
        if (userManager != null) {
            Authorizable authorizable;
            try {
                authorizable = userManager.getAuthorizable(userId);
            } catch (RepositoryException var6) {
                throw new SlingIOException(new IOException(var6));
            }

            if (authorizable != null && !authorizable.isGroup()) {
                User user = (User) authorizable;
                return user;
            } else {
                throw new IllegalArgumentException("The provided userId does not identify an existing user.");
            }
        } else {
            throw new IllegalArgumentException("Cannot obtain a UserManager for the given resource resolver.");
        }
    }

    private static String getKeyStorePathForUser(User user, String keyStoreFileName) throws SlingIOException {
        String userHome;
        try {
            userHome = user.getPath();
        } catch (RepositoryException var4) {
            throw new SlingIOException(new IOException(var4));
        }
        return userHome + "/" + "keystore" + "/" + keyStoreFileName;
    }

    private static char[] extractStorePassword(ResourceResolver resolver, String storePath, CryptoSupport cryptoSupport)
            throws SecurityException {
        Resource storeResource = resolver.getResource(storePath);
        if (storeResource != null) {
            Node storeParentNode = (Node) storeResource.getParent().adaptTo(Node.class);

            try {
                Property passwordProperty = storeParentNode.getProperty("keystorePassword");
                if (passwordProperty != null) {
                    return cryptoSupport.unprotect(passwordProperty.getString()).toCharArray();
                } else {
                    throw new SecurityException(
                            "Missing 'keystorePassword' property on " + ResourceUtil.getParent(storePath));
                }
            } catch (RepositoryException var6) {
                throw new SecurityException(var6);
            } catch (CryptoException var7) {
                throw new SecurityException(var7);
            }
        } else {
            return null;
        }
    }

}
