package com.adobe.acs.commons.email;

import java.util.List;
import java.util.Map;
import javax.mail.internet.InternetAddress;

/**
 * 
 * @author Upasana Chaube, 2014
 *
 */
public interface EmailService {
	
	Boolean sendEmail(String templatePath, List<InternetAddress> recipientsList, Map<String,String> emailParams);
}
