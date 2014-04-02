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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.jcr.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.commons.lang.text.StrLookup;
import org.apache.commons.mail.HtmlEmail;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.email.EmailService;
import com.adobe.acs.commons.email.EmailServiceConstants;
import com.day.cq.commons.mail.MailTemplate;
import com.day.cq.mailer.MessageGateway;
import com.day.cq.mailer.MessageGatewayService;

@Component(label = "ACS AEM Commons - E-mail Service",
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
            // no recipients - return false
            return false;
        }
    }

    @Override
    public boolean sendEmail(final String templatePath, final Map<String, String> emailParams,
            final InternetAddress... recipients) {
        
        if (recipients != null && recipients.length > 0) {
            MessageGateway<HtmlEmail> messageGateway = messageGatewayService.getGateway(HtmlEmail.class);
            if (messageGateway == null) {
                log.error("Failed to send email due to null message gateway service. Please check configuration.");
                return false;
            }

            HtmlEmail email = getEmail(templatePath, emailParams);
            if (email != null) {
                boolean success = false;
                for (InternetAddress address : recipients) {
                    try {
                        email.setTo(Collections.singleton(address));
                        messageGateway.send(email);
                        success = true;
                    } catch (Exception e) {
                        log.error("Exception sending email to " + address, e);
                    }
                }
                return success;
            } else {
                // no email - return false
                return false;
            }
        } else {
            // no recipients - return false
            return false;
        }

    }

    private HtmlEmail getEmail(String templatePath, Map<String, String> emailParams) {
        ResourceResolver resourceResolver = null;
        try {
            resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);

           final MailTemplate mailTemplate = MailTemplate
                    .create(templatePath, resourceResolver.adaptTo(Session.class));

           if (mailTemplate == null) {
               log.warn("Email template at {} could not be created.", templatePath);
               return null;
           }

            final HtmlEmail email = mailTemplate.getEmail(StrLookup.mapLookup(emailParams), HtmlEmail.class);

            if (emailParams.containsKey(EmailServiceConstants.SENDER_EMAIL_ADDRESS)
                    && emailParams.containsKey(EmailServiceConstants.SENDER_NAME)) {
                email.setFrom(emailParams.get(EmailServiceConstants.SENDER_EMAIL_ADDRESS),
                        emailParams.get(EmailServiceConstants.SENDER_NAME));
            } else if (emailParams.containsKey(EmailServiceConstants.SENDER_EMAIL_ADDRESS)) {
                email.setFrom(emailParams.get(EmailServiceConstants.SENDER_EMAIL_ADDRESS));
            }

            return email;
        } catch (Exception e) {
            log.error("Unable to construct email from template " + templatePath, e);
            if (resourceResolver != null) {
                resourceResolver.close();
            }
            return null;
        }
    }

}
