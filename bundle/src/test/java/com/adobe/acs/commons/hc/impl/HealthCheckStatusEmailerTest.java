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
import com.adobe.granite.license.ProductInfo;
import com.adobe.granite.license.ProductInfoService;
import org.apache.commons.lang.reflect.FieldUtils;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.api.execution.HealthCheckExecutionOptions;
import org.apache.sling.hc.api.execution.HealthCheckExecutionResult;
import org.apache.sling.hc.api.execution.HealthCheckExecutor;
import org.apache.sling.hc.util.HealthCheckMetadata;
import org.apache.sling.settings.SlingSettingsService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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

    @Mock
    ProductInfoService productInfoService;

    @Mock
    SlingSettingsService slingSettingsService;

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
        when(healthCheckExecutor.execute(any(HealthCheckExecutionOptions.class), ArgumentMatchers.<String>any())).thenReturn(results);

        when(productInfoService.getInfos()).thenReturn(new ProductInfo[]{mock(ProductInfo.class)});
        Set<String> runModes = new HashSet<String>();
        runModes.add("author");
        when(slingSettingsService.getRunModes()).thenReturn(runModes);
    }

    @Test
    public void run_WithoutFailuresDontSendEmail() throws Exception {
        results.add(successExecutionResult);

        config.put(HealthCheckStatusEmailer.PROP_SEND_EMAIL_ONLY_ON_FAILURE, true);
        healthCheckStatusEmailer.activate(config);

        healthCheckStatusEmailer.run();
        verifyNoInteractions(emailService);
    }

    @Test
    public void run_WithoutFailuresSendEmail() throws Exception {
        results.add(successExecutionResult);

        config.put(HealthCheckStatusEmailer.PROP_SEND_EMAIL_ONLY_ON_FAILURE, false);
        healthCheckStatusEmailer.activate(config);

        healthCheckStatusEmailer.run();
        verify(emailService, times(1)).sendEmail(any(String.class),
                any(Map.class), ArgumentMatchers.<String>any());
    }

    @Test
    public void run_WithFailuresSendEmail() throws Exception {
        results.add(failureExecutionResult);

        config.put(HealthCheckStatusEmailer.PROP_SEND_EMAIL_ONLY_ON_FAILURE, true);
        healthCheckStatusEmailer.activate(config);

        healthCheckStatusEmailer.run();
        verify(emailService, times(1)).sendEmail(any(String.class),
                any(Map.class), ArgumentMatchers.<String>any());
    }

    @Test
    public void run_WithFailuresSendEmail_2() throws Exception {
        results.add(failureExecutionResult);

        config.put(HealthCheckStatusEmailer.PROP_SEND_EMAIL_ONLY_ON_FAILURE, false);
        healthCheckStatusEmailer.activate(config);

        healthCheckStatusEmailer.run();
        verify(emailService, times(1)).sendEmail(any(String.class),
                any(Map.class), ArgumentMatchers.<String>any());
    }

    @Test
    public void resultToPlainText() throws Exception {
        final List<HealthCheckExecutionResult> successResults = new ArrayList<>();
        successResults.add(successExecutionResult);
        final String actual = healthCheckStatusEmailer.resultToPlainText("HC Test", successResults);

        Matcher titleMatcher = Pattern.compile("^HC Test$", Pattern.MULTILINE).matcher(actual);
        Matcher entryMatcher = Pattern.compile("^\\[ OK \\]\\s+hc success$", Pattern.MULTILINE).matcher(actual);
        Matcher negativeMatcher = Pattern.compile("^\\[ CRTICAL \\]\\s+hc failure", Pattern.MULTILINE).matcher(actual);

        assertTrue(titleMatcher.find());
        assertTrue(entryMatcher.find());
        assertFalse(negativeMatcher.find());
    }

    @Test
    public void throttledExecution() throws IllegalAccessException {
        results.add(failureExecutionResult);

        config.put(HealthCheckStatusEmailer.PROP_SEND_EMAIL_ONLY_ON_FAILURE, true);
        // Set a long delay to ensure we hit it on the 2nd .run() call..
        config.put(HealthCheckStatusEmailer.PROP_HEALTH_CHECK_TIMEOUT_OVERRIDE, 100000);
        healthCheckStatusEmailer.activate(config);

        Calendar minuteAgo = Calendar.getInstance();
        // Make sure enough time has "ellapsed" so that the call to send email does something
        minuteAgo.add(Calendar.MINUTE, -1);
        minuteAgo.add(Calendar.SECOND, -1);
        FieldUtils.writeField(healthCheckStatusEmailer, "nextEmailTime",  minuteAgo, true);

        // Send the first time
        healthCheckStatusEmailer.run();
        // Get throttled the 2nd time
        healthCheckStatusEmailer.run();

        verify(emailService, times(1)).sendEmail(any(String.class),
                any(Map.class), ArgumentMatchers.<String>any());
    }
}

