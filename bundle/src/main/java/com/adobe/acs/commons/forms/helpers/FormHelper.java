package com.adobe.acs.commons.forms.helpers;

import com.adobe.acs.commons.forms.Form;
import org.apache.sling.api.resource.Resource;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public interface FormHelper extends AbstractFormHelper {
    public static final String SELECTOR = "post";
    public static final String EXTENSION = ".html";
    public static final String FORM_NAME_INPUT = "_form";

    /**
	 * Gets the From from either the POST Requests parameters or the GET
	 * request's (synthetic) attributes.
	 * 
	 * @param formName
	 * @param request
	 * @return
	 */
	public Form getForm(String formName, HttpServletRequest request);


	/**
	 * Returns a series of hidden fields used to persist multi-page form data
	 * between forms.
	 * 
	 * @param form
	 * @param keys
	 * @return
	 * @throws java.io.IOException
	 */
	public String getFormInputsHTML(Form form, String... keys)
			throws IOException;

    /**
     * Builds the form's action URI based on the current resource
     *
     * Appends ".post.html" to the resource's path.
     *
     * @param resource
     * @return
     */
    public String getAction(final Resource resource);
}