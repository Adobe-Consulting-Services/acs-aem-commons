/*
* #%L
 * * ACS AEM Commons Bundle
 * *
 * %%
 * Copyright (C) 2013 Adobe
 * *
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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrLookup;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.SimpleEmail;
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
    description = "A Generic Email service that sends an email to a given list of recipients.")
@Service
public final class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Reference
    private MessageGatewayService messageGatewayService;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Override
    public List<String> sendEmail(final String templatePath, final Map<String, String> emailParams,
        final String... recipients) {
        List<String> failureList = new ArrayList<String>();
        if (recipients == null || recipients.length <= 0) {
            throw new IllegalArgumentException("Invalid Recipients");
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
        List<InternetAddress> failureInternetAddresses =  sendEmail(templatePath, emailParams, iAddressRecipients);

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
            throw new IllegalArgumentException("Invalid Recipients");
        }

        if (StringUtils.isBlank(templatePath)) {
            throw new IllegalArgumentException("Template path is null or empty");
        }
        Email email = getEmail(templatePath, emailParams);

        if (email == null) {
            throw new IllegalArgumentException("Error while creating template");
        }

        MessageGateway<Email> messageGateway = messageGatewayService.getGateway(email.getClass());

        for (InternetAddress address : recipients) {
            try {
                email.setTo(Collections.singleton(address));
                messageGateway.send(email);
            } catch (Exception e) {
                failureList.add(address);
                log.error("Exception sending email to " + address, e);
            }
         }

        return failureList;
    }


    private Email getEmail(String templatePath, Map<String, String> emailParams) {
        ResourceResolver resourceResolver = null;
        try {
            resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);

           final MailTemplate mailTemplate = MailTemplate.create(templatePath, resourceResolver.adaptTo(Session.class));

           if (mailTemplate == null) {
               log.warn("Email template at {} could not be created.", templatePath);
               return null;
           }

           Class<? extends Email> emailClass = templatePath.endsWith(".html") ? HtmlEmail.class : SimpleEmail.class;

           final Email email = mailTemplate.getEmail(StrLookup.mapLookup(emailParams), emailClass);

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
        } finally {
            if (resourceResolver != null) {
                resourceResolver.close();
            }
        }

        return null;
    }

}
