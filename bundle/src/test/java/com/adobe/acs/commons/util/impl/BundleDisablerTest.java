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
package com.adobe.acs.commons.util.impl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.util.converter.Converters;

import com.adobe.acs.commons.util.impl.BundleDisabler.Config;

@RunWith(MockitoJUnitRunner.class)
public class BundleDisablerTest {

    @Mock
    private BundleContext bundleContext;

    @Mock
    private Bundle ownBundle;

    private final List<Bundle> bundles = new ArrayList<Bundle>();

    @Test
    public void testNullProperties() {
        Converters.standardConverter().convert(bundleContext).to(BundleDisabler.Config.class);
        new BundleDisabler(bundleContext, configuration(), Collections.emptyList() );
        verifyNoMoreInteractions(bundleContext);
    }

    @Before
    public void setUp() {
        bundles.clear();
        when(bundleContext.getBundles()).then(new Answer<Bundle[]>() {
            @Override
            public Bundle[] answer(final InvocationOnMock invocationOnMock) throws Throwable {
                return bundles.toArray(new Bundle[bundles.size()]);
            }
        });
    }

    @Test
    public void shouldNotDisableOwnBundle() {
        new BundleDisabler(bundleContext, configuration("my.own.bundle"), Collections.emptyList());
    }

    @Test
    public void shouldStopBundle() {
        Bundle targetBundle = mock(Bundle.class);
        bundles.add(targetBundle);

        when(targetBundle.getSymbolicName()).thenReturn("to.stop.bundle");

        new BundleDisabler(bundleContext, configuration("to.stop.bundle"), Collections.emptyList());

        try {
            verify(targetBundle).stop();
        } catch (BundleException be) {
            // stop throws exception but we are just verifying that the mock has been called.
        }
    }

    @Test
    public void shouldNotStopUninstalledBundle() {
        Bundle targetBundle = mock(Bundle.class);
        bundles.add(targetBundle);

        when(targetBundle.getState()).thenReturn(Bundle.UNINSTALLED);
        when(targetBundle.getSymbolicName()).thenReturn("to.stop.bundle");

        new BundleDisabler(bundleContext, configuration("to.stop.bundle"), Collections.emptyList());

        try {
            verify(targetBundle, never()).stop();
        } catch (BundleException be) {
            // stop throws exception but we are just verifying that the mock has been called.
        }
    }

    
    private @NotNull Config configuration(String... bundles) {
        Map<String, Object> propertiesMap = Collections.<String, Object>singletonMap("bundles", bundles);
        return Converters.standardConverter().convert(propertiesMap).to(BundleDisabler.Config.class);
    }

}
