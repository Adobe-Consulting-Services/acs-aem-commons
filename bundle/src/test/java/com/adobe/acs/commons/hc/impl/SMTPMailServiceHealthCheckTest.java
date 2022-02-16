/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2020 Adobe
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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;

import com.adobe.acs.commons.util.RequireAem;
import com.adobe.acs.commons.util.impl.RequireAemImpl;
import com.day.cq.mailer.MessageGateway;
import com.day.cq.mailer.MessageGatewayService;

import org.apache.commons.mail.SimpleEmail;
import org.apache.sling.hc.api.HealthCheck;
import org.apache.sling.hc.api.Result;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.wcm.testing.mock.aem.junit.AemContext;

@RunWith(MockitoJUnitRunner.class)
public class SMTPMailServiceHealthCheckTest {
    @Mock
    private MessageGatewayService messageGatewayService;

    @Mock
    private MessageGateway<SimpleEmail> messageGateway;

    @Rule
    public AemContext ctx = new AemContext();

    @Before
    public void setUp() {
        ctx.registerService(RequireAem.class, new RequireAemImpl(), "distribution", "classic");
        ctx.registerService(MessageGatewayService.class, messageGatewayService);
    }

    @Test
    public void testExecute_MissingMessageGateway() throws Exception {
        ctx.registerInjectActivateService(new SMTPMailServiceHealthCheck());

        HealthCheck healthCheck = ctx.getServices(HealthCheck.class,
                "(" + HealthCheck.NAME + "=SMTP Mail Service)")[0];

        Result actual = healthCheck.execute();

        assertEquals(Result.Status.CRITICAL, actual.getStatus());
    }

    @Test
    public void testExecute_DefaultEmail() throws Exception {
        doReturn(messageGateway).when(messageGatewayService).getGateway(SimpleEmail.class);

        ctx.registerInjectActivateService(new SMTPMailServiceHealthCheck());

        HealthCheck healthCheck = ctx.getServices(HealthCheck.class, "(" + HealthCheck.NAME + "=SMTP Mail Service)")[0];

        Result actual = healthCheck.execute();

        assertEquals(Result.Status.WARN, actual.getStatus());
    }

    @Test
    public void testExecute_ExceedDailyAllowance() throws Exception {
        doReturn(messageGateway).when(messageGatewayService).getGateway(SimpleEmail.class);

        ctx.registerInjectActivateService(new SMTPMailServiceHealthCheck(),
                "email", "ira@dog.com",
                "max.emails.per.day", "1");

        HealthCheck healthCheck = ctx.getServices(HealthCheck.class,
                "(" + HealthCheck.NAME + "=SMTP Mail Service)")[0];

        Result actual = healthCheck.execute();

        assertEquals(Result.Status.OK, actual.getStatus());

        actual = healthCheck.execute();

        assertEquals(Result.Status.WARN, actual.getStatus());
    }
}
