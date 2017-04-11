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

import com.adobe.acs.commons.email.impl.EmailServiceImpl;
import com.day.cq.commons.mail.MailTemplate;
import com.day.cq.mailer.MessageGateway;
import com.day.cq.mailer.MessageGatewayService;
import org.apache.commons.mail.ByteArrayDataSource;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.SimpleEmail;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.activation.DataSource;
import javax.jcr.Session;
import javax.mail.internet.MimeMultipart;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest(MailTemplate.class)
public class EmailServiceImplTest {

    @Mock
    private MessageGatewayService messageGatewayService;

    @Mock
    private ResourceResolverFactory resourceResolverFactory;

    @Mock
    private ResourceResolver resourceResolver;

    @Mock
    private MessageGateway<SimpleEmail> messageGateway;

    @Mock
    private MessageGateway<HtmlEmail> messageGatewayHtmlEmail;

    @Mock
    private Session session;

    @InjectMocks
    private EmailServiceImpl emailService;

    @Rule
    private ExpectedException thrown = ExpectedException.none();

    private String emailTemplatePath;

    private String emailTemplateAttachmentPath;

    @Before
    public final void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);

        when(messageGatewayService.getGateway(SimpleEmail.class)).thenReturn(messageGateway);
        when(messageGatewayService.getGateway(HtmlEmail.class)).thenReturn(messageGatewayHtmlEmail);
        when(resourceResolverFactory.getServiceResourceResolver(Matchers.anyMap())).thenReturn(resourceResolver);
        when(resourceResolver.adaptTo(Session.class)).thenReturn(session);

        emailTemplatePath = new File(this.getClass().getResource("/emailTemplate.txt").toURI()).getPath();

        emailTemplateAttachmentPath = new File(this.getClass().getResource("/emailTemplateAttachment.html").toURI()).getPath();

        // Mock the Mail Template
        PowerMockito.mockStatic(MailTemplate.class);
        when(MailTemplate.create(emailTemplatePath, session)).thenReturn(
            new MailTemplate(new FileInputStream(emailTemplatePath), "UTF-8"));

        when(MailTemplate.create(emailTemplateAttachmentPath, session)).thenReturn(
                new MailTemplate(new FileInputStream(emailTemplateAttachmentPath), "UTF-8"));

    }


    @Test
    public final void testSendEmailMultipleRecipients() throws Exception {

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

        List<String> failureList = emailService.sendEmail(emailTemplatePath, params, recipients);

        verify(messageGateway, times(recipients.length)).send(captor.capture());

        assertEquals(expectedSenderEmailAddress, captor.getValue().getFromAddress().getAddress());
        assertEquals(expectedSenderName, captor.getValue().getFromAddress().getPersonal());
        assertEquals(expectedSubject, captor.getValue().getSubject());

        List<SimpleEmail> actualAllHtmlEmailsSent = captor.getAllValues();
        assertEquals(actualAllHtmlEmailsSent.size(), recipients.length);

        //If email sent to all recipients is successful, failureList is empty
        assertTrue(failureList.isEmpty());
    }


    @Test
    public final void testSendEmailSingleRecipient() throws Exception {

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

        List<String> failureList = emailService.sendEmail(emailTemplatePath, params, recipient);

        verify(messageGateway, times(1)).send(captor.capture());

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
        String attachment = "This is a attachment.";
        String attachmentName = "attachment.txt";
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

        List<String> failureList = emailService.sendEmail(emailTemplateAttachmentPath, params, attachments, recipient);

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
    public final void testSendEmailNoRecipients() throws Exception {
        final String templatePath = emailTemplatePath;
        final Map<String, String> params = new HashMap<String, String>();
        final String[] recipients = new String[] {};

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Invalid Recipients");

        emailService.sendEmail(templatePath, params, recipients);
    }

    @Test
    public final void testBlankTemplatePath() throws Exception {
        final String templatePath = null;
        final Map<String, String> params = new HashMap<String, String>();
        final String recipient =  "upasanac@acs.com";
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Mail template path [ null ] could not resolve to a valid template");

        emailService.sendEmail(templatePath, params, recipient);
     }

    @Test
    public final void testInValidTemplatePath() throws Exception {
        final String templatePath = "/invalidTemplatePath.txt";
        final Map<String, String> params = new HashMap<String, String>();
        final String recipient =  "upasanac@acs.com";
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Mail template path [ /invalidTemplatePath.txt ] could not resolve to a valid template");

        emailService.sendEmail(templatePath, params, recipient);
     }


    @After
    public final void tearDown() {
        Mockito.reset();
    }
}