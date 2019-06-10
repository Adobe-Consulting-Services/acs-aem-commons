/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2014 Adobe
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
package com.adobe.acs.commons.util.impl;

import org.junit.Before;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.osgi.framework.BundleException;
import org.osgi.service.component.ComponentContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BundleDisablerTest {

    @Mock
    private ComponentContext componentContext;

    @Mock
    private BundleContext bundleContext;

    @Mock
    private Bundle ownBundle;

    @InjectMocks
    private BundleDisabler disabler;

    private final List<Bundle> bundles = new ArrayList<Bundle>();

    @Test
    public void testNullProperties() {
        disabler.activate(componentContext, Collections.<String, Object>emptyMap());
        verifyNoMoreInteractions(bundleContext);
    }

    @Before
    public void setUp() {
        bundles.clear();
        when(componentContext.getBundleContext()).thenReturn(bundleContext);
        when(bundleContext.getBundles()).then(new Answer<Bundle[]>() {
            @Override
            public Bundle[] answer(final InvocationOnMock invocationOnMock) throws Throwable {
                return bundles.toArray(new Bundle[bundles.size()]);
            }
        });
    }

    @Test
    public void shouldNotDisableOwnBundle() {
        disabler.activate(componentContext, bundleProperties("my.own.bundle"));
    }

    @Test
    public void shouldStopBundle() {
        Bundle targetBundle = mock(Bundle.class);
        bundles.add(targetBundle);

        when(targetBundle.getSymbolicName()).thenReturn("to.stop.bundle");

        disabler.activate(componentContext, bundleProperties("to.stop.bundle"));

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

        disabler.activate(componentContext, bundleProperties("to.stop.bundle"));

        try {
            verify(targetBundle, never()).stop();
        } catch (BundleException be) {
            // stop throws exception but we are just verifying that the mock has been called.
        }
    }

    private Map<String, Object> bundleProperties(String... bundles) {
        return Collections.<String, Object>singletonMap("bundles", bundles);
    }

}
