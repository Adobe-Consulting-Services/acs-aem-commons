/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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
package com.adobe.acs.commons.fam;

import com.adobe.acs.commons.fam.actions.Actions;
import com.adobe.acs.commons.functions.CheckedBiConsumer;
import com.adobe.acs.commons.functions.CheckedConsumer;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Test Actions retry methods
 */
public class ActionsTest {

    @Test
    public void thirdTimesTheCharm() throws Exception {
        ResourceResolver rr = mock(ResourceResolver.class);
        CheckedConsumer retry = mock(CheckedConsumer.class);
        doThrow(new NullPointerException())
                .doThrow(new NullPointerException())
                .doNothing()
                .when(retry).accept(any());
        Actions.retry(3, 1, retry).accept(rr);
        verify(retry, times(3)).accept(any());
    }

    @Test
    public void retryThrowsError() throws Exception {
        final ResourceResolver rr = mock(ResourceResolver.class);
        final CheckedConsumer retry = mock(CheckedConsumer.class);
        try {
            doThrow(Exception.class)
                    .doThrow(Exception.class)
                    .doThrow(Exception.class)
                    .doThrow(Exception.class)
                    .doThrow(Exception.class)
                    .doThrow(Exception.class)
                    .doThrow(Exception.class)
                    .doThrow(Exception.class)
                    .doThrow(Exception.class)
                    .doThrow(Exception.class)
                    .when(retry).accept(rr);
            Actions.retry(10, 1, retry).accept(rr);
            fail("Should have thrown an exception!");
        } catch (Throwable ex) {
            // Success case
            verify(retry, times(10)).accept(rr);
        }
    }
    
    @Test
    public void testRetryAll() throws Exception {
        ResourceResolver rr = mock(ResourceResolver.class);
        CheckedBiConsumer retry = mock(CheckedBiConsumer.class);
        doThrow(new NullPointerException())
                .doThrow(new NullPointerException())
                .doNothing()
                .when(retry).accept(any(), any());
        Actions.retryAll(3, 1, retry).accept(rr, "test");
        verify(retry, times(3)).accept(any(), any());
    }
}