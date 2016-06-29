package com.adobe.acs.commons.email.impl;

import com.adobe.acs.commons.email.EmailServiceConstants;
import com.adobe.acs.commons.email.EmailServiceWithAttachment;
import com.day.cq.commons.mail.MailTemplate;
import com.day.cq.mailer.MessageGateway;
import com.day.cq.mailer.MessageGatewayService;
import org.apache.commons.lang.text.StrLookup;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.SimpleEmail;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
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
 * Original source https://github.com/Adobe-Consulting-Services/acs-aem-commons/blob/master/bundle/src/main/java/com/adobe/acs/commons/email/impl/EmailServiceImpl.java
 *
 * A Generic Email service that sends an email with attachments to a given list of recipients.
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
 *
 */
@Component
@Service
public class EmailServiceWithAttachmentImpl implements EmailServiceWithAttachment {
    private static final Logger log = LoggerFactory.getLogger(EmailServiceWithAttachmentImpl.class);

    @Reference
    MessageGatewayService messageGatewayService;

    @Reference
    ResourceResolverFactory resourceResolverFactory;

    @Override
    public List<String> sendEmail(final String templatePath, final Map<String, String> emailParams, Map<String, DataSource> attachments, final String... recipients) {

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
        List<InternetAddress> failureInternetAddresses = sendEmail(templatePath, emailParams, attachments, iAddressRecipients);

        for (InternetAddress address : failureInternetAddresses) {
            failureList.add(address.toString());
        }

        return failureList;
    }


    @Override
    public List<InternetAddress> sendEmail(final String templatePath, final Map<String, String> emailParams, Map<String, DataSource> attachments, final InternetAddress... recipients) {

        List<InternetAddress> failureList = new ArrayList<InternetAddress>();

        if (recipients == null || recipients.length <= 0) {
            throw new IllegalArgumentException("Invalid Recipients");
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

    private <T extends Email> T getEmail(final MailTemplate mailTemplate,
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

        return (T) email;
    }

    private Class<? extends Email> getMailType(String templatePath) {
        return templatePath.endsWith(".html") ? HtmlEmail.class : SimpleEmail.class;
    }

    private MailTemplate getMailTemplate(String templatePath) throws IllegalArgumentException {
        MailTemplate mailTemplate = null;
        ResourceResolver resourceResolver = null;
        try {
            resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
            mailTemplate = MailTemplate.create(templatePath, resourceResolver.adaptTo(Session.class));

            if (mailTemplate == null) {
                throw new IllegalArgumentException("Mail template path [ "
                        + templatePath + " ] could not resolve to a valid template");
            }
        } catch (LoginException e) {
            log.error("Unable to obtain an administrative resource resolver to get the Mail Template at [ "
                    + templatePath + " ]", e);
        } finally {
            if (resourceResolver != null) {
                resourceResolver.close();
            }
        }

        return mailTemplate;
    }
}
