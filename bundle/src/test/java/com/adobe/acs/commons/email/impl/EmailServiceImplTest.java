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

import com.day.cq.mailer.MessageGateway;
import com.day.cq.mailer.MessageGatewayService;
import junitx.util.PrivateAccessor;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.mail.ByteArrayDataSource;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.SimpleEmail;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.activation.DataSource;
import javax.mail.internet.MimeMultipart;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class EmailServiceImplTest {

    @Mock
    private MessageGatewayService messageGatewayService;

    @Mock
    private MessageGateway<SimpleEmail> messageGatewaySimpleEmail;

    @Mock
    private MessageGateway<HtmlEmail> messageGatewayHtmlEmail;

    @InjectMocks
    private EmailServiceImpl emailService = new EmailServiceImpl();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public SlingContext context = new SlingContext(ResourceResolverType.JCR_MOCK);


    private static final String emailTemplatePath = "/emailTemplate.txt";
    private static final String EMAIL_TEMPLATE = "emailTemplate.txt";

    private static final String emailTemplateAttachmentPath = "/emailTemplateAttachment.html";
    private static final String EMAIL_TEMPLATE_ATTACHMENT = "emailTemplateAttachment.html";

    @Before
    public final void setUp() throws Exception  {
        context.load().binaryFile(this.getClass().getResourceAsStream(EMAIL_TEMPLATE), emailTemplatePath);
        context.load().binaryFile(this.getClass().getResourceAsStream(EMAIL_TEMPLATE_ATTACHMENT), emailTemplateAttachmentPath);

        when(messageGatewayService.getGateway(SimpleEmail.class)).thenReturn(messageGatewaySimpleEmail);
        when(messageGatewayService.getGateway(HtmlEmail.class)).thenReturn(messageGatewayHtmlEmail);

        context.registerService(MessageGatewayService.class, messageGatewayService);
        context.registerInjectActivateService(emailService);
    }


    @Test
    public final void testSendEmailMultipleRecipients() {

        final String expectedMessage = "This is just a message";
        final String expectedSenderName = "John Smith";
        final String expectedSenderEmailAddress = "john@smith.com";
        //This subject is provided directly inside the sample emailtemplate
        final String expectedSubject = "Greetings";

        final Map<String, String> params = new HashMap<String, String>();
        params.put("message", expectedMessage);
        params.put("senderName", expectedSenderName);
        params.put("senderEmailAddress", expectedSenderEmailAddress);

        final String[] recipients = new String[] {"upasanac@acs.com",
                                                  "david@acs.com",
                                                  "justin@acs.com"
                                                 };
        ArgumentCaptor<SimpleEmail> captor = ArgumentCaptor.forClass(SimpleEmail.class);

        final List<String> failureList = emailService.sendEmail(emailTemplatePath, params, recipients);

        verify(messageGatewaySimpleEmail, times(recipients.length)).send(captor.capture());

        assertEquals(expectedSenderEmailAddress, captor.getValue().getFromAddress().getAddress());
        assertEquals(expectedSenderName, captor.getValue().getFromAddress().getPersonal());
        assertEquals(expectedSubject, captor.getValue().getSubject());

        List<SimpleEmail> actualAllHtmlEmailsSent = captor.getAllValues();
        assertEquals(actualAllHtmlEmailsSent.size(), recipients.length);

        //If email sent to all recipients is successful, failureList is empty
        assertTrue(failureList.isEmpty());
    }


    @Test
    public final void testSendEmailSingleRecipient() {

        final String expectedMessage = "This is just a message";
        final String expectedSenderName = "John Smith";
        final String expectedSenderEmailAddress = "john@smith.com";
        //Subject is provided inside the HtmlTemplate directly
        final String expectedSubject = "Greetings";

        final Map<String, String> params = new HashMap<String, String>();
        params.put("message", expectedMessage);
        params.put("senderName", expectedSenderName);
        params.put("senderEmailAddress", expectedSenderEmailAddress);

        final String recipient =  "upasanac@acs.com";

        ArgumentCaptor<SimpleEmail> captor = ArgumentCaptor.forClass(SimpleEmail.class);

        final List<String> failureList = emailService.sendEmail(emailTemplatePath, params, recipient);

        verify(messageGatewaySimpleEmail, times(1)).send(captor.capture());

        assertEquals(expectedSenderEmailAddress, captor.getValue().getFromAddress().getAddress());
        assertEquals(expectedSenderName, captor.getValue().getFromAddress().getPersonal());
        assertEquals(expectedSubject, captor.getValue().getSubject());
        assertEquals(recipient, captor.getValue().getToAddresses().get(0).toString());

        //If email is sent to the recipient successfully, the response is an empty failureList
        assertTrue(failureList.isEmpty());
    }

    @Test
    public final void testSendEmailAttachment() throws Exception {

        final String expectedMessage = "This is just a message";
        final String expectedSenderName = "John Smith";
        final String expectedSenderEmailAddress = "john@smith.com";
        final String attachment = "This is a attachment.";
        final String attachmentName = "attachment.txt";
        //Subject is provided inside the HtmlTemplate directly
        final String expectedSubject = "Greetings";

        final Map<String, String> params = new HashMap<String, String>();
        params.put("message", expectedMessage);
        params.put("senderName", expectedSenderName);
        params.put("senderEmailAddress", expectedSenderEmailAddress);

        final String recipient =  "upasanac@acs.com";

        Map<String, DataSource> attachments = new HashMap();
        attachments.put(attachmentName, new ByteArrayDataSource(attachment, "text/plain"));

        ArgumentCaptor<HtmlEmail> captor = ArgumentCaptor.forClass(HtmlEmail.class);

        final List<String> failureList = emailService.sendEmail(emailTemplateAttachmentPath, params, attachments, recipient);

        verify(messageGatewayHtmlEmail, times(1)).send(captor.capture());

        assertEquals(expectedSenderEmailAddress, captor.getValue().getFromAddress().getAddress());
        assertEquals(expectedSenderName, captor.getValue().getFromAddress().getPersonal());
        assertEquals(expectedSubject, captor.getValue().getSubject());
        assertEquals(recipient, captor.getValue().getToAddresses().get(0).toString());
        Method getContainer = captor.getValue().getClass().getSuperclass().getDeclaredMethod("getContainer");
        getContainer.setAccessible(true);
        MimeMultipart mimeMultipart = (MimeMultipart) getContainer.invoke(captor.getValue());
        getContainer.setAccessible(false);
        assertEquals(attachment, mimeMultipart.getBodyPart(0).getContent().toString());

        //If email is sent to the recipient successfully, the response is an empty failureList
        assertTrue(failureList.isEmpty());
    }

    @Test
    public final void testSendEmailNoRecipients() {
        final String templatePath = emailTemplatePath;
        final Map<String, String> params = new HashMap<String, String>();
        final String[] recipients = new String[] {};

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Invalid Recipients");

        emailService.sendEmail(templatePath, params, recipients);
    }


    @Test(expected=IllegalArgumentException.class)
    public final void testBlankTemplatePath() {
        final String templatePath = null;
        final Map<String, String> params = new HashMap<String, String>();
        final String recipient =  "upasanac@acs.com";

        emailService.sendEmail(templatePath, params, recipient);
     }

    @Test
    public final void testInValidTemplatePath() {
        final String templatePath = "/invalidTemplatePath.txt";
        final Map<String, String> params = new HashMap<String, String>();
        final String recipient =  "upasanac@acs.com";
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Mail template path [ /invalidTemplatePath.txt ] could not resolve to a valid template");

        emailService.sendEmail(templatePath, params, recipient);
     }

    @Test
    public void testDefaultTimeouts() {
        context.registerInjectActivateService(emailService,Collections.emptyMap());
        SimpleEmail email = sendTestEmail();
        assertEquals(30000, email.getSocketConnectionTimeout());
        assertEquals(30000, email.getSocketTimeout());
    }

    @Test
    public void testCustomTimeouts() {
        Map<String, Object> params = new HashMap<>();
        params.put("so.timeout", 100);
        params.put("conn.timeout", 500);
        emailService.activate(params);
        SimpleEmail email = sendTestEmail();
        assertEquals(500, email.getSocketConnectionTimeout());
        assertEquals(100, email.getSocketTimeout());
    }

    private SimpleEmail sendTestEmail() {
        final Map<String, String> params = new HashMap<String, String>();
        params.put("message", "This is just a message");
        params.put("senderName", "John Smith");
        params.put("senderEmailAddress", "john@smith.com");

        final String recipient =  "upasanac@acs.com";

        ArgumentCaptor<SimpleEmail> captor = ArgumentCaptor.forClass(SimpleEmail.class);

        List<String> failureList = emailService.sendEmail(emailTemplatePath, params, recipient);

        verify(messageGatewaySimpleEmail, times(1)).send(captor.capture());

        return captor.getValue();
    }

    @Test
    public final void testSubjectSetting() {
        final String expectedSubject = "问候";

        final Map<String, String> params = new HashMap<String, String>();
        params.put("subject", expectedSubject);

        final String recipient =  "upasanac@acs.com";

        ArgumentCaptor<SimpleEmail> captor = ArgumentCaptor.forClass(SimpleEmail.class);

        emailService.sendEmail(emailTemplatePath, params, recipient);
        verify(messageGatewaySimpleEmail, times(1)).send(captor.capture());

        assertEquals(expectedSubject, captor.getValue().getSubject());
    }

    @Test
    public final void testBounceAddress() throws Exception {
        final String expectedBounceAddress = RandomStringUtils.randomAlphabetic(10) + "@test.com";

        final Map<String, String> params = new HashMap<String, String>();
        params.put("bounceAddress", expectedBounceAddress);

        final String recipient =  "upasanac@acs.com";

        ArgumentCaptor<SimpleEmail> captor = ArgumentCaptor.forClass(SimpleEmail.class);

        emailService.sendEmail(emailTemplatePath, params, recipient);
        verify(messageGatewaySimpleEmail, times(1)).send(captor.capture());

        // getter isn't available until 1.4. See https://issues.apache.org/jira/browse/EMAIL-146
        Object actualBounceAddress = PrivateAccessor.getField(captor.getValue(), "bounceAddress");

        assertEquals(expectedBounceAddress, actualBounceAddress);
    }
}