/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2024 Adobe
 *
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
 */
package com.adobe.acs.commons.email;

import java.io.IOException;
import java.util.Map;

import javax.mail.MessagingException;

import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;

import com.day.cq.commons.mail.MailTemplate;

/**
 * Abstraction to allow different methods to be used between Cloud Service and on prem.
 */
public interface MailTemplateManager {

    /**
     * Get the email from the template
     *
     * @param <T>      The email type
     * @param template The email template
     * @param params   Optional parameters used inside the template
     * @param mailType The email type
     * @return The email object
     * @throws IOException        If an error occurs handling the text template.
     * @throws MessagingException If an error occurs during building the email message.
     * @throws EmailException     If an error occurs during building the email.
     */
    <T extends Email> T getEmail(MailTemplate template, final Map<String, String> params, Class<T> mailType)
            throws IOException, EmailException, MessagingException;
}
