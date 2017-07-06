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
package com.adobe.acs.commons.hc.impl;

import com.adobe.acs.commons.email.EmailService;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.api.execution.HealthCheckExecutionOptions;
import org.apache.sling.hc.api.execution.HealthCheckExecutionResult;
import org.apache.sling.hc.api.execution.HealthCheckExecutor;
import org.apache.sling.hc.util.HealthCheckMetadata;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class HealthCheckStatusEmailerTest {

    @Mock
    HealthCheckExecutor healthCheckExecutor;

    @Mock
    HealthCheckExecutionResult successExecutionResult;

    @Mock
    HealthCheckExecutionResult failureExecutionResult;

    @Mock
    EmailService emailService;

    @InjectMocks
    HealthCheckStatusEmailer healthCheckStatusEmailer = new HealthCheckStatusEmailer();


    Map<String, Object> config;
    List<HealthCheckExecutionResult> results;

    @Before
    public void setUp() throws Exception {
        config = new HashMap<String, Object>();
        config.put(HealthCheckStatusEmailer.PROP_RECIPIENTS_EMAIL_ADDRESSES, "test@example.com");

        // Success
        HealthCheckMetadata successMetadata = mock(HealthCheckMetadata.class);
        Result successResult = mock(Result.class);
        when(successMetadata.getTitle()).thenReturn("hc success");
        when(successResult.isOk()).thenReturn(true);
        when(successResult.getStatus()).thenReturn(Result.Status.OK);
        when(successExecutionResult.getHealthCheckMetadata()).thenReturn(successMetadata);
        when(successExecutionResult.getHealthCheckResult()).thenReturn(successResult);

        // Failure
        HealthCheckMetadata failureMetadata = mock(HealthCheckMetadata.class);
        Result failureResult = mock(Result.class);
        when(failureMetadata.getTitle()).thenReturn("hc failure");
        when(failureResult.isOk()).thenReturn(false);
        when(failureResult.getStatus()).thenReturn(Result.Status.CRITICAL);
        when(failureExecutionResult.getHealthCheckMetadata()).thenReturn(failureMetadata);
        when(failureExecutionResult.getHealthCheckResult()).thenReturn(failureResult);

        results = new ArrayList<>();
        when(healthCheckExecutor.execute(any(HealthCheckExecutionOptions.class), any(String[].class))).thenReturn(results);
    }

    @Test
    public void run_WithoutFailuresDontSendEmail() throws Exception {
        results.add(successExecutionResult);

        config.put(HealthCheckStatusEmailer.PROP_SEND_EMAIL_ONLY_ON_FAILURE, true);
        healthCheckStatusEmailer.activate(config);

        healthCheckStatusEmailer.run();
        verifyZeroInteractions(emailService);
    }

    @Test
    public void run_WithoutFailuresSendEmail() throws Exception {
        results.add(successExecutionResult);

        config.put(HealthCheckStatusEmailer.PROP_SEND_EMAIL_ONLY_ON_FAILURE, false);
        healthCheckStatusEmailer.activate(config);

        healthCheckStatusEmailer.run();
        verify(emailService, times(1)).sendEmail(any(String.class),
                any(Map.class), any(String[].class));
    }

    @Test
    public void run_WithFailuresSendEmail() throws Exception {
        results.add(failureExecutionResult);

        config.put(HealthCheckStatusEmailer.PROP_SEND_EMAIL_ONLY_ON_FAILURE, true);
        healthCheckStatusEmailer.activate(config);

        healthCheckStatusEmailer.run();
        verify(emailService, times(1)).sendEmail(any(String.class),
                any(Map.class), any(String[].class));
    }

    @Test
    public void run_WithFailuresSendEmail_2() throws Exception {
        results.add(failureExecutionResult);

        config.put(HealthCheckStatusEmailer.PROP_SEND_EMAIL_ONLY_ON_FAILURE, false);
        healthCheckStatusEmailer.activate(config);

        healthCheckStatusEmailer.run();
        verify(emailService, times(1)).sendEmail(any(String.class),
                any(Map.class), any(String[].class));
    }

    @Test
    public void resultToPlainText() throws Exception {
        final List<HealthCheckExecutionResult> successResults = new ArrayList<>();
        successResults.add(successExecutionResult);
        final String actual = healthCheckStatusEmailer.resultToPlainText("HC Test", successResults);
        assertTrue(actual.contains("HC Test"));
        assertTrue(actual.contains("[ OK ]            hc success"));
    }
}