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

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.email.EmailService;
import com.adobe.acs.commons.wcm.AuthorUIHelper;
import com.day.cq.commons.Externalizer;
import com.day.cq.dam.commons.util.DamUtil;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowData;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.metadata.MetaDataMap;

/**
 * This abstract <code>SendTemplatedEmailProcess</code> class is a WorkFlow process step
 * that will send an email using {@link com.adobe.acs.commons.email.EmailService EmailService}.
 * By default jcr properties from the payload are automatically added to the email parameter
 * map. If the payload is a cq:Page then the properties at the jcr:content level
 * are added. If the payload is a dam:Asset then the properties at the metadata
 * node are added. In addition the parameters outlined in
 * {@link com.adobe.acs.commons.email.process.impl.SendTemplatedEmailConstants
 * SendTemplatedEmailConstants} are also automatically added. <br>
 * This process will send the email to a CQ user or members of a CQ group
 * specified by the process argument
 * {@link com.adobe.acs.commons.email.process.impl.SendTemplatedEmailProcess.Arguments#SEND_TO
 * SEND_TO} Implementing classes can override this logic by overriding the
 * method:
 * {@link com.adobe.acs.commons.email.process.impl.SendTemplatedEmailProcess#getEmailAddrs(com.day.cq.workflow.exec.WorkItem, org.apache.sling.api.resource.Resource, java.lang.String[])
 * getEmailAddrs() - method}<br>
 * Implementing classes can also add additional parameters by overriding the
 * {@link com.adobe.acs.commons.email.process.impl.SendTemplatedEmailProcess#getAdditionalParams(WorkItem, WorkflowSession, Resource)
 * getAdditionalParams() - method}
 * <p>
 * <p>
 * <p>
 * <b>Process Configuration</b> This process supports the following
 * configuration arguments:
 * <dl>
 * <dt><b>emailTemplate</b></dt>
 * <dd>String representing a path to the template to be used to send the email.
 * If no template is set no email is sent.</dd>
 * <dt><b>sendTo</b></dt>
 * <dd>String representing a path to a user or group. If the path is a user an
 * email will be sent to that user. if the path is to a group then the email
 * will be sent to all members of that group.</dd>
 * <dt><b>dateFormat</b></dt>
 * <dd>An optional parameter to specify how jcr Date properties are converted to
 * Strings. The format should be set using the
 * {@link java.text.SimpleDateFormat java.text.SimpleDateFormat}. Defaults to
 * <code>yyyy-MM-dd hh:mm a</code></dd>
 * </dl>
 *
 */
@Component
@Property(label = "Workflow Label", name = "process.label", value = "Send Templated Email", description = "Sends a templated email using the ACS Commons Email Service")
@Service
public class SendTemplatedEmailProcess implements WorkflowProcess {

    private static final Logger log = LoggerFactory.getLogger(SendTemplatedEmailProcess.class);

    /**
     * Service used to send the email
     */
    @Reference
    private EmailService emailService;

    /**
     * Service used to generate a link to the payload on author environment
     */
    @Reference
    private AuthorUIHelper authorUiHelper;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    /**
     * used to generate a link to the payload on publish environment
     */
    @Reference
    private Externalizer externalizer;

    /**
     * The available arguments to this process implementation.
     */
    protected enum Arguments {
        PROCESS_ARGS("PROCESS_ARGS"),
        /**
         * emailTemplate - process argument
         */
        TEMPLATE("emailTemplate"),
        /**
         * sendTo - process argument
         */
        SEND_TO("sendTo"),

        /**
         * dateFormat - process argument
         */
        DATE_FORMAT("dateFormat");

        private String argumentName;

        Arguments(String argumentName) {
            this.argumentName = argumentName;
        }

        public String getArgumentName() {
            return this.argumentName;
        }

    }

