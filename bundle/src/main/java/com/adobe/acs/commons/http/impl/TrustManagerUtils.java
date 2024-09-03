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

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.jetbrains.annotations.NotNull;

public class TrustManagerUtils {

    private TrustManagerUtils() {
        // no supposed to be instantiated
    }

    /**
     * 
     * @return a trust manager which accepts all certificates.
     */
    static @NotNull X509TrustManager createAlwaysTrusted() {
        return new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[]{};
            }

            @Override
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        };
    }

    /**
     * Creates a trust manager which combines the JRE default trust manager with the given one (the latter being checked first).
     * @return the composite trust manager
     * @throws KeyStoreException in case the JRE trust store cannot be read
     * @throws NoSuchAlgorithmException in case no JRE trust manager can be found with the default algorithm
     */
    static @NotNull X509TrustManager createComposition(@NotNull X509TrustManager trustManager) throws NoSuchAlgorithmException, KeyStoreException {
        X509TrustManager jreTrustManager = getJreTrustManager();
       return createCompositeTrustManager(jreTrustManager, trustManager);
    }

    private static @NotNull X509TrustManager getJreTrustManager() throws NoSuchAlgorithmException, KeyStoreException {
        return findDefaultTrustManager(null);
    }

    private static @NotNull X509TrustManager findDefaultTrustManager(KeyStore keyStore)
            throws NoSuchAlgorithmException, KeyStoreException {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore); // If keyStore is null, tmf will be initialized with the default trust store
        return (X509TrustManager) Arrays.stream(tmf.getTrustManagers()).filter(X509TrustManager.class::isInstance).findFirst().orElseThrow(() -> new IllegalStateException("The TrustManagerFactory does not expose a X509TrustManager"));
    }

    private static @NotNull X509TrustManager createCompositeTrustManager(@NotNull X509TrustManager jreTrustManager,
            X509TrustManager customTrustManager) {
        return new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                // If you're planning to use client-cert auth,
                // merge results from "defaultTm" and "myTm".
                return jreTrustManager.getAcceptedIssuers();
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                try {
                    customTrustManager.checkServerTrusted(chain, authType);
                } catch (CertificateException e) {
                    // This will throw another CertificateException if this fails too.
                    jreTrustManager.checkServerTrusted(chain, authType);
                }
            }

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                // If you're planning to use client-cert auth,
                // do the same as checking the server.
                jreTrustManager.checkClientTrusted(chain, authType);
            }

        };
    }

}
