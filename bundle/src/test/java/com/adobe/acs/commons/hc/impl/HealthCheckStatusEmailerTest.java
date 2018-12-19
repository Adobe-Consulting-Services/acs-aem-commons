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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.reflect.FieldUtils;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.api.execution.HealthCheckExecutionOptions;
import org.apache.sling.hc.api.execution.HealthCheckExecutionResult;
import org.apache.sling.hc.api.execution.HealthCheckExecutor;
import org.apache.sling.hc.util.HealthCheckMetadata;
import org.apache.sling.settings.SlingSettingsService;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.adobe.acs.commons.email.EmailService;
import com.adobe.acs.commons.email.impl.EmailServiceImpl;
import com.adobe.granite.license.ProductInfo;
import com.adobe.granite.license.ProductInfoService;

@RunWith(MockitoJUnitRunner.class)
public class HealthCheckStatusEmailerTest {
   
//   @Mock
//   HealthCheckStatusEmailer.Config config;

@Rule
public SlingContext context = new SlingContext();


Map<String,Object> configuration = new HashMap<>();

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

    List<HealthCheckExecutionResult> results;

    @Before
    public void setUp() throws Exception {
        //when(config.recipients_emailaddresses()).thenReturn(new String[] {"test@example.com"});
        configuration.put("recipients.emailaddresses", "test@example.com");
        configuration.put("email.sendonlyonfailure", Boolean.TRUE);
//        context.registerInjectActivateService(emailService);
        context.registerService(EmailService.class, emailService);
        context.registerService(HealthCheckExecutor.class, healthCheckExecutor);
        context.registerService(ProductInfoService.class, productInfoService);
        context.runMode("author");
//        context.registerInjectActivateService;

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

        when(productInfoService.getInfos()).thenReturn(new ProductInfo[]{mock(ProductInfo.class)});
        Set<String> runModes = new HashSet<String>();
        runModes.add("author");
        when(slingSettingsService.getRunModes()).thenReturn(runModes);
    }

    @Test
    public void run_WithoutFailuresDontSendEmail() throws Exception {
        results.add(successExecutionResult);
        
        //when(config.email_sendonlyonfailure()).thenReturn(true);

        context.registerInjectActivateService(healthCheckStatusEmailer, configuration);
        
        //healthCheckStatusEmailer.activate(config);

        healthCheckStatusEmailer.run();
        verifyZeroInteractions(emailService);
    }

    @Test
    public void run_WithoutFailuresSendEmail() throws Exception {
        results.add(successExecutionResult);

//        when(config.email_sendonlyonfailure()).thenReturn(false);
//        healthCheckStatusEmailer.activate(config);
        
        configuration.put("email.sendonlyonfailure", "false");
        context.registerInjectActivateService(healthCheckStatusEmailer, configuration);

        healthCheckStatusEmailer.run();
        verify(emailService, times(1)).sendEmail(any(String.class),
                any(Map.class), any(String[].class));
    }

    @Test
    public void run_WithFailuresSendEmail() throws Exception {
        results.add(failureExecutionResult);

//        when(config.email_sendonlyonfailure()).thenReturn(true);
//        healthCheckStatusEmailer.activate(config);
        context.registerInjectActivateService(healthCheckStatusEmailer, configuration);
        
        healthCheckStatusEmailer.run();
        verify(emailService, times(1)).sendEmail(any(String.class),
                any(Map.class), any(String[].class));
    }

    @Test
    public void run_WithFailuresSendEmail_2() throws Exception {
        results.add(failureExecutionResult);

//        when(config.email_sendonlyonfailure()).thenReturn(false);
//        healthCheckStatusEmailer.activate(config);
        configuration.put("email.sendonlyonfailure", "false");
        context.registerInjectActivateService(healthCheckStatusEmailer, configuration);

        healthCheckStatusEmailer.run();
        verify(emailService, times(1)).sendEmail(any(String.class),
                any(Map.class), any(String[].class));
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
    @Ignore
    public void throttledExecution() throws IllegalAccessException {
        results.add(failureExecutionResult);
        
        // Set a long delay to ensure we hit it on the 2nd .run() call..
        
        // TODO: This parameter is not getting applied properly, ignoring for now
        configuration.put("hc.timeout.override", 100000);
        context.registerInjectActivateService(healthCheckStatusEmailer, configuration);

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
                any(Map.class), any(String[].class));
    }
}

