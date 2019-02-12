/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
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

import com.day.cq.commons.mail.MailTemplate;
import com.day.cq.mailer.MessageGateway;
import com.day.cq.mailer.MessageGatewayService;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang.text.StrLookup;
import org.apache.commons.mail.SimpleEmail;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.hc.api.HealthCheck;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.util.FormattingResultLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.internet.InternetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component(metatype = true,
        label = "ACS AEM Commons - Health Check - SMTP E-Mail Service",
        description = "Checks if the AEM E-Mail Service can connect and send mail via the configured SMTP server.",
        policy = ConfigurationPolicy.REQUIRE
)
@Properties({
        @Property(
                name = HealthCheck.NAME,
                value = "SMTP Mail Service",
                propertyPrivate = true),
        @Property(
                label = "Tags",
                name = HealthCheck.TAGS,
                unbounded = PropertyUnbounded.ARRAY,
                value = {"integrations", "smtp", "email"},
                description = "Tags for this check to be used by composite health checks."),
        @Property(
                name = HealthCheck.MBEAN_NAME,
                value = "smtpMailService",
                propertyPrivate = true)})
@Service
@SuppressWarnings("checkstyle:abbreviationaswordinname")
public class SMTPMailServiceHealthCheck implements HealthCheck {
    private static final Logger log = LoggerFactory.getLogger(SMTPMailServiceHealthCheck.class);

    // 10 seconds
    private static final int TIMEOUT = 1000 * 10;

    private static String MAIL_TEMPLATE =
            System.getProperty("line.separator")
                    + "Sling Health Check for AEM E-mail Service connectivity";

    private static final String DEFAULT_EMAIL = "healthcheck@example.com";
    @Property(
            label = "Test E-mail Address",
            description = "E-mail address to send test message to.",
            value = DEFAULT_EMAIL)
    private static final String PROP_EMAIL = "email";
    private String toEmail;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY)
    private MessageGatewayService messageGatewayService;

    @Activate
    protected void activate(Map<String, Object> properties) {
        this.toEmail = PropertiesUtil.toString(properties.get(PROP_EMAIL), DEFAULT_EMAIL);
    }

    @Override
    @SuppressWarnings("squid:S1141")
    public Result execute() {
        final FormattingResultLog resultLog = new FormattingResultLog();

        if (messageGatewayService == null) {
            resultLog.critical("MessageGatewayService OSGi service could not be found.");
            resultLog.info("Verify the Default Mail Service is active: http://<host>:<port>/system/console/components/com.day.cq.mailer.impl.CqMailingService");
        } else {
            final MessageGateway<SimpleEmail> messageGateway = messageGatewayService.getGateway(SimpleEmail.class);
            if (messageGateway == null) {
                resultLog.critical("The AEM Default Mail Service is INACTIVE, thus e-mails cannot be sent.");
                resultLog.info("Verify the Default Mail Service is active and configured: http://<host>:<port>/system/console/components/com.day.cq.mailer.DefaultMailService");
                log.warn("Could not retrieve a SimpleEmail Message Gateway");

            } else {
                try {
                    List<InternetAddress> emailAddresses = new ArrayList<InternetAddress>();
                    emailAddresses.add(new InternetAddress(this.toEmail));
                    MailTemplate mailTemplate = new MailTemplate(IOUtils.toInputStream(MAIL_TEMPLATE, "UTF-8"), CharEncoding.UTF_8);
                    SimpleEmail email = mailTemplate.getEmail(StrLookup.mapLookup(Collections.emptyMap()), SimpleEmail.class);

                    email.setSubject("AEM E-mail Service Health Check");
                    email.setTo(emailAddresses);

                    email.setSocketConnectionTimeout(TIMEOUT);
                    email.setSocketTimeout(TIMEOUT);
                    try {
                        messageGateway.send(email);
                        resultLog.info("The E-mail Service appears to be working properly. Verify the health check e-mail was sent to [ {} ]", this.toEmail);
                    } catch (Exception e) {
                        resultLog.critical("Failed sending e-mail. Unable to send a test toEmail via the configured E-mail server: " + e.getMessage(), e);
                        log.warn("Failed to send E-mail for E-mail Service health check", e);
                    }

                    logMailServiceConfig(resultLog, email);
                } catch (Exception e) {
                    resultLog.healthCheckError("Sling Health check could not formulate a test toEmail: " + e.getMessage(), e);
                    log.error("Unable to execute E-mail health check", e);
                }
            }
        }

        return new Result(resultLog);
    }

    private void logMailServiceConfig(FormattingResultLog resultLog, SimpleEmail email) {
        resultLog.info("SMTP Host: {}", email.getHostName());
        resultLog.info("SMTP use SSL: {}", email.isSSL());
        if (email.isSSL()) {
            resultLog.info("SMTP SSL Port: {}", email.getSslSmtpPort());
        } else {
            resultLog.info("SMTP Port: {}", email.getSmtpPort());
        }
        resultLog.info("SMTP From Address: {}", email.getFromAddress());
        resultLog.info("Socket Connection Timeout: {} seconds", email.getSocketConnectionTimeout() / 1000);
        resultLog.info("Socket IO Timeout: {} seconds", email.getSocketTimeout() / 1000);
    }
}