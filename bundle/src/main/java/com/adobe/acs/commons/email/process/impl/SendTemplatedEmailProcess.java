/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2014 Adobe
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
package com.adobe.acs.commons.email.process.impl;

import javax.jcr.RepositoryException;

import com.adobe.acs.commons.email.process.AbstractSendTemplatedEmailProcess;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(label = "ACS AEM Commons - Workflow Process - Send Templated Email Workflow Process", description = "Uses the Email Service api to send an email based on workflow arguments")
@Properties({ @Property(label = "Workflow Label", name = "process.label", value = "Send Templated Email", description = "Sends a templated email using the ACS Commons Email Service") })
@Service
public class SendTemplatedEmailProcess extends AbstractSendTemplatedEmailProcess {
	
	private static final Logger log = LoggerFactory.getLogger(SendTemplatedEmailProcess.class);
	@Override
    protected void activate(ComponentContext context) throws RepositoryException {
    	//activate
		log.debug("Send Templated Email Workflow Process activated.");
    }

	@Override
    protected void deactivate(ComponentContext context) {
    	//deactivate
		log.debug("Send Templated Email Workflow Process deactivated.");
    }
	

}
