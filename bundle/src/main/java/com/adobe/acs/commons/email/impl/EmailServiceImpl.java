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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.jcr.Session;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.commons.lang.text.StrLookup;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.email.EmailService;
import com.adobe.acs.commons.email.EmailServiceConstants;
import com.day.cq.commons.mail.MailTemplate;
import com.day.cq.mailer.MessageGateway;
import com.day.cq.mailer.MessageGatewayService;

@Component(immediate = true, label = "ACS AEM Commons - E-mail Service",
    description = "A Generic Email service that sends an email to the given list of recipients.")
@Service
public final class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Reference
    private MessageGatewayService messageGatewayService;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Override
    public boolean sendEmail(final String templatePath, final Map<String, String> emailParams,
            final String... recipients) {

        if (recipients != null && recipients.length > 0) {
            List<InternetAddress> addresses = new ArrayList<InternetAddress>(recipients.length);
            for (String recipient : recipients) {
                try {
                    addresses.add(new InternetAddress(recipient));
                } catch (AddressException e) {
                    log.warn("Invalid email address {} passed to sendEmail(). Skipping.", recipient);
                }
            }
            InternetAddress[] iAddressRecipients = addresses.toArray(new InternetAddress[addresses.size()]);
 
            return sendEmail(templatePath, emailParams, iAddressRecipients);

        } else {
            return false;
        }
    }

    @Override
    public boolean sendEmail(final String templatePath, final Map<String, String> emailParams,
            final InternetAddress... recipients) {

        ResourceResolver resourceResolver = null;

        try {
            resourceResolver = getResourceResolver();

            if (resourceResolver.getResource(templatePath) == null) {
                log.error("Missing template at path {} ", templatePath);
                return false;
            }

            final MailTemplate mailTemplate = MailTemplate
                    .create(templatePath, resourceResolver.adaptTo(Session.class));

            final HtmlEmail email = mailTemplate.getEmail(StrLookup.mapLookup(emailParams), HtmlEmail.class);

            List<InternetAddress> recipientsList = Arrays.asList(recipients);

            email.setTo(recipientsList);

            setSenderInformation(email, emailParams);

            /**
            * This is the configuration to send out an email, which will at least
            * contain one recipient, and message body.
            *
            */
            MessageGateway<HtmlEmail> messageGateway = messageGatewayService.getGateway(HtmlEmail.class);

            if (messageGateway != null) {
                messageGateway.send(email);
                return true;
            } else {
                log.error("Failed to send email due to null message gateway service. Please check configuration.");
            }

        } catch (IOException e) {
            log.error("Failed to send the Email to Addresses: ", e);

        } catch (MessagingException e) {
            log.error("Failed to send the Email to Addresses: ", e);

        } catch (EmailException e) {
            log.error("Failed to send the Email to Addresses: ", e);

        } catch (Exception e) {
            log.error("Failed to send the Email to Addresses: ", e);

        } finally {
            if (resourceResolver != null) {
                resourceResolver.close();
            }
        }

        return false;

    }

    private ResourceResolver getResourceResolver() throws LoginException {

        return resourceResolverFactory.getAdministrativeResourceResolver(null);
    }

    private void setSenderInformation(final HtmlEmail email, final Map<String, String> emailParams)
            throws EmailException {

        if (emailParams.containsKey(EmailServiceConstants.SENDER_EMAIL_ADDRESS)
                && emailParams.containsKey(EmailServiceConstants.SENDER_NAME)) {
            email.setFrom(emailParams.get(EmailServiceConstants.SENDER_EMAIL_ADDRESS),
                    emailParams.get(EmailServiceConstants.SENDER_NAME));
        } else if (emailParams.containsKey(EmailServiceConstants.SENDER_EMAIL_ADDRESS)) {
            email.setFrom(emailParams.get(EmailServiceConstants.SENDER_EMAIL_ADDRESS));
        }
    }
}
