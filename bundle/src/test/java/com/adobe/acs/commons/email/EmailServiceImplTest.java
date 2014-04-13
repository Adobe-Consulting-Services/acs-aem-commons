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

import javax.jcr.Session;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    /*
    private Map<String, String> emailParams;
    private InternetAddress[] recipientsInternetAddressArray;
    private InternetAddress singleRecipientInternetAddress;
    private String[] recipientsStringArray;
    private String singleRecipientString;
    private Method getEmailMethod;
    private Class[] parameterTypes;
    private Object[] parameters;
*/
    private String emailTemplatePath;

    @Before
    public void setUp() throws Exception {

        emailTemplatePath = this.getClass().getResource("/emailTemplate.txt").getFile().toString();

        emailService = new EmailServiceImpl();
        MockitoAnnotations.initMocks(this);

        when(messageGatewayService.getGateway(HtmlEmail.class)).thenReturn(messageGateway);
        when(resourceResolverFactory.getAdministrativeResourceResolver(null)).thenReturn(resourceResolver);
        when(resourceResolver.adaptTo(Session.class)).thenReturn(session);

        // Get Mail Template
        PowerMockito.mockStatic(MailTemplate.class);
        when(MailTemplate.create(emailTemplatePath, session)).thenReturn(new MailTemplate(new FileInputStream(emailTemplatePath), "UTF-8"));
    }
    /*
    private void populateTestData() {
    	emailTemplatePath = this.getClass().getResource("/emailTemplate.txt").getFile().toString();
    	emailParams.put("message", "This is just for test");
    	emailParams.put("senderName", "John Smith");
    	emailParams.put("senderEmailAddress", "johnsmith@example.com");    	
    }
    
    private void populateDataForSingleInternetAddressRecipient() throws AddressException {
    	singleRecipientInternetAddress = new InternetAddress("recipient@example.com");
    }
    
    private void populateDataForMultiInternetAddressRecipients() throws AddressException {
    	recipientsInternetAddressArray = new InternetAddress[] { new InternetAddress("recipient1@example.com"),
                                                                 new InternetAddress("recipient2@example.com")
                                                                };
    }
    
    private void populateDataForSingleStringRecipient() {
    	singleRecipientString = "recipient@example.com";
    }
    
    private void populateDataForMultiStringRecipient() {
    	recipientsStringArray = new String[] { "recipient1@example.com",
                                               "recipient2@example.com",
                                               "recipient3@example.com"
                                              };
    }
    
    private void testPreperationForGetEmailMethod() throws Exception {
    	parameterTypes = new Class[2];
    	parameterTypes[0] = java.lang.String.class;
    	parameterTypes[1] = java.util.Map.class;
    	getEmailMethod = emailService.getClass().getDeclaredMethod("getEmail", parameterTypes);
    	getEmailMethod.setAccessible(true);
    	parameters = new Object[2];

    }
    */


    @Test
    public void test_sendEmail() throws Exception {
        final String expMessage = "this is just a message";
        final String expSenderName = "John Smith";
        final String expSenderEmailAddress = "john@smith.com";

        final String templatePath = emailTemplatePath;

        final Map<String, String> params = new HashMap<String, String>();
        params.put("message", expMessage);
        params.put("senderName", expSenderName);
        params.put("senderEmailAddress", expSenderEmailAddress);

        final String[] recipients = new String[] {
                "upsanac@acs.com",
                "david@acs.com",
                "justin@acs.com"};

        final ArgumentCaptor<HtmlEmail> captor = ArgumentCaptor.forClass(HtmlEmail.class);
        final boolean result = emailService.sendEmail(templatePath, params, recipients);

        verify(messageGateway, times(recipients.length)).send(captor.capture());

        assertEquals(expSenderEmailAddress, captor.getValue().getFromAddress().getAddress());
        assertEquals(expSenderName, captor.getValue().getFromAddress().getPersonal());
        /* Add other assertions against the HtmlEmail object that is passed to Send */

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

    /*
    
    @Test
    public void test_to_send_email_to_multiple_Internet_addressess() throws Exception {
    	boolean emailSent_Expected = true;   
    	populateDataForMultiInternetAddressRecipients();   	
    	mockMailTemplate();  
    	boolean emailSent_Actual = emailService.sendEmail(emailTemplatePath, emailParams, recipientsInternetAddressArray); 	
    	
		assertEquals(emailSent_Expected, emailSent_Actual);
    	
    }
    
    @Test
    public void test_to_send_email_to_single_Internet_address() throws Exception {
    	boolean emailSent_Expected = true;  
    	populateDataForSingleInternetAddressRecipient();
    	mockMailTemplate();
    	boolean emailSent_Actual = emailService.sendEmail(emailTemplatePath, emailParams, singleRecipientInternetAddress);
   
    	assertEquals(emailSent_Expected, emailSent_Actual);
    }
    
    @Test
	public void test_to_send_email_to_multiple_String_addresses() throws Exception {
    	boolean emailSent_Expected = true; 
    	populateDataForMultiStringRecipient();
    	mockMailTemplate();
    	boolean emailSent_Actual = emailService.sendEmail(emailTemplatePath, emailParams, recipientsStringArray);
    
    	assertEquals(emailSent_Expected, emailSent_Actual);
	}
    
    @Test
    public void test_to_send_email_to_single_String_address() throws Exception {
    	boolean emailSent_Expected = true; 
    	populateDataForSingleStringRecipient();
    	mockMailTemplate();
    	boolean emailSent_Actual = emailService.sendEmail(emailTemplatePath, emailParams, singleRecipientString);
   
    	assertEquals(emailSent_Expected, emailSent_Actual);

    }
    
    @Test
    public void test_to_validate_Html_Email_generation() throws Exception {
    	testPreperationForGetEmailMethod();
    	parameters[0] = emailTemplatePath;
    	parameters[1] = emailParams;  		
    	mockMailTemplate();
    	HtmlEmail email = (HtmlEmail) getEmailMethod.invoke(emailService, parameters);  	
    	assertNotNull(email);
    }
   
    @PrepareForTest(MailTemplate.class)
    @Test
    public void test_to_validate_generated_Html_Email_Content() throws Exception{
    	String expected_fromAddress = emailParams.get("senderEmailAddress");
    	String expected_subject = "Greetings";
    	String expected_senderName = emailParams.get("senderName");
    	
    	testPreperationForGetEmailMethod();
    	
    	parameters[0] = emailTemplatePath;
    	parameters[1] = emailParams;
    	
    	mockMailTemplate();
    	
    	HtmlEmail email = (HtmlEmail) getEmailMethod.invoke(emailService, parameters);  
    	
    	String actual_fromAddress = email.getFromAddress().getAddress();
    	String actual_subject = email.getSubject();
    	String actual_senderName = email.getFromAddress().getPersonal();
    	
    	assertEquals(expected_fromAddress, actual_fromAddress);
    	assertEquals(expected_subject, actual_subject);
    	assertEquals(expected_senderName, actual_senderName);

    }
    
    private void mockMailTemplate() throws IOException {   	
	   	PowerMockito.mockStatic(MailTemplate.class); 
	   	InputStream inputstream = new FileInputStream(emailTemplatePath);
	   	MailTemplate mailTemplate = new MailTemplate(inputstream, "UTF-8");    
	   	when(MailTemplate.create(emailTemplatePath, session)).thenReturn(mailTemplate);
   }
    
    */
   
   @After
	public void tearDown() {
	   Mockito.reset();
	}
}
