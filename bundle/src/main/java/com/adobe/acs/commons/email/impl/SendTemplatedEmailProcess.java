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

import com.adobe.acs.commons.email.EmailService;
import com.adobe.acs.commons.email.SendTemplatedEmailConstants;
import com.adobe.acs.commons.wcm.AuthorUIHelper;
import com.day.cq.commons.Externalizer;
import com.day.cq.dam.commons.util.DamUtil;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowData;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.metadata.MetaDataMap;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component(label = "ACS AEM Commons - Workflow Process - Send Templated Email Workflow Process", description = "Uses the Email Service api to send an email based on workflow arguments")
@Properties({ @Property(label = "Workflow Label", name = "process.label", value = "Send Templated Email", description = "Sends a templated email using the ACS Commons Email Service") })
@Service
public class SendTemplatedEmailProcess implements WorkflowProcess {
	private static final Logger log = LoggerFactory
			.getLogger(SendTemplatedEmailProcess.class);

	private static final String AUTHENTICATION_INFO_SESSION = "user.jcr.session";

	@Reference
	private EmailService emailService;

	@Reference
	private AuthorUIHelper authorUIHelper;

	@Reference
	private ResourceResolverFactory resourceResolverFactory;

	@Reference
	private Externalizer externalizer;

	private SimpleDateFormat sdf;

	/**
	 * The available arguments to this process implementation.
	 */
	protected enum Arguments {
		PROCESS_ARGS("PROCESS_ARGS"), SEND_TO("sendTo"), TEMPLATE(
				"emailTemplate"), DATE_FORMAT("dateFormat");

		private String argumentName;

		Arguments(String argumentName) {
			this.argumentName = argumentName;
		}

		public String getArgumentName() {
			return this.argumentName;
		}

	}

	@Override
	public final void execute(WorkItem workItem,
			WorkflowSession workflowSession, MetaDataMap metaData)
			throws WorkflowException {

		final WorkflowData workflowData = workItem.getWorkflowData();
		final String type = workflowData.getPayloadType();

		// Check if the payload is a path in the JCR
		if (!StringUtils.equals(type, "JCR_PATH")) {
			return;
		}

		String[] args = buildArguments(metaData);

		// process arguments
		String sendToUser = getValueFromArgs(
				Arguments.SEND_TO.getArgumentName(), args);
		String emailTemplate = getValueFromArgs(
				Arguments.TEMPLATE.getArgumentName(), args);

		if (sendToUser == null || emailTemplate == null) {
			log.warn("Invalid process arguments, returning");
			return;
		}

		// set date format to be used in emails
		String sdfParam = getValueFromArgs(
				Arguments.DATE_FORMAT.getArgumentName(), args);
		sdf = getSimpleDateFormat(sdfParam);

		// Get the path to the JCR resource from the payload
		final String payloadPath = workflowData.getPayload().toString();

		// Get ResourceResolver
		final Map<String, Object> authInfo = new HashMap<String, Object>();
		authInfo.put(AUTHENTICATION_INFO_SESSION, workflowSession.getSession());
		final ResourceResolver resourceResolver;

		try {
			resourceResolver = resourceResolverFactory
					.getResourceResolver(authInfo);
			Resource payloadRes = resourceResolver.getResource(payloadPath);

			// Email Parameter map
			Map<String, String> emailParams = new HashMap<String, String>();

			// Set jcr path
			emailParams.put(SendTemplatedEmailConstants.JCR_PATH, payloadPath);

			// Get Payload params
			Map<String, String> payloadProp = SendTemplatedEmailHelper
					.getPayloadProperties(payloadRes, sdf);
			if (payloadProp != null) {
				emailParams.putAll(payloadProp);
			}

			// Get Url params
			Map<String, String> urlParams = getURLs(payloadRes);
			if (urlParams != null) {
				emailParams.putAll(urlParams);
			}

			 //Get Workflow params Map<String, String> urlParams =
			 Map<String, String> wfParams = getWorkflowParams(workItem); 
			 if(wfParams != null) {
				 emailParams.putAll(wfParams); 
			  }

			// get email addresses based on CQ user or group
			String[] emailTo = SendTemplatedEmailHelper.getEmailAddrs(
					resourceResolver, sendToUser);

			List<String> failureList = emailService.sendEmail(emailTemplate,
					emailParams, emailTo);

			if (failureList.isEmpty()) {
				log.info("Email sent successfully to {} recipients",
						emailTo.length);
			} else {
				log.error("Email sent failed");
			}

		} catch (LoginException e) {
			log.error(
					"Could not acquire a ResourceResolver object from the Workflow Session's JCR Session: {}",
					e);
		}
	}

