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

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import javax.jcr.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import org.apache.commons.mail.HtmlEmail;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import com.adobe.acs.commons.email.impl.EmailServiceImpl;
import com.day.cq.commons.mail.MailTemplate;
import com.day.cq.mailer.MessageGateway;
import com.day.cq.mailer.MessageGatewayService;

@RunWith(PowerMockRunner.class)
public class EmailServiceImplTest {

    @Mock
    MessageGatewayService messageGatewayService;
	
    @Mock
    ResourceResolverFactory resourceResolverFactory;
	
    @Mock
    MessageGateway<HtmlEmail> messageGateway;
	
    @Mock
    ResourceResolver resourceResolver;
	
    @Mock
    Session session;
	
    @InjectMocks
    private EmailServiceImpl emailService;
	
    private String emailTemplatePath;
    private Map<String, String> emailParams;
    private InternetAddress[] recipientsInternetAddressArray;
    private InternetAddress singleRecipientInternetAddress;
    private String[] recipientsStringArray;
    private String singleRecipientString;
    private Method getEmailMethod;
    private Class[] parameterTypes;
    private Object[] parameters;
    
    @Before
    public void setUp() throws Exception {
    
    	emailParams = new HashMap<String, String>();
    	
    	when(messageGatewayService.getGateway(HtmlEmail.class)).thenReturn(messageGateway);
    	when(resourceResolverFactory.getAdministrativeResourceResolver(null)).thenReturn(resourceResolver);
    	when(resourceResolver.adaptTo(Session.class)).thenReturn(session);
    	
    	populateTestData();   	
    	
    }
 
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
    
    @PrepareForTest(MailTemplate.class)
    @Test
    public void test_to_send_email_to_multiple_Internet_addressess() throws Exception {
    	boolean emailSent_Expected = true;   
    	populateDataForMultiInternetAddressRecipients();   	
    	mockMailTemplate();  
    	boolean emailSent_Actual = emailService.sendEmail(emailTemplatePath, emailParams, recipientsInternetAddressArray); 	

		assertEquals(emailSent_Expected, emailSent_Actual);
    	
    }
    
    @PrepareForTest(MailTemplate.class)
    @Test
    public void test_to_send_email_to_single_Internet_address() throws Exception {
    	boolean emailSent_Expected = true;  
    	populateDataForSingleInternetAddressRecipient();
    	mockMailTemplate();
    	boolean emailSent_Actual = emailService.sendEmail(emailTemplatePath, emailParams, singleRecipientInternetAddress);
    	
    	assertEquals(emailSent_Expected, emailSent_Actual);
    }
    
    @PrepareForTest(MailTemplate.class)
    @Test
	public void test_to_send_email_to_multiple_String_addresses() throws Exception {
    	boolean emailSent_Expected = true; 
    	populateDataForMultiStringRecipient();
    	mockMailTemplate();
    	boolean emailSent_Actual = emailService.sendEmail(emailTemplatePath, emailParams, recipientsStringArray);
    	
    	assertEquals(emailSent_Expected, emailSent_Actual);
	}
    
    @PrepareForTest(MailTemplate.class)
    @Test
    public void test_to_send_email_to_single_String_address() throws Exception {
    	boolean emailSent_Expected = true; 
    	populateDataForSingleStringRecipient();
    	mockMailTemplate();
    	boolean emailSent_Actual = emailService.sendEmail(emailTemplatePath, emailParams, singleRecipientString);
    	
    	assertEquals(emailSent_Expected, emailSent_Actual);

    }
    
    @PrepareForTest(MailTemplate.class)
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
	   	PowerMock.mockStatic(MailTemplate.class); 
	   	InputStream inputstream = new FileInputStream(emailTemplatePath);
	   	MailTemplate mailTemplate = new MailTemplate(inputstream, "UTF-8");  	
	   	expect(MailTemplate.create(emailTemplatePath, session)).andReturn(mailTemplate);      	
	   	PowerMock.replay(MailTemplate.class);
   }
    
 
   
   @After
	public void tearDown() {
	     
	}
}
