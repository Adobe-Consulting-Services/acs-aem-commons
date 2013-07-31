package com.adobe.acs.commons.forms.helpers;

import com.adobe.acs.commons.forms.Form;
import com.day.cq.wcm.api.Page;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;

import java.io.IOException;

public interface FormHelper extends PostFormHelper {
    public static final String SELECTOR = "post";
    public static final String EXTENSION = ".html";
    public static final String FORM_NAME_INPUT = ":form";
    public static final String FORM_RESOURCE_INPUT = ":formResource";

    /**
	 * Gets the From from either the POST Requests parameters or the GET
	 * request's (synthetic) attributes.
	 * 
	 * @param formName
	 * @param request
	 * @return
	 */
	public Form getForm(String formName, SlingHttpServletRequest request);


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
     * Builds the form's action URI based on the provided resource's path
     *
     * Appends ".post.html" to the resource's path.
     *
     * @param resource
     * @return
     */
    public String getAction(final Resource resource);

    /**
     * Builds the form's action URI based on the provided page's path
     *
     * Appends ".html/<suffix>" to the resource's path.
     *
     * @param page
     * @return
     */
    public String getAction(final Page page);

    /**
     * Builds the form's action URI based on the provided path
     *
     * Appends ".html/<suffix>" to the resource's path.
     *
     * @param path
     * @return
     */
    public String getAction(final String path);
}