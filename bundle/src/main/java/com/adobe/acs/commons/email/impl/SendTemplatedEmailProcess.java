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
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import com.day.cq.commons.Externalizer;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.dam.commons.util.DamUtil;
import com.day.cq.wcm.api.Page;
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
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Component(
        label = "ACS AEM Commons - Workflow Process - Send Templated Email Workflow Process",
        description = "Uses the Email Service api to send an email based on workflow arguments"
)
@Properties({
    @Property(
            label = "Workflow Label",
            name = "process.label",
            value = "Send Templated Email",
            description = "Sends a templated email using the ACS Commons Email Service"
    )
})
@Service
public class SendTemplatedEmailProcess implements WorkflowProcess {
    private static final Logger log = LoggerFactory.getLogger(SendTemplatedEmailProcess.class);

    private static final String AUTHENTICATION_INFO_SESSION = "user.jcr.session";
    
    private static final String ASSET_DETAILS_URL = "/assetdetails.html";
    
    private static final String EDITOR_URL = "/editor.html";
    
    private static final String AUTHOR_LINK = "authorLink";
    
    private static final String PUBLISH_LINK = "publishLink";
    
    private static final String PN_USER_EMAIL = "profile/email";

    @Reference
    private EmailService emailService;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;
    
    @Reference
    private Externalizer externalizer;
    
    private SimpleDateFormat sdf;

    /**
     * The available arguments to this process implementation.
     */
    public enum Arguments {
        PROCESS_ARGS("PROCESS_ARGS"), SEND_TO("sendTo"), TEMPLATE("emailTemplate"), 
        DATE_FORMAT("dateFormat"), CLASSIC_UI("classicUI");

        private String argumentName;

        Arguments(String argumentName) {
            this.argumentName = argumentName;
        }

        public String getArgumentName() {
            return this.argumentName;
        }

    }
    
