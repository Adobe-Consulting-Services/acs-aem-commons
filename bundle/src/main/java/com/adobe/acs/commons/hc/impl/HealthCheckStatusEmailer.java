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
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.hc.api.execution.HealthCheckExecutionOptions;
import org.apache.sling.hc.api.execution.HealthCheckExecutionResult;
import org.apache.sling.hc.api.execution.HealthCheckExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component(
        label = "ACS AEM Commons - Health Check Status E-mailer",
        description = "Scheduled Service that runs specified Health Checks and e-mails the results",
        configurationFactory = true,
        policy = ConfigurationPolicy.REQUIRE,
        metatype = true
)
@Properties({
        @Property(
                label = "Cron expression defining when this Scheduled Service will run",
                name = "scheduler.expression",
                description = "Every weekday @ 8am = [ 0 0 8 ? * MON-FRI * ] Visit www.cronmaker.com to generate cron expressions.",
                value = "0 0 8 ? * MON-FRI *"
        ),
        @Property(
                name = "scheduler.concurrent",
                boolValue = false,
                propertyPrivate = true
        ),
        @Property(
                name = "scheduler.runOn",
                value = "LEADER",
                propertyPrivate = true
        )
})
@Service(value = Runnable.class)
public class HealthCheckStatusEmailer implements Runnable {
    private final Logger log = LoggerFactory.getLogger(HealthCheckStatusEmailer.class);

    private static final int HEALTH_CHECK_STATUS_PADDING = 20;
    private static final int NUM_DASHES = 100;

    /* OSGi Properties */

    private static final String DEFAULT_EMAIL_TEMPLATE_PATH = "/etc/notification/email/acs-commons/health-check-status-email.txt";
    private String emailTemplatePath = DEFAULT_EMAIL_TEMPLATE_PATH;
    @Property(label = "E-mail Template Path",
            description = "The absolute JCR path to the e-mail template",
            value = DEFAULT_EMAIL_TEMPLATE_PATH)
    public static final String PROP_TEMPLATE_PATH = "email.template.path";


    private static final String DEFAULT_EMAIL_SUBJECT_PREFIX = "AEM Health Check report";
    private String emailSubject = DEFAULT_EMAIL_SUBJECT_PREFIX;
    @Property(label = "E-mail Subject Prefix",
            description = "The e-mail subject prefix. E-mail subject format is: <E-mail Subject Prefix> [ # Failures ] [ # Success ] [ <AEM Instance Name> ]",
            value = DEFAULT_EMAIL_SUBJECT_PREFIX)
    public static final String PROP_EMAIL_SUBJECT = "email.subject";

    private static final boolean DEFAULT_SEND_EMAIL_ONLY_ON_FAILURE = true;
    private boolean sendEmailOnlyOnFailure = DEFAULT_SEND_EMAIL_ONLY_ON_FAILURE;
    @Property(label = "Send e-mail only on failure",
            description = "If true, an e-mail is ONLY sent if at least 1 Health Check failure occurs. [ Default: true ]",
            boolValue = DEFAULT_SEND_EMAIL_ONLY_ON_FAILURE)
    public static final String PROP_SEND_EMAIL_ONLY_ON_FAILURE = "email.send-only-on-failure";

    private static final String DEFAULT_AEM_INSTANCE_NAME = "Unknown AEM Instance";
    private String aemInstanceName = DEFAULT_AEM_INSTANCE_NAME;
    @Property(label = "AEM Instance Name",
            description = "Identifies the AEM instance in the e-mail in the event this is used across many AEM instances.",
            value = DEFAULT_AEM_INSTANCE_NAME)
    public static final String PROP_AEM_INSTANCE_NAME = "aem-instance.name";

    private static final String[] DEFAULT_RECIPIENT_EMAIL_ADDRESSES = new String[]{};
    private String[] recipientEmailAddresses = DEFAULT_RECIPIENT_EMAIL_ADDRESSES;
    @Property(label = "Recipient E-mail Addresses",
            description = "A list of e-mail addresses to send this e-mail to.",
            cardinality = Integer.MAX_VALUE,
            value = {})
    public static final String PROP_RECIPIENTS_EMAIL_ADDRESSES = "email.recipients.email-addresses";

    private static final String[] DEFAULT_HEALTH_CHECK_TAGS = new String[]{"system"};
    private String[] healthCheckTags = DEFAULT_HEALTH_CHECK_TAGS;
    @Property(label = "Health Check Tags",
            description = "The AEM Health Check Tag names to execute. [ Default: system ]",
            cardinality = Integer.MAX_VALUE,
            value = {"system"})
    public static final String PROP_HEALTH_CHECK_TAGS = "hc.tags";

    private static final boolean DEFAULT_HEALTH_CHECK_TAGS_OPTIONS_OR = true;
    private boolean healthCheckTagsOptionsOr = DEFAULT_HEALTH_CHECK_TAGS_OPTIONS_OR;
    @Property(label = "'OR' Health Check Tags",
            description = "When set to true, all Health Checks that are in any of the Health Check Tags (hc.tags) are executed. If false, then the Health Check must be in ALL of the Health Check tags (hc.tags). [ Default: true ]",
            boolValue = DEFAULT_HEALTH_CHECK_TAGS_OPTIONS_OR)
    public static final String PROP_HEALTH_CHECK_TAGS_OPTIONS_OR = "hc.tags.options.or";

    @Property(
            name = "webconsole.configurationFactory.nameHint",
            value = "Health Check Status E-mailer for [ {aem-instance.name} ] using Health Check Tags [ {hc.tags} ]"
    )