    @Override
    public final void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaData)
            throws WorkflowException {

        final WorkflowData workflowData = workItem.getWorkflowData();
        final String type = workflowData.getPayloadType();

        // Check if the payload is a path in the JCR
        if (!StringUtils.equals(type, "JCR_PATH")) {
            return;
        }

        String[] args = buildArguments(metaData);

        // process arguments
        String emailTemplate = getValueFromArgs(Arguments.TEMPLATE.getArgumentName(), args);

        if (emailTemplate == null) {
            log.warn("Invalid process arguments, returning");
            return;
        }

        // set date format to be used in emails
        String sdfParam = getValueFromArgs(Arguments.DATE_FORMAT.getArgumentName(), args);
        SimpleDateFormat sdf = getSimpleDateFormat(sdfParam);

        // Get the path to the JCR resource from the payload
        final String payloadPath = workflowData.getPayload().toString();

        // Get ResourceResolver
        final Map<String, Object> authInfo = new HashMap<String, Object>();
        authInfo.put(JcrResourceConstants.AUTHENTICATION_INFO_SESSION, workflowSession.getSession());

        try (ResourceResolver resourceResolver = resourceResolverFactory.getResourceResolver(authInfo) ) {
            Resource payloadRes = resourceResolver.getResource(payloadPath);

            // Email Parameter map
            Map<String, String> emailParams = new HashMap<String, String>();

            // Set jcr path
            emailParams.put(SendTemplatedEmailConstants.JCR_PATH, payloadPath);

            // Get Payload params
            Map<String, String> payloadProp = SendTemplatedEmailUtils.getPayloadProperties(payloadRes, sdf);
            if (payloadProp != null) {
                emailParams.putAll(payloadProp);
            }

            // Get Url params
            Map<String, String> urlParams = getUrls(payloadRes);
            emailParams.putAll(urlParams);

            // Get Additional Parameters to add
            Map<String, String> wfParams = getAdditionalParams(workItem, workflowSession, payloadRes);
            emailParams.putAll(wfParams);

            // get email addresses based on CQ user or group
            String[] emailTo = getEmailAddrs(workItem, payloadRes, args);

            List<String> failureList = emailService.sendEmail(emailTemplate, emailParams, emailTo);

            if (failureList.isEmpty()) {
                log.info("Email sent successfully to {} recipients", emailTo.length);
            } else {
                log.error("Email sent failed");
            }

        } catch (LoginException e) {
            log.error("Could not acquire a ResourceResolver object from the Workflow Session's JCR Session: {}", e);
        }
    }

    /***
     * Gets a String[] of email addresses to send the email to. By default calls
     * {@link com.adobe.acs.commons.email.process.impl.SendTemplatedEmailUtils#getEmailAddrsFromUserPath(ResourceResolver, String)}
     * Protected so that it can be overridden by implementing classes to add
     * unique logic to where emails are routed to.
     *
     * @param workItem
     *            the current WorkItem in the workflow
     * @param payloadResource
     *            the current payload as a Resource
     * @param args
     *            process arguments configured by the workflow step
     * @return String[] of email addresses
     */
    protected String[] getEmailAddrs(WorkItem workItem, Resource payloadResource, String[] args) {
        ResourceResolver resolver = payloadResource.getResourceResolver();
        String sendToUser = getValueFromArgs(Arguments.SEND_TO.getArgumentName(), args);
        return SendTemplatedEmailUtils.getEmailAddrsFromPathOrName(resolver, sendToUser);
    }

    /***
     * Returns a Map<String, String> of additional parameters that will be added
     * to the full list of email parameters that is sent to the EmailService. By
     * default adds the Workflow Title:
     * {@link com.adobe.acs.commons.email.process.impl.SendTemplatedEmailConstants#WF_MODEL_TITLE
     * WF_MODEL_TITLE} and adds the Workflow Step Title:
     * {@link com.adobe.acs.commons.email.process.impl.SendTemplatedEmailConstants#WF_STEP_TITLE
     * WF_STEP_TITLE}
     * {@link com.adobe.acs.commons.email.process.impl.SendTemplatedEmailConstants#WF_INITIATOR
     * WF_INITIATOR} Protected so that implementing classes can override and
     * add additional parameters.
     *
     * @param workItem
     * @param workflowSession
     * @param payloadResource
     * @return Map<String, String> of additional parameters to be added to email
     *         params
     */
    protected Map<String, String> getAdditionalParams(WorkItem workItem, WorkflowSession workflowSession,
            Resource payloadResource) {
        Map<String, String> wfParams = new HashMap<String, String>();

        try {
            wfParams.put(SendTemplatedEmailConstants.WF_STEP_TITLE, workItem.getNode().getTitle());
            wfParams.put(SendTemplatedEmailConstants.WF_MODEL_TITLE, workItem.getWorkflow().getWorkflowModel()
                    .getTitle());
            // Set workflow initiator
            wfParams.put(SendTemplatedEmailConstants.WF_INITIATOR, workItem.getWorkflow().getInitiator());
        } catch (Exception e) {
            log.warn("Error getting workflow title and workflow step title {}", e);
        }

        return wfParams;
    }

    /***
     * Gets value from workflow process arguments
     *
     * @param key
     * @param arguments
     * @return String of the argument value or null if not found
     */
    protected String getValueFromArgs(String key, String[] arguments) {
        for (String str : arguments) {
            String trimmedStr = str.trim();
            if (trimmedStr.startsWith(key + ":")) {
                return trimmedStr.substring((key + ":").length());
            }
        }
        return null;
    }

    /***
     * Uses the AuthorUIHelper to generate links to the payload on author Uses
     * Externalizer to generate links to the payload on publish
     *
     * @param payloadRes
     * @return
     */
    private Map<String, String> getUrls(Resource payloadRes) {

        Map<String, String> urlParams = new HashMap<String, String>();
        if (payloadRes == null) {
            return urlParams;
        }

        String payloadPath = payloadRes.getPath();
        ResourceResolver resolver = payloadRes.getResourceResolver();

        if (DamUtil.isAsset(payloadRes)) {
            // add author url
            String assetDetailsUrl = authorUiHelper.generateEditAssetLink(payloadPath, true, resolver);
            urlParams.put(SendTemplatedEmailConstants.AUTHOR_LINK, assetDetailsUrl);

            // add publish url
            String publishUrl = externalizer.publishLink(resolver, payloadPath);
            urlParams.put(SendTemplatedEmailConstants.PUBLISH_LINK, publishUrl);

        } else {

            // add absolute author url
            String assetDetailsUrl = authorUiHelper.generateEditPageLink(payloadPath, true, resolver);
            urlParams.put(SendTemplatedEmailConstants.AUTHOR_LINK, assetDetailsUrl);

            // add publish url
            String publishUrl = externalizer.publishLink(resolver, payloadPath + ".html");
            urlParams.put(SendTemplatedEmailConstants.PUBLISH_LINK, publishUrl);
        }

        return urlParams;
    }

    /***
     *
     * @param metaData
     * @return
     */
    private String[] buildArguments(MetaDataMap metaData) {
        // the 'old' way, ensures backward compatibility
        String processArgs = metaData.get(Arguments.PROCESS_ARGS.getArgumentName(), String.class);
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
        SimpleDateFormat defaultFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm a");

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

    @Activate
    protected void activate(ComponentContext context) throws RepositoryException {
        // activate
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        // deactivate
    }

}
