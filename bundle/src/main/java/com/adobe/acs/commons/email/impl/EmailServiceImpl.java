/*
* #%L
* ACS AEM Commons Bundle
* %%
* Copyright (C) 2013 Adobe
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
package com.adobe.acs.commons.email.impl;

import com.adobe.acs.commons.email.EmailService;
import com.adobe.acs.commons.email.EmailServiceConstants;
import com.day.cq.commons.mail.MailTemplate;
import com.day.cq.mailer.MessageGateway;
import com.day.cq.mailer.MessageGatewayService;
import org.apache.commons.lang.text.StrLookup;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.SimpleEmail;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.DataSource;
import javax.jcr.Session;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * ACS AEM Commons - E-mail Service
 * A Generic Email service that sends an email to a given list of recipients.
 *
 * The mailType is set to HTMLEmail by default if there are any attachments. Since we are using
 * the template to determine the mailType, the template name has to be *.html.
 *
 * Here is an example to send an email with attachments:
 *
 *      String attachment1 = "This text should be in the attache txt file."
 *      Map<String, DataSource> attachments = new HashMap<>();
 *      attachments.put("attachment1.txt", new ByteArrayDataSource(attachment1, "text/plain"));
 *      ...
 *      ...
 *      List<String> participantList = emailService.sendEmail(htmlEmailTemplatePath, emailParams, attachments, key);
 */
