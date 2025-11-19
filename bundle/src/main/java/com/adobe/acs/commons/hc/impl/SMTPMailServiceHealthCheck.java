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
package com.adobe.acs.commons.hc.impl;

import com.adobe.acs.commons.email.impl.MailTemplateManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import com.adobe.acs.commons.util.RequireAem;
import com.day.cq.commons.mail.MailTemplate;
import com.day.cq.mailer.MessageGateway;
import com.day.cq.mailer.MessageGatewayService;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.SimpleEmail;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.hc.api.HealthCheck;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.util.FormattingResultLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.internet.InternetAddress;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component(configurationPolicy = ConfigurationPolicy.REQUIRE)
@SuppressWarnings("checkstyle:abbreviationaswordinname")
public class SMTPMailServiceHealthCheck implements HealthCheck {
    private static final Logger log = LoggerFactory.getLogger(SMTPMailServiceHealthCheck.class);

    // 10 seconds
    private static final int TIMEOUT = 1000 * 10;

    private static String MAIL_TEMPLATE =
            System.getProperty("line.separator")
                    + "Sling Health Check for AEM E-mail Service connectivity";

    private static final String DEFAULT_EMAIL = "healthcheck@example.com";
        private static final String PROP_EMAIL = "email";
    private String toEmail;

    private static final int DEFAULT_MAX_EMAILS_PER_DAY = 24;
        private static final String PROP_MAX_EMAILS_PER_DAY = "max.emails.per.day";
    private int maxEmailsPerDay = DEFAULT_MAX_EMAILS_PER_DAY;

    // Disable this feature on AEM as a Cloud Service
    @Reference(target = "(distribution=classic)")
    RequireAem requireAem;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY, policyOption = ReferencePolicyOption.GREEDY)
    private MessageGatewayService messageGatewayService;

    @Reference
    private MailTemplateManager mailTemplateManager;

    private transient ConcurrentHashMap<String, Integer> tracker = new ConcurrentHashMap<>();

    @Activate
    protected void activate(Map<String, Object> properties) {
        this.toEmail = PropertiesUtil.toString(properties.get(PROP_EMAIL), DEFAULT_EMAIL);
        this.maxEmailsPerDay = PropertiesUtil.toInteger(properties.get(PROP_MAX_EMAILS_PER_DAY), DEFAULT_MAX_EMAILS_PER_DAY);
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
            } else if (StringUtils.equals(this.toEmail, DEFAULT_EMAIL)) {
                resultLog.warn("The default e-mail address [ " + this.toEmail + "] is configured, which no one can verify (since you don't own @example.com). Please set this to a valid e-mail address is configured that can handle the rate of e-mails you will be sending.");
                log.warn("Default e-mail address [ {} ] used. Skipping sending e-mail as it cannot be verified on the other end. Please set a valid e-mail address via OSGi configuration, and ensure you performing this healthcheck at a reasonable rate so as not to flood your SMTP server or e-mail inbox.", DEFAULT_EMAIL);
            } else if (getEmailsSentToday() > this.maxEmailsPerDay) {
                resultLog.warn("Max quota for e-mails sent from this service per day [ " + this.maxEmailsPerDay + " ] has been met. Skipping sending further e-mail pings until tomorrow.");
                log.warn("Max quota for e-mails sent from this service per day [ {} ] has been met. Skipping sending further e-mail pings until tomorrow.", this.maxEmailsPerDay);
            } else {
                try {
                    List<InternetAddress> emailAddresses = new ArrayList<InternetAddress>();
                    emailAddresses.add(new InternetAddress(this.toEmail));
                    MailTemplate mailTemplate = new MailTemplate(IOUtils.toInputStream(MAIL_TEMPLATE, "UTF-8"), CharEncoding.UTF_8);
                    SimpleEmail email = mailTemplateManager.getEmail(mailTemplate, null, SimpleEmail.class);

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

    private synchronized int getEmailsSentToday() {
        final Calendar now = Calendar.getInstance();
        final String key = now.get(Calendar.YEAR) + "-" + now.get(Calendar.MONTH) + "-" + now.get(Calendar.DATE);

        if (tracker.containsKey(key)) {
            tracker.put(key, tracker.get(key) + 1);
        } else {
            tracker.clear();
            tracker.put(key, 1);
        }

        return tracker.get(key);
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