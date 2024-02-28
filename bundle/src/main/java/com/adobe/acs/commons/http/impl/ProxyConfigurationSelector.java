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

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.osgi.services.ProxyConfiguration;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.FieldOption;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

/**
 * Encapsulates all {@link ProxyConfiguration}s registered implicitly via OSGi configuration admin.
 * Implements logic similar to <a href="https://github.com/apache/httpcomponents-client/blob/54900db4653d7f207477e6ee40135b88e9bcf832/httpclient-osgi/src/main/java/org/apache/http/osgi/impl/OSGiHttpRoutePlanner.java">org.apache.http.osgi.impl.OSGiHttpRoutePlanner</a>.
 * to find a matching config.
 */
@Component(service = ProxyConfigurationSelector.class)
public class ProxyConfigurationSelector {

    @Reference(fieldOption = FieldOption.REPLACE, policyOption = ReferencePolicyOption.GREEDY, policy = ReferencePolicy.DYNAMIC)
    volatile List<ProxyConfiguration> configurations;

    List<ProxyConfiguration> findApplicableConfiguration(URI uri) {
        // similar logic as in https://github.com/apache/httpcomponents-client/blob/54900db4653d7f207477e6ee40135b88e9bcf832/httpclient-osgi/src/main/java/org/apache/http/osgi/impl/OSGiHttpRoutePlanner.java#L67
        // using first config, except if there is a config which excludes the host, then don't use any config at all
        List<ProxyConfiguration> configs = new LinkedList<>();
        for (final ProxyConfiguration proxyConfiguration : this.configurations) {
            if (proxyConfiguration.isEnabled()) {
                for (final String exception : proxyConfiguration.getProxyExceptions()) {
                    if (createMatcher(exception).matches(uri.getHost())) {
                        return Collections.emptyList();
                    }
                }
                configs.add(proxyConfiguration);
            }
        }
        return configs;
    }

    Optional<ProxyConfiguration> findConfigurationForProxy(String proxyHost, int proxyPort) {
        return configurations.stream().filter(c -> proxyHost.equals(c.getHostname()) && proxyPort == c.getPort()).findFirst();
    }

    private static final String DOT = ".";

    /**
     * The IP mask pattern against which hosts are matched.
     */
    public static final Pattern IP_MASK_PATTERN = Pattern.compile("^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                                                                  "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                                                                  "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                                                                  "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");

    private static HostMatcher createMatcher(final String name) {
        final NetworkAddress na = NetworkAddress.parse(name);
        if (na != null) {
            return new IPAddressMatcher(na);
        }

        if (name.startsWith(DOT)) {
            return new DomainNameMatcher(name);
        }

        return new HostNameMatcher(name);
    }

    private interface HostMatcher {

        boolean matches(String host);

    }

    private static class HostNameMatcher implements HostMatcher {

        private final String hostName;

        HostNameMatcher(final String hostName) {
            this.hostName = hostName;
        }

        @Override
        public boolean matches(final String host) {
            return hostName.equalsIgnoreCase(host);
        }
    }

    private static class DomainNameMatcher implements HostMatcher {

        private final String domainName;

        DomainNameMatcher(final String domainName) {
            this.domainName = domainName.toLowerCase(Locale.ROOT);
        }

        @Override
        public boolean matches(final String host) {
            return host.toLowerCase(Locale.ROOT).endsWith(domainName);
        }
    }

    private static class IPAddressMatcher implements HostMatcher {

        private final NetworkAddress address;

        IPAddressMatcher(final NetworkAddress address) {
            this.address = address;
        }

        @Override
        public boolean matches(final String host) {
            final NetworkAddress hostAddress = NetworkAddress.parse(host);
            return hostAddress != null && address.address == (hostAddress.address & address.mask);
        }

    }

    public static class NetworkAddress {

        final int address;

        final int mask;

        static NetworkAddress parse(final String adrSpec) {

            if (null != adrSpec) {
                final Matcher nameMatcher = IP_MASK_PATTERN.matcher(adrSpec);
                if (nameMatcher.matches()) {
                    try {
                        final int i1 = toInt(nameMatcher.group(1), 255);
                        final int i2 = toInt(nameMatcher.group(2), 255);
                        final int i3 = toInt(nameMatcher.group(3), 255);
                        final int i4 = toInt(nameMatcher.group(4), 255);
                        final int ip = i1 << 24 | i2 << 16 | i3 << 8 | i4;

                        int mask = toInt(nameMatcher.group(4), 32);
                        mask = (mask == 32) ? -1 : -1 - (-1 >>> mask);

                        return new NetworkAddress(ip, mask);
                    } catch (final NumberFormatException nfe) {
                        // not expected after the pattern match !
                    }
                }
            }

            return null;
        }

        private static int toInt(final String value, final int max) {
            if (value == null || value.isEmpty()) {
                return max;
            }

            int number = Integer.parseInt(value);
            if (number > max) {
                number = max;
            }
            return number;
        }

        public InetAddress getInetAddress() {
            byte[] byteAddress = new byte[] {
                (byte)(address >>> 24),
                (byte)(address >>> 16),
                (byte)(address >>> 8),
                (byte)address
            };
            try {
                return InetAddress.getByAddress(byteAddress);
            } catch (UnknownHostException e) {
                // this should never happen
                throw new IllegalArgumentException("Wrong length of address", e);
            }
        }
        
        NetworkAddress(final int address, final int mask) {
            this.address = address;
            this.mask = mask;
        }

    }
}
