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
package com.adobe.acs.commons.email;

import java.util.Map;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

/**
 * A service interface for sending a generic template based Email Notification.
 * 
 */
public interface EmailService {
	
	/**
	 * API that sends an email to a given recipients
	 *  using an Email template. It uses the CQ Mail Service configuration.
	 *  Hence it should be configured in the felix console. 
	 * @param templatePath, Absolute path of the template used to send the Email 
	 * (e.g. /etc/notification/email/acsEmailTemplate/emailtemplate.txt)
	 * @param emailParams, Email param Map to be injected in the template
	 * @param recipients, Variable InternetAddress array of recipient's email id's 
	 * 
	 * @return Boolean, true- if Email is sent, false otherwise.
	 */
	boolean sendEmail(String templatePath, Map<String,String> emailParams, InternetAddress... recipients);
	
	/**
	 * API that sends an email to a given recipients
	 *  using an Email template. It uses the CQ Mail Service configuration.
	 *  Hence it should be configured in the felix console.
	 * @param templatePath, Absolute path of the template used to send the Email 
	 * (e.g. /etc/notification/email/acsEmailTemplate/emailtemplate.txt)
	 * @param emailParams, Email param Map to be injected in the template
	 * @param recipients, Variable String array of recipient's email id's 
	 * @return
	 * @throws AddressException
	 */
	boolean sendEmail(String templatePath, Map<String, String> emailParams, String... recipients) throws AddressException;
}