    @Reference
    private EmailService emailService;

    @Reference
    private HealthCheckExecutor healthCheckExecutor;

    @Override
    public final void run() {
        log.trace("Executing ACS Commons Health Check E-mailer scheduled service");

        final List<HealthCheckExecutionResult> success = new ArrayList<>();
        final List<HealthCheckExecutionResult> failure = new ArrayList<>();

        final long start = System.currentTimeMillis();

        final HealthCheckExecutionOptions options = new HealthCheckExecutionOptions();
        options.setForceInstantExecution(true);
        options.setCombineTagsWithOr(healthCheckTagsOptionsOr);
        final List<HealthCheckExecutionResult> results = healthCheckExecutor.execute(options, healthCheckTags);

        log.debug("Obtained [ {} ] results for Health Check tags [ {} ]", results.size(), StringUtils.join(healthCheckTags, ", "));
        for (HealthCheckExecutionResult result : results) {
            if (result.getHealthCheckResult().isOk()) {
                success.add(result);
            } else {
                failure.add(result);
            }
        }

        final long timeTaken = System.currentTimeMillis() - start;
        log.info("Executed ACS Commons Health Check E-mailer scheduled service in [ {} ms ]", timeTaken);

        if (!sendEmailOnlyOnFailure || (sendEmailOnlyOnFailure && failure.size() > 0)) {
            sendEmail(success, failure, timeTaken);
        } else {
            log.debug("Declining to send e-mail notification of 100% successful Health Check execution due to configuration.");
        }
    }

    /**
     * Creates the e-mail template parameter map and invokes the OSGi E-Mail Service.
     *
     * @param success the list of successful Health Check Execution Results
     * @param failure the list of unsuccessful Health Check Execution Results
     * @param timeTaken the time taken to execute all Health Checks
     */
    protected final void sendEmail(final List<HealthCheckExecutionResult> success, final List<HealthCheckExecutionResult> failure, final long timeTaken) {
        final Map<String, String> emailParams = new HashMap<>();
        emailParams.put("subject", String.format("%s [ %d Failures ] [ %d Success ] [ %s ]", emailSubject, failure.size(), success.size(), aemInstanceName));
        emailParams.put("failure", resultToPlainText("Failing Health Checks", failure));
        emailParams.put("success", resultToPlainText("Successful Health Checks", success));
        emailParams.put("executedAt", Calendar.getInstance().getTime().toString());
        emailParams.put("serverName", aemInstanceName);
        emailParams.put("timeTaken", String.valueOf(timeTaken));

        final List<String> failureList = emailService.sendEmail(emailTemplatePath, emailParams, recipientEmailAddresses);

        if (failureList.size() > 0) {
            log.warn("Could not send health status check e-mails to recipients [ {} ]", StringUtils.join(failureList, ", "));
        }

        log.info("Successfully sent Health Check email to [ {} ] recipients", recipientEmailAddresses.length - failureList.size());
    }

    /**
     * Gererates the plain-text email sections for sets of Health Check Execution Results.
     *
     * @param title The section title
     * @param results the  Health Check Execution Results to render as plain text
     * @return the String for this section to be embedded in the e-mail
     */
    protected final String resultToPlainText(final String title, final List<HealthCheckExecutionResult> results) {
        final StringBuilder sb = new StringBuilder();

        sb.append(title);
        sb.append(System.lineSeparator());

        if (results.size() == 0) {
            sb.append("No " + StringUtils.lowerCase(title) + " could be found!");
            sb.append(System.lineSeparator());
        } else {
            sb.append(StringUtils.repeat("-", NUM_DASHES));
            sb.append(System.lineSeparator());

            for (final HealthCheckExecutionResult result : results) {
                sb.append(StringUtils.rightPad("[ " + result.getHealthCheckResult().getStatus().name() + " ]", HEALTH_CHECK_STATUS_PADDING));
                sb.append("  ");
                sb.append(result.getHealthCheckMetadata().getTitle());
                sb.append(System.lineSeparator());
            }
        }

        return sb.toString();
    }


    /**
     * OSGi Activate method.
     *
     * @param config the OSGi config params
     */
    @Activate
    protected final void activate(final Map<String, Object> config) {
        emailTemplatePath = PropertiesUtil.toString(config.get(PROP_TEMPLATE_PATH), DEFAULT_EMAIL_TEMPLATE_PATH);
        emailSubject = PropertiesUtil.toString(config.get(PROP_EMAIL_SUBJECT), DEFAULT_EMAIL_SUBJECT_PREFIX);
        aemInstanceName = PropertiesUtil.toString(config.get(PROP_AEM_INSTANCE_NAME), DEFAULT_AEM_INSTANCE_NAME);
        recipientEmailAddresses = PropertiesUtil.toStringArray(config.get(PROP_RECIPIENTS_EMAIL_ADDRESSES), DEFAULT_RECIPIENT_EMAIL_ADDRESSES);
        healthCheckTags = PropertiesUtil.toStringArray(config.get(PROP_HEALTH_CHECK_TAGS), DEFAULT_HEALTH_CHECK_TAGS);
        healthCheckTagsOptionsOr = PropertiesUtil.toBoolean(config.get(PROP_HEALTH_CHECK_TAGS_OPTIONS_OR), DEFAULT_HEALTH_CHECK_TAGS_OPTIONS_OR);
        sendEmailOnlyOnFailure = PropertiesUtil.toBoolean(config.get(PROP_SEND_EMAIL_ONLY_ON_FAILURE), DEFAULT_SEND_EMAIL_ONLY_ON_FAILURE);
    }
}
