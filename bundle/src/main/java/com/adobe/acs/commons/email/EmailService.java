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

import java.util.List;
import java.util.Map;
import javax.mail.internet.InternetAddress;

/**
 * A service interface for sending a generic template based Email Notification.
 * 
 */
public interface EmailService {
	
	/**
	 * API that sends an email to a given list of recipients using an Email template.
	 * It uses the CQ Mail Service configuration. Hence it should be configured in the felix console.
	 * 
	 * @param templatePath, Absolute path of the template used to send the Email (e.g. /etc/notification/email/acsEmailTemplate/emailtemplate.txt)
	 * @param recipientsList, List of recipient's email id's 
	 * @param emailParams, Email param Map to be injected in the template
	 * @return Boolean, true- if Email is sent, false otherwise.
	 */
	Boolean sendEmail(String templatePath, List<InternetAddress> recipientsList, Map<String,String> emailParams);
}