	private Map<String, String> getWorkflowParams(WorkItem workItem) {
		Map<String, String> wfParams = new HashMap<String, String>();
		
		try {
			wfParams.put(SendTemplatedEmailConstants.WF_STEP_TITLE, workItem.getNode().getTitle());
			wfParams.put(SendTemplatedEmailConstants.WF_MODEL_TITLE, workItem.getWorkflow().getWorkflowModel().getTitle());
		} catch (Exception e) {
			log.warn("Error getting workflow title and workflow step title {}" , e);
		}
		
		return wfParams;
	}

	private Map<String, String> getURLs(Resource payloadRes) {

		Map<String, String> urlParams = new HashMap<String, String>();
		if (payloadRes == null) {
			return urlParams;
		}

		String payloadPath = payloadRes.getPath();
		ResourceResolver resolver = payloadRes.getResourceResolver();

		if (DamUtil.isAsset(payloadRes)) {
			// add author url
			String assetDetailsUrl = authorUIHelper.generateEditAssetLink(
					payloadPath, true, resolver);
			urlParams.put(SendTemplatedEmailConstants.AUTHOR_LINK, assetDetailsUrl);

			// add publish url
			String publishUrl = externalizer.publishLink(resolver, payloadPath);
			urlParams.put(SendTemplatedEmailConstants.PUBLISH_LINK, publishUrl);

		} else {

			// add absolute author url
			String assetDetailsUrl = authorUIHelper.generateEditPageLink(
					payloadPath, true, resolver);
			urlParams.put(SendTemplatedEmailConstants.AUTHOR_LINK, assetDetailsUrl);

			// add publish url
			String publishUrl = externalizer.publishLink(resolver, payloadPath
					+ ".html");
			urlParams.put(SendTemplatedEmailConstants.PUBLISH_LINK, publishUrl);
		}

		return urlParams;
	}

	private String getValueFromArgs(String key, String arguments[]) {
		for (String str : arguments) {
			String trimmedStr = str.trim();
			if (trimmedStr.startsWith(key + ":")) {
				return trimmedStr.substring((key + ":").length());
			}
		}
		return null;
	}

	/***
	 * 
	 * @param metaData
	 * @return
	 */
	private String[] buildArguments(MetaDataMap metaData) {
		// the 'old' way, ensures backward compatibility
		String processArgs = metaData.get(
				Arguments.PROCESS_ARGS.getArgumentName(), String.class);
		if (processArgs != null && !processArgs.equals("")) {
			return processArgs.split(",");
		} else {
			return new String[0];
		}
	}

	/***
	 * Set the format to be used for displaying dates in the email Defaults to
	 * format of 'yyyy-MM-dd hh:mm a'
	 * 
	 * @param formatString
	 *            - workflow process argument to override default format
	 * @return SimpleDateFormat that will be used to convert jcr Date properties
	 *         to Strings
	 */
	private SimpleDateFormat getSimpleDateFormat(String formatString) {
		SimpleDateFormat defaultFormat = new SimpleDateFormat(
				"yyyy-MM-dd hh:mm a");

		if (formatString == null || formatString.isEmpty()) {
			return defaultFormat;
		}

		try {
			return new SimpleDateFormat(formatString);
		} catch (IllegalArgumentException e) {
			// invalid pattern
			return defaultFormat;
		}
	}

}
