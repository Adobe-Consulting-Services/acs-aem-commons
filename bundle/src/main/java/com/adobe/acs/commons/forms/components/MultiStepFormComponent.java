package com.adobe.acs.commons.forms.components;

import com.adobe.acs.commons.forms.Form;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;

public interface MultiStepFormComponent {

	/**
	 * Get the data from the HTTP Request and move into the Map-based Form
	 * abstraction
	 * 
	 * @param request
	 * @return
	 */
	public Form getForm(SlingHttpServletRequest request, String step);
	
	/**
	 * Validate the provided form data. Create any Error records on the form
	 * itself.
	 * 
	 * @param form
	 * @return
	 */
	public Form validate(Form form, String step);

	/**
	 * Save the data to the underlying data store; implementation specific. This
	 * could be CRX or external data store.
	 * 
	 * @param form
	 * @return
	 */
	public boolean save(Form form, String step);

	/**
	 * Handle successful form submission. Typically includes a 302 redirect to a
	 * Success page.
	 * 
	 * @param form
	 * @param request
	 * @param response
	 */
	public void onSuccess(Form form, String step,
                          SlingHttpServletRequest request, SlingHttpServletResponse response)
			throws Exception;

	/**
	 * Handle unsuccessful form submission. Typically includes a 302 redirect
	 * back to self.
	 * 
	 * @param form
	 * @param request
	 * @param response
	 */
	public void onFailure(Form form, String step, SlingHttpServletRequest request,
                          SlingHttpServletResponse response) throws Exception;
}
