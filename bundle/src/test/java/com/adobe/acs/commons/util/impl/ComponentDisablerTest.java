/*
 * #%L
 * ACS AEM Tools Bundle
 * %%
 * Copyright (C) 2012 Adobe
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

import static org.mockito.Mockito.*;

import java.util.Collections;

import org.apache.felix.scr.Component;
import org.apache.felix.scr.ScrService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ComponentDisablerTest {

    @Mock
    private ScrService scrService;

    @InjectMocks
    private ComponentDisabler disabler;

    @Test
    public void testNullProperties() {
        disabler.activate(Collections.<String, Object> emptyMap());
        verifyNoMoreInteractions(scrService);
    }

    @Test
    public void testDisablingComponent() {
        // component with pid1
        Component component1 = mock(Component.class);
        when(component1.getState()).thenReturn(Component.STATE_ACTIVE);

        // components with pid2
        Component component2 = mock(Component.class);
        when(component1.getState()).thenReturn(Component.STATE_DISABLED);
        Component component3 = mock(Component.class);
        when(component1.getState()).thenReturn(Component.STATE_DISABLED);

        when(scrService.getComponents("pid1")).thenReturn(new Component[] { component1 });
        when(scrService.getComponents("pid2")).thenReturn(new Component[] { component2, component3 });
        when(scrService.getComponents("pid3")).thenReturn(null);

        disabler.activate(Collections.<String, Object> singletonMap("components",
                new String[] { "pid1", "pid2", "pid3" }));
        verify(scrService).getComponents("pid1");
        verify(scrService).getComponents("pid2");
        verify(scrService).getComponents("pid3");
        verify(component1).getState();
        verify(component2).getState();
        verify(component3).getState();

        verify(component2).getClassName();
        verify(component2).getConfigurationPid();

        verify(component3).getClassName();
        verify(component3).getConfigurationPid();

        verify(component2).disable();
        verify(component3).disable();
        verifyNoMoreInteractions(component1, component2, component3, scrService);
    }

}