    @Override
    public final void execute(WorkItem workItem, WorkflowSession workflowSession,
                         MetaDataMap metaData) throws WorkflowException {
    	
        final WorkflowData workflowData = workItem.getWorkflowData();

        final String type = workflowData.getPayloadType();
  
        // Check if the payload is a path in the JCR
        if (!StringUtils.equals(type, "JCR_PATH")) {
            return;
        }
        
        String[] args = buildArguments(metaData);
       
        //process arguments
    	String sendToUser =  getValueFromArgs(Arguments.SEND_TO.getArgumentName(), args); 
    	String emailTemplate = getValueFromArgs(Arguments.TEMPLATE.getArgumentName(), args); 
    	
    	if(sendToUser == null || emailTemplate == null) {
    		log.warn("Invalid process arguments, returning");
    		return;
    	}
    	
    	 
        //set date format to be used in emails
    	String sdfParam = getValueFromArgs(Arguments.DATE_FORMAT.getArgumentName(), args);
        sdf = getSimpleDateFormat(sdfParam);
        
        // Get the path to the JCR resource from the payload
        final String payloadPath = workflowData.getPayload().toString();

        // Get ResourceResolver
        final Map<String, Object> authInfo = new HashMap<String, Object>();
        authInfo.put(AUTHENTICATION_INFO_SESSION, workflowSession.getSession());
        final ResourceResolver resourceResolver;
 
        try {
            resourceResolver = resourceResolverFactory.getResourceResolver(authInfo);
            Resource payloadRes = resourceResolver.getResource(payloadPath);
            
        	//get email addresses based on CQ user or group
        	String[] emailTo = getEmailAddrs(resourceResolver, sendToUser);
            
            //Email Parameter map
            Map<String, String> emailParams = new HashMap<String, String>();
            
            //Check if the payload is an asset 
            if(DamUtil.isAsset(payloadRes)) {
   
            	//get metadata resource
            	Resource mdRes = payloadRes.getChild(JcrConstants.JCR_CONTENT 
            						+ "/" + DamConstants.METADATA_FOLDER);
            	
            	Map<String, String> assetMetadata = getJcrKeyValuePairs(mdRes);
            	emailParams.putAll(assetMetadata);
            	
            	//add author url
            	String assetDetailsUrl = externalizer.authorLink(null, ASSET_DETAILS_URL 
            								 + payloadPath);
            	emailParams.put(AUTHOR_LINK, assetDetailsUrl);
            	
            	//add publish url
            	String publishUrl = externalizer.publishLink(null, payloadPath);
            	emailParams.put(PUBLISH_LINK, publishUrl);
            	
            } else {
            	//check if the payload is a page
            	Page payloadPage = payloadRes.adaptTo(Page.class);
            	
            	if(payloadPage != null) {
                   	Map<String, String> pageContent = getJcrKeyValuePairs(payloadPage.getContentResource());
                   	emailParams.putAll(pageContent);
                   	
                   	//add absolute author url
                	String assetDetailsUrl = externalizer.authorLink(null, EDITOR_URL 
                								+ payloadPath + ".html");
                	emailParams.put(AUTHOR_LINK, assetDetailsUrl);
                	
                	//add publish url
                	String publishUrl = externalizer.publishLink(null, payloadPath + ".html");
                	emailParams.put(PUBLISH_LINK, publishUrl);
            	}
            }
            
            //emailParams.put("senderEmailAddress","abcd@example.com");  
            //emailParams.put("senderName","David Smith");
     
            emailService.sendEmail(emailTemplate, emailParams, emailTo);
           
          
        } catch (LoginException e) {
            log.error("Could not acquire a ResourceResolver object from the Workflow Session's JCR Session: {}", e);
        } catch (RepositoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    /***
     * Gets email(s) based on the path to an principal
     * If the path is a user
     * it returns an array with a single email
     * if the path is a group returns an array emails for
     * each individual in the group
     * @param resourceResolver
     * @param sendToPath
     * @return - String[] of email(s) associated with account
     * @throws RepositoryException
     */
	private String[] getEmailAddrs(ResourceResolver resourceResolver, String userPath) throws RepositoryException {
		List<String> emailList = new LinkedList<String>();
		
		Resource authRes = resourceResolver.getResource(userPath);
		
		if(authRes != null) {
			Authorizable authorizable = authRes.adaptTo(Authorizable.class);
			if(authorizable != null) {
				 //check if it is a group
				if(authorizable.isGroup()) {
					Group authGroup = authRes.adaptTo(Group.class);
					
					//iterate over members of the group and add emails
					Iterator<Authorizable> memberIt =  authGroup.getMembers();
					while(memberIt.hasNext()) {
						String currEmail = getAuthorizableEmail(memberIt.next());
						if(currEmail != null)
							emailList.add(currEmail);
					}
				} else {
					//otherwise is an individual user
					String authEmail = getAuthorizableEmail(authorizable);
					if(authEmail != null) 
						emailList.add(authEmail);
				}
			}
		}
		
		String[] emailReturn = new String[emailList.size()];
		return emailList.toArray(emailReturn);
	}
	
	private String getAuthorizableEmail(Authorizable authorizable) throws RepositoryException {
		if(authorizable.hasProperty(PN_USER_EMAIL)) {
			Value[] emailVal = authorizable.getProperty(PN_USER_EMAIL);
			return emailVal[0].getString();
		}
		
		return null;
	}

	/***
     * Method to add all properties of a resource to Key/Value map of strings only
     * 
     * @param resource
     * @return a string map where the key is the jcr property and the value is 
     * @throws RepositoryException 
     * @throws JSONException 
     */
	private Map<String, String> getJcrKeyValuePairs(Resource resource) {
		
		Map<String, String> returnMap = new HashMap<String, String>();
		
		if(resource == null) {
			return returnMap;
		}
		
		ValueMap resMap = ResourceUtil.getValueMap(resource);
		
		for(Map.Entry<String, Object> entry: resMap.entrySet()) {
			
			Object value = entry.getValue();
			
			if(value instanceof Calendar) {
				//Date property
				String fmtDate = formatDate((Calendar) value);
				returnMap.put(entry.getKey(), fmtDate);
			} else if (value instanceof String[]) {
				//concatenate string array
				String strValue = concatStrings((String[]) value);
				returnMap.put(entry.getKey(), strValue);
				
			} else {
				//all other properties just use default to string
				returnMap.put(entry.getKey(), value.toString());
			}
		}

		return returnMap;
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
        String processArgs = metaData.get(Arguments.PROCESS_ARGS.getArgumentName(), String.class);
        if (processArgs != null && !processArgs.equals("")) {
            return processArgs.split(",");
        } else {
            return new String[0];
        }
    }
	
	/***
	 * Concatenates the values of a string array into a 
	 * comma separated list.
	 * @param strArray
	 * @return
	 */
	private String concatStrings(String[] strArray) {
		String returnStr = "";
		
		for(String valString : strArray) {
			returnStr += valString + ", ";
		}
		
		if(returnStr.length() > 2) {
			//remove trailing comma
			return returnStr.substring(0, returnStr.length() -2);
		}
		return returnStr;
	}
	
	/***
	 * Set the format to be used for displaying dates in the email
	 * Defaults to format of 'yyyy-MM-dd hh:mm a'
	 * 
	 * @param formatString - workflow process argument to override default format
	 * @return SimpleDateFormat that will be used to convert jcr Date properties to Strings
	 */
	private SimpleDateFormat getSimpleDateFormat(String formatString) {
		SimpleDateFormat defaultFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm a");
		
		if(formatString == null || formatString.isEmpty()) {
			return defaultFormat;
		}
		
		try {
		    return new SimpleDateFormat(formatString);
		} catch (IllegalArgumentException e) {
		    // invalid pattern
			return defaultFormat;
		}
	}
	
	/***
	 * Format date as a string using global variable
	 * sdf
	 * @param calendar
	 * @return
	 */
	private String formatDate(Calendar calendar) {
		
		return sdf.format(calendar.getTime());
	}
	
}
