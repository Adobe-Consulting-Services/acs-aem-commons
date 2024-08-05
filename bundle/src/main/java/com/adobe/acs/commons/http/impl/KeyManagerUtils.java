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

import java.net.Socket;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.X509KeyManager;

import org.jetbrains.annotations.NotNull;

public class KeyManagerUtils {

    private KeyManagerUtils() {
        // no supposed to be instantiated
    }

    static @NotNull X509KeyManager createSingleClientSideCertificateKeyManager(@NotNull KeyStore keyStore, @NotNull char[] password, @NotNull String clientCertAlias) throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException {
        return new FixClientAliasX509KeyManagerWrapper(clientCertAlias, createKeyManager(keyStore, password));
    }

    private static @NotNull X509KeyManager createKeyManager(@NotNull KeyStore keyStore, @NotNull char[] password)
            throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException {
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, password);
        return (X509KeyManager) Arrays.stream(kmf.getKeyManagers()).filter(X509KeyManager.class::isInstance).findFirst().orElseThrow(() -> new IllegalStateException("The KeyManagerFactory does not expose a X509KeyManager"));
    }

    private static final class FixClientAliasX509KeyManagerWrapper implements X509KeyManager {
        private final String clientAlias;
        private final X509KeyManager delegate;
        
        FixClientAliasX509KeyManagerWrapper(String clientAlias, X509KeyManager delegate) {
            this.clientAlias = clientAlias;
            this.delegate = delegate;
        }
        
        @Override
        public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
            return clientAlias;
        }

        @Override
        public X509Certificate[] getCertificateChain(String alias) {
            return delegate.getCertificateChain(alias);
        }

        @Override
        public String[] getClientAliases(String s, Principal[] principals) {
            return delegate.getClientAliases(s, principals);
        }

        @Override
        public String[] getServerAliases(String s, Principal[] principals) {
            return delegate.getServerAliases(s, principals);
        }

        @Override
        public String chooseServerAlias(String s, Principal[] principals, Socket socket) {
                return delegate.chooseServerAlias(s, principals, socket);
        }

        @Override
        public PrivateKey getPrivateKey(String s) {
            return delegate.getPrivateKey(s);
        }
    }

}
