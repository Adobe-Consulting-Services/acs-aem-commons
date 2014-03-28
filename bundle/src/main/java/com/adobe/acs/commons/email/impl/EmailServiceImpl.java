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
import java.util.List;
import java.util.Map;
import javax.jcr.Session;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.commons.lang.text.StrLookup;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.adobe.acs.commons.email.EmailService;
import com.adobe.acs.commons.email.EmailServiceConstants;
import com.day.cq.commons.mail.MailTemplate;
import com.day.cq.mailer.MessageGateway;
import com.day.cq.mailer.MessageGatewayService;

/**
 * Generic Email Notification Service implementation class that defines an API to send a template based Email
 * notification to a given list of recipients.
 *
 */
@Component(immediate = true, metatype = true, label = "ACS AEM Commons - E-mail Service",
			description = "A Generic Email service that sends an email to the given list of recipients using a template specified")
@Service
public class EmailServiceImpl implements EmailService {

	public static final Logger log  = LoggerFactory.getLogger(EmailServiceImpl.class);
	
	/**
	 * This is the global email configuration inside the DAY CQ MAIL service.
	 */
	@Reference
	private MessageGatewayService messageGatewayService;
	 
	@Reference
	private ResourceResolverFactory resourceResolverFactory;
	
	/**
	 * {@inheritDoc}
	 */
	public boolean sendEmail(String templatePath, List<InternetAddress> recipientsList, Map<String, String> emailParams) {
		
		
		ResourceResolver resourceResolver = null;
		
		try {
			resourceResolver = getResourceResolver();
			
			if (resourceResolver.getResource(templatePath) == null) {
				log.error("[IllegalArgumentException] : Missing template -> "+ templatePath);
				return Boolean.FALSE;
			}
			
			final MailTemplate mailTemplate = MailTemplate.create(templatePath, resourceResolver.adaptTo(Session.class));			
		
			final HtmlEmail email = mailTemplate.getEmail(StrLookup.mapLookup(emailParams),HtmlEmail.class);
			
			email.setTo(recipientsList);
			
			setSenderInformation(email, emailParams);

			/**
			 * This is the configuration to send out an email, which will at least
			 * contain one recipient, and message body.
			 * 
			 */
			MessageGateway<HtmlEmail> messageGateway = messageGatewayService.getGateway(HtmlEmail.class);	
			
			if(messageGateway != null) {
				messageGateway.send(email);
				return Boolean.TRUE;
			} 
			else {
				log.error("FAILED to send out the email because the message gateway service is NULL, please check the Mail Service configuration in CQ");
			}
				
		} catch (IOException e) {		
			log.error("[IOException] : Failed to send the Email to Addresses: ", e);		
			
		} catch (MessagingException e) {
			log.error("[MessagingException] : Failed to send the Email to Addresses: ", e);			
			
		} catch (EmailException e) {
			log.error("[EmailException] : Failed to send the Email to Addresses: ", e);	
			
		} catch(Exception e) {
			log.error("[Exception] : Failed to send the Email to Addresses: ", e);	
			
		} finally {
            if (resourceResolver != null) {
                resourceResolver.close();
            }
        }
		
		return Boolean.FALSE;

	}
	
	
	/**
	 * Gets the resourceResolver object from the ResourceResolverFactory Service
	 * @return resourceResolver
	 * @throws LoginException
	 */
	private ResourceResolver getResourceResolver() throws LoginException {
		
		return resourceResolverFactory.getAdministrativeResourceResolver(null);
	}

	
	/**
	 * Sets the Email Sender information if thats present in the input param
	 * @param email
	 * @param emailParams
	 * @throws EmailException
	 */
	private void setSenderInformation(HtmlEmail email, Map<String, String> emailParams) throws EmailException {
		
		if(emailParams.containsKey(EmailServiceConstants.SENDER_EMAIL_ID) && 
				emailParams.containsKey(EmailServiceConstants.SENDER_NAME)) {
			email.setFrom(emailParams.get(EmailServiceConstants.SENDER_EMAIL_ID), emailParams.get(EmailServiceConstants.SENDER_NAME));
		}
		else if(emailParams.containsKey(EmailServiceConstants.SENDER_EMAIL_ID)) {
			email.setFrom(emailParams.get(EmailServiceConstants.SENDER_EMAIL_ID));
		}
	}
}
