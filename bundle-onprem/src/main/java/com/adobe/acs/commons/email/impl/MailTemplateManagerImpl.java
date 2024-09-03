/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2024 Adobe
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
import java.util.Map;

import javax.mail.MessagingException;

import org.apache.commons.lang.text.StrLookup;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.osgi.service.component.annotations.Component;

import com.day.cq.commons.mail.MailTemplate;

@Component(service = MailTemplateManager.class)
public class MailTemplateManagerImpl implements MailTemplateManager {

    @Override
    public <T extends Email> T getEmail(final MailTemplate template,
        final Map<String, String> params,
        final Class<T> mailType)
            throws IOException, EmailException, MessagingException {
        return template.getEmail(StrLookup.mapLookup(params), mailType);
    }
}