@Component(configurationPolicy=ConfigurationPolicy.REQUIRE)
@Designate(ocd=EmailServiceImpl.Config.class)
public final class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);
    private static final String MSG_INVALID_RECIPIENTS = "Invalid Recipients";
    
    @ObjectClassDefinition(name="ACS AEM COmmons - EMail Service", description="ACS AEM Commons - Email Service")
    public @interface Config {
        @AttributeDefinition(name="Connect Timeout", description="Connection timeout in miliseconds")
        int conn_timeout() default DEFAULT_CONNECT_TIMEOUT;
        
        @AttributeDefinition(name="Socket Timeout", description="Socket timeout in miliseconds")
        int so_timeout() default DEFAULT_SOCKET_TIMEOUT;
    }
    

    @Reference
    private MessageGatewayService messageGatewayService;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    public static final int DEFAULT_CONNECT_TIMEOUT = 30000;

    public static final int DEFAULT_SOCKET_TIMEOUT = 30000;


    private static String SERVICE_NAME = "email-service";

    private int connectTimeout;
    private int soTimeout;

    @Activate
    protected void activate(Config conf) {
        connectTimeout = conf.conn_timeout();
        soTimeout = conf.so_timeout();
    }

    @Override
    public List<String> sendEmail(final String templatePath,
                                  final Map<String, String> emailParams,
                                  final String... recipients) {

        List<String> failureList = new ArrayList<String>();

        if (recipients == null || recipients.length <= 0) {
            throw new IllegalArgumentException(MSG_INVALID_RECIPIENTS);
        }

        List<InternetAddress> addresses = new ArrayList<InternetAddress>(recipients.length);
        for (String recipient : recipients) {
            try {
                addresses.add(new InternetAddress(recipient));
            } catch (AddressException e) {
                log.warn("Invalid email address {} passed to sendEmail(). Skipping.", recipient);
            }
        }
        InternetAddress[] iAddressRecipients = addresses.toArray(new InternetAddress[addresses.size()]);
        List<InternetAddress> failureInternetAddresses = sendEmail(templatePath, emailParams, iAddressRecipients);

        for (InternetAddress address : failureInternetAddresses) {
            failureList.add(address.toString());
        }

        return failureList;
    }


    @Override
    public List<InternetAddress> sendEmail(final String templatePath, final Map<String, String> emailParams,
                                           final InternetAddress... recipients) {

        List<InternetAddress> failureList = new ArrayList<InternetAddress>();

        if (recipients == null || recipients.length <= 0) {
            throw new IllegalArgumentException(MSG_INVALID_RECIPIENTS);
        }

        final MailTemplate mailTemplate = this.getMailTemplate(templatePath);
        final Class<? extends Email> mailType = this.getMailType(templatePath);
        final MessageGateway<Email> messageGateway = messageGatewayService.getGateway(mailType);

        for (final InternetAddress address : recipients) {
            try {
                // Get a new email per recipient to avoid duplicate attachments
                final Email email = getEmail(mailTemplate, mailType, emailParams);
                email.setTo(Collections.singleton(address));
                messageGateway.send(email);
            } catch (Exception e) {
                failureList.add(address);
                log.error("Error sending email to [ " + address + " ]", e);
            }
        }

        return failureList;
    }

    @Override
    public List<InternetAddress> sendEmail(String templatePath, Map<String, String> emailParams, Map<String, DataSource> attachments, InternetAddress... recipients) {

        List<InternetAddress> failureList = new ArrayList<InternetAddress>();

        if (recipients == null || recipients.length <= 0) {
            throw new IllegalArgumentException(MSG_INVALID_RECIPIENTS);
        }

        final MailTemplate mailTemplate = this.getMailTemplate(templatePath);
        final Class<? extends Email> mailType;
        if (attachments != null && attachments.size() > 0) {
            mailType = HtmlEmail.class;
        } else {
            mailType = this.getMailType(templatePath);
        }
        final MessageGateway<Email> messageGateway = messageGatewayService.getGateway(mailType);

        for (final InternetAddress address : recipients) {
            try {
                // Get a new email per recipient to avoid duplicate attachments
                Email email = getEmail(mailTemplate, mailType, emailParams);
                email.setTo(Collections.singleton(address));

                if (attachments != null && attachments.size() > 0) {
                    for (Map.Entry<String, DataSource> entry : attachments.entrySet()) {
                        ((HtmlEmail) email).attach(entry.getValue(), entry.getKey(), null);
                    }
                }

                messageGateway.send(email);
            } catch (Exception e) {
                failureList.add(address);
                log.error("Error sending email to [ " + address + " ]", e);
            }
        }

        return failureList;
    }

    @Override
    public List<String> sendEmail(String templatePath, Map<String, String> emailParams, Map<String, DataSource> attachments, String... recipients) {
        List<String> failureList = new ArrayList<String>();

        if (recipients == null || recipients.length <= 0) {
            throw new IllegalArgumentException(MSG_INVALID_RECIPIENTS);
        }

        List<InternetAddress> addresses = new ArrayList<InternetAddress>(recipients.length);
        for (String recipient : recipients) {
            try {
                addresses.add(new InternetAddress(recipient));
            } catch (AddressException e) {
                log.warn("Invalid email address {} passed to sendEmail(). Skipping.", recipient);
            }
        }
        InternetAddress[] iAddressRecipients = addresses.toArray(new InternetAddress[addresses.size()]);
        List<InternetAddress> failureInternetAddresses = sendEmail(templatePath, emailParams, attachments, iAddressRecipients);

        for (InternetAddress address : failureInternetAddresses) {
            failureList.add(address.toString());
        }

        return failureList;
    }

    private Email getEmail(final MailTemplate mailTemplate,
                           final Class<? extends Email> mailType,
                           final Map<String, String> params) throws EmailException, MessagingException, IOException {

        final Email email = mailTemplate.getEmail(StrLookup.mapLookup(params), mailType);

        if (params.containsKey(EmailServiceConstants.SENDER_EMAIL_ADDRESS)
                && params.containsKey(EmailServiceConstants.SENDER_NAME)) {

            email.setFrom(
                    params.get(EmailServiceConstants.SENDER_EMAIL_ADDRESS),
                    params.get(EmailServiceConstants.SENDER_NAME));

        } else if (params.containsKey(EmailServiceConstants.SENDER_EMAIL_ADDRESS)) {
            email.setFrom(params.get(EmailServiceConstants.SENDER_EMAIL_ADDRESS));
        }
        if (connectTimeout > 0) {
            email.setSocketConnectionTimeout(connectTimeout);
        }
        if (soTimeout > 0) {
            email.setSocketTimeout(soTimeout);
        }

        // #1008 setting the subject via the setSubject(..) parameter.
        if (params.containsKey(EmailServiceConstants.SUBJECT)) {
            email.setSubject(params.get(EmailServiceConstants.SUBJECT));
        }

        return email;
    }

    private Class<? extends Email> getMailType(String templatePath) {
        return templatePath.endsWith(".html") ? HtmlEmail.class : SimpleEmail.class;
    }

    private MailTemplate getMailTemplate(String templatePath) throws IllegalArgumentException {
        MailTemplate mailTemplate = null;
        Map<String, Object> authInfo = Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, (Object) SERVICE_NAME);
        try (ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(authInfo) ){
            mailTemplate = MailTemplate.create(templatePath, resourceResolver.adaptTo(Session.class));

            if (mailTemplate == null) {
                throw new IllegalArgumentException("Mail template path [ "
                        + templatePath + " ] could not resolve to a valid template");
            }
        } catch (LoginException e) {
            log.error("Unable to obtain an administrative resource resolver to get the Mail Template at [ "
                    + templatePath + " ]", e);
        }

        return mailTemplate;
    }
}
