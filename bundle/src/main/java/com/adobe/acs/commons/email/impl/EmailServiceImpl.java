package com.adobe.acs.commons.email.impl;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.jcr.Session;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
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
 * 
 * @author Upasana Chaube, 2014
 *
 */

@Component(immediate = true, metatype = true, label = "Generic Email Service",
			description = "A Generic Email service that sends an email to the given list of recipients using a template specified")
@Service(EmailService.class)
public class EmailServiceImpl implements EmailService {

	public static final Logger LOG  = LoggerFactory.getLogger(EmailServiceImpl.class);
	
	/**
	 * This is the global email configuration inside the DAY CQ MAIL service.
	 * 
	 */
	@Reference
	private MessageGatewayService messageGatewayService;

	/**
	 * This is the configuration to send out an email, which will at least
	 * contain one recipient, and message body.
	 * 
	 */
	private MessageGateway<HtmlEmail> messageGateway;
	 
	@Reference
	private ResourceResolverFactory resourceResolverFactory;
	
	public Boolean sendEmail(String templatePath, List<InternetAddress> recipientsList, Map<String, String> emailParams) {
		
		try {
			
			Resource templateRsrc = null;	
			ResourceResolver resourceResolver = getResourceResolver();
			if (resourceResolver.resolve(templatePath) != null) {
				templateRsrc = resourceResolver.resolve(templatePath) ;
			}
			if (templateRsrc == null) {
				LOG.error("IllegalArgumentException: Missing template -> "+ templatePath);
				return false;
			}
  		
			final MailTemplate mailTemplate = MailTemplate.create(templateRsrc.getPath(), templateRsrc.getResourceResolver().adaptTo(Session.class));			
		
			final HtmlEmail email = mailTemplate.getEmail(StrLookup.mapLookup(emailParams),HtmlEmail.class);
			
			email.setTo(recipientsList);
			
			//Set Email Sender information if thats present in the input param			
			if(emailParams.containsKey(EmailServiceConstants.SENDER_EMAIL_ID) && 
					emailParams.containsKey(EmailServiceConstants.SENDER_NAME))
				email.setFrom(emailParams.get(EmailServiceConstants.SENDER_EMAIL_ID), emailParams.get(EmailServiceConstants.SENDER_NAME));
			else if(emailParams.containsKey(EmailServiceConstants.SENDER_EMAIL_ID))
				email.setFrom(emailParams.get(EmailServiceConstants.SENDER_EMAIL_ID));
			
			// Check the logs to see that messageGatewayService is not null
			LOG.info("messageGatewayService : " + messageGatewayService);
			
			if (messageGatewayService == null) {
				LOG.error("FAILED to send out the email because the message gateway service is NULL, please check the Mail Service configuration in CQ");
			}

			else {
				messageGateway = messageGatewayService.getGateway(HtmlEmail.class);
				
				// Check the logs to see that messageGateway is not null
				LOG.info("messageGateway : " + messageGateway);
				
				messageGateway.send(email);
				
			    return true;
			}

		} catch (IOException e) {		
			LOG.error("IOException : FAILED TO SEND THE EMAIL TO ADDRESSES: ", e);		
			
		} catch (MessagingException e) {
			LOG.error("MessagingException : FAILED TO SEND THE EMAIL TO ADDRESSES: ", e);			
			
		} catch (EmailException e) {
			LOG.error("EmailException : FAILED TO SEND THE EMAIL TO ADDRESSES: ", e);	
			
		}catch(Exception e){
			LOG.error("Exception : FAILED TO SEND THE EMAIL TO ADDRESSES: ", e);	
		}
		 return false;

	}
	
	private ResourceResolver getResourceResolver() throws LoginException {
		
		return resourceResolverFactory.getAdministrativeResourceResolver(null);
	}

}
