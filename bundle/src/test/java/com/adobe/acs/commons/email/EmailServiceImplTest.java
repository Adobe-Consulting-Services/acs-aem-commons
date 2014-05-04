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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.jcr.Session;
import org.apache.commons.mail.HtmlEmail;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import com.adobe.acs.commons.email.impl.EmailServiceImpl;
import com.day.cq.commons.mail.MailTemplate;
import com.day.cq.mailer.MessageGateway;
import com.day.cq.mailer.MessageGatewayService;

@RunWith(PowerMockRunner.class)
@PrepareForTest(MailTemplate.class)
public class EmailServiceImplTest {

    @Mock
    MessageGatewayService messageGatewayService;
	
    @Mock
    ResourceResolverFactory resourceResolverFactory;
	
    @Mock
    ResourceResolver resourceResolver;

    @Mock
    MessageGateway<HtmlEmail> messageGateway;
	
    @Mock
    Session session;
	
    @InjectMocks
    private EmailServiceImpl emailService;

    private String emailTemplatePath;

    @Before
    public void setUp() throws Exception {
    	
    	MockitoAnnotations.initMocks(this);
      
        when(messageGatewayService.getGateway(HtmlEmail.class)).thenReturn(messageGateway);
        when(resourceResolverFactory.getAdministrativeResourceResolver(null)).thenReturn(resourceResolver);
        when(resourceResolver.adaptTo(Session.class)).thenReturn(session);

        emailTemplatePath = this.getClass().getResource("/emailTemplate.txt").getFile().toString();
        
        // Get Mail Template
        PowerMockito.mockStatic(MailTemplate.class);
        when(MailTemplate.create(emailTemplatePath, session)).thenReturn(new MailTemplate(new FileInputStream(emailTemplatePath), "UTF-8"));
    }
  

    @Test
    public void test_sendEmail_multipleRecipients() throws Exception {
       
    	final String expectedMessage = "This is just a message";
        final String expectedSenderName = "John Smith";
        final String expectedSenderEmailAddress = "john@smith.com";
        //Subject is provided inside the HtmlTemplate directly
        final String expectedSubject = "Greetings";
        
        final Map<String, String> params = new HashMap<String, String>();
        params.put("message", expectedMessage);
        params.put("senderName", expectedSenderName);
        params.put("senderEmailAddress", expectedSenderEmailAddress);

        final String[] recipients = new String[] { "upsanac@acs.com",
                                                    "david@acs.com",
                                                    "justin@acs.com"
                                                  };
        ArgumentCaptor<HtmlEmail> captor = ArgumentCaptor.forClass(HtmlEmail.class);
        
        boolean result = emailService.sendEmail(emailTemplatePath, params, recipients);
        
        verify(messageGateway, times(recipients.length)).send(captor.capture());
        
        assertEquals(expectedSenderEmailAddress, captor.getValue().getFromAddress().getAddress());
        assertEquals(expectedSenderName, captor.getValue().getFromAddress().getPersonal());
        assertEquals(expectedSubject, captor.getValue().getSubject());
        
        List<HtmlEmail> actualAllHtmlEmailsSent = captor.getAllValues();
        assertEquals(actualAllHtmlEmailsSent.size(), recipients.length);
        
        // Assert the last recipient address who received an email
        assertEquals(recipients[2], captor.getValue().getToAddresses().get(0).toString());
        
        // Assert the result of the method call is true
        assertTrue(result);
    }

    
    @Test
    public void test_sendEmail_singleRecipient() throws Exception {
       
    	final String expectedMessage = "This is just a message";
        final String expectedSenderName = "John Smith";
        final String expectedSenderEmailAddress = "john@smith.com";
        //Subject is provided inside the HtmlTemplate directly
        final String expectedSubject = "Greetings";
        
        final Map<String, String> params = new HashMap<String, String>();
        params.put("message", expectedMessage);
        params.put("senderName", expectedSenderName);
        params.put("senderEmailAddress", expectedSenderEmailAddress);

        final String recipient =  "upsanac@acs.com";
        
        ArgumentCaptor<HtmlEmail> captor = ArgumentCaptor.forClass(HtmlEmail.class);
        
        boolean result = emailService.sendEmail(emailTemplatePath, params, recipient);
        
        verify(messageGateway, times(1)).send(captor.capture());
        
        assertEquals(expectedSenderEmailAddress, captor.getValue().getFromAddress().getAddress());
        assertEquals(expectedSenderName, captor.getValue().getFromAddress().getPersonal());
        assertEquals(expectedSubject, captor.getValue().getSubject());
        assertEquals(recipient, captor.getValue().getToAddresses().get(0).toString());
        
        // Assert the result of the method call is true
        assertTrue(result);
    }
    

    @Test
    public void test_sendEmail_noRecipients() throws Exception {
        final String templatePath = emailTemplatePath;

        final Map<String, String> params = new HashMap<String, String>();

        final String[] recipients = new String[] { };

        final boolean result = emailService.sendEmail(templatePath, params, recipients);

        assertFalse(result);
    }


   @After
	public void tearDown() {
	   Mockito.reset();
	}
}
