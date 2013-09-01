# Forms

## Purpose

Provide an abstraction for managing the submission of statically defined HTML Forms.

### Form.java

Form is a class that represents an HTML form submission data:

* name: the form's conceptual name; acts as a UID when multiple forms are on a page (ex. login, registration)
* data: which holds submitted form data
* errors: which holds error messaging associated with form field

### Form Helpers

Form objects represent the the form data; Helpers interact with Form objects.

#### Forward Form Helper

The Forward Form Helper is used when Forms need to redirect (Forward-as-GET) internally to render end state of forms.

Forward Form Helper requests the target resource as an internal Synthetic GET Request, and passes the Form object as a SlingHttpServletRequest attribute.

Key features/use-cases:

* Form-payload is too large to be transferred via GET Query Params (to render error page)
* You aren't uncomfortable exposing for- data as clear text in query params (even though they fall under SSL envelope)
* You don't mind resubmitting forms when a user clicked "refresh"
* Keeps a "clean" address bar in the browser

#### PRG Form Helper (POST-Redirect-GET)

The PRG Form Helper is used when Forms need to redirect externally (302) to render either

PRG Form Helper requests the target resource as a 302 Redirect, serialized the Form data, data pass it as GET Query Parameters. This works well when Form data is under 2000 characters in total.

Key features/use-cases:

* Form-payload is too small-ish (< 2000 chars encoded)
* You like a clean separation between your GET and POST requests
* Don't mind a messy address bar in the browser (errors are returned to form via GET QPs)

Remember: GET Query Parameters are passed WITHIN the HTTPS envelope, so they are not visible on the wire

## Sample Implementation

This example used the PRGFormHelper, however this can easily be swapped out for the ForwardFormHelper;
The main difference is how errors are sent back to the original form

Forward-as-GET
    `formHelper.forwardAsGet(...)` in `post.POST.jsp`

vs.

POST-redirect-GET
    `formHelper.sendRedirect(...)` in `post.POST.jsp`


There is also a normalized FormHelper API that wraps common usecases in .renderForm(..) and .renderOtherForm(...) methods.
Generally these methods can be used, and `.sendRedrect(..)` and `.forwardAsGet(..)` can be reserved for unusual use-cases where even more control is required.


### CQ5 Component

#### /apps/acme/components/demo

#### demo.jsp



    <%@include file="/libs/foundation/global.jsp"%>
    <%@page session="false"
        import="com.adobe.acs.commons.forms.*,
                com.adobe.acs.commons.forms.helpers.*"%>

    <%-- ***********************************************************************************************************


         Initialize the Form object with the slingRequest.

         This will intelligently look for the Form data from POST Parameters, GET Query Parameters,
         or HttpServletRequest Attributes depending on the context and FormHelper used.

         If the request is a "fresh" request to the page, the form and its errors will be empty.

         Changing between Form strategies (PRG vs Fwd-as-GET) is as easy as swapping out the FormHelper as shown below.

         Also, don't forget to make them the same in post.POST.jsp;

         IMPORTANT: FormHelper Services should be sync'd between the form rendering JSP and the POST handler JSP.

    ************************************************************************************************************ --%>

    <%

    // FormHelper formHelper = sling.getService(PostRedirectGetFormHelper.class);
    FormHelper formHelper = sling.getService(ForwardAsGetFormHelper.class);

    Form form = formHelper.getForm("demo", slingRequest);

    %>

    <%-- ***********************************************************************************************************

        Check if the form has any errors, and display a list of all the error messages.

    ************************************************************************************************************ --%>

    <% if(form.hasErrors()) { %>
    	<h2 class="alert">Your form has errors!</h2><%	}
    %>

    <%-- ***********************************************************************************************************

    	Set the form to POST back to the component; use formHelper.getAction(..) to add the suffix that is
    	registered with the POST handler. Defaults to /acs/form -- can change via OSGi Configuration on:

    	    * com.adobe.acs.commons.forms.helpers.impl.PostFormHelperImpl#prop.form-suffix

    ************************************************************************************************************ --%>

    <form method="post" action="<%= formHelper.getAction(currentPage) %>">
    <%= formHelper.getFormInputsHTML(form) %>

    <%-- ***********************************************************************************************************

        Optionally specify the selector used to resolve the script handler
        for this POST request. If not used defaults to "post" (to resolve to post.POST.jsp).

        Useful for multi-page form wizards.

    ************************************************************************************************************ --%>

    <%= formHelper.getFormSelectorInputHTML("post") %>

    <fieldset>
    	<legend>Form Demo</legend>

         <%-- *******************************************************************************************************

    	    You can check for specific error messages..

            Below we use Server-provided error messages via: form.getError("myField")
        	and apply an "error" CSS Class to the label.

         ******************************************************************************************************* --%>

        <div><%= form.getError("myField") %></div>
    	<label <%= form.hasError("myField") ? "class=\"error\"" : "" %>>My Field:</label>

         <%-- *******************************************************************************************************

    		The form fields can be "re-populated" with submitted values by setting the
            `value` attribute of the input field.

         ******************************************************************************************************* --%>

    	<input type="text" name="myField" value="<%= form.get("myField") %>"/>

    	<input type="submit" value="Submit"/>
    </fieldset>
    </form>



#### post.POST.jsp

Note the naming convention of post.POST.jsp; Unless overridden using `formHelper.getFormSelectorInputHTML("custom")`
in `formdemo.jsp`, the selector `post` is used.


    <%@include file="/libs/foundation/global.jsp"%>
    <%@page session="false"
        import="com.adobe.acs.commons.forms.*,
    	    	com.adobe.acs.commons.forms.helpers.*"%>

    <%-- ***********************************************************************************************************

         Changing between Form strategies (PostRedirectGet vs ForwardAsGet) is as
         easy as swapping out the FormHelper as show below.
         Or for the common case, use generic FormHelper with .renderForm(..)

    ************************************************************************************************************ --%>

    <%

    //PostRedirectGetFormHelper formHelper = sling.getService(ForwardAsGetFormHelper.class);
    //ForwardAsGetFormHelper formHelper = sling.getService(ForwardAsGetFormHelper.class);

	// FormHelper formHelper = sling.getService(PostRedirectGetFormHelper.class);
	FormHelper formHelper = sling.getService(ForwardAsGetFormHelper.class);


    //  Get the Form
    Form form = formHelper.getForm("demo", slingRequest);

	// Validation should be moved to a supporting OSGi Service - placement only for illustration
	if(form.get("myField") != null && form.get("myField").length() > 10) {
    	// Data is all good!
    } else {
        form.setError("myField", "What kind of answer is: " + form.get("myField"));
    }

    if(form.hasErrors()) {
    %>
    <%-- *******************************************************************************************************

        Choose the return-to-form method based on the Form strategy.
        You can use PostRedirectGetFormHelper.sendRedirect(..) or ForwardAsGetFormHelper.forwardAsGet(..) variations
        if the FormHelper.renderForm(..) variations are sufficient (renderForm covers the 80% usecase)

        You'll want to match currentPage/resource/path up to form.getAction(..) in formdemo.jsp

    ******************************************************************************************************* --%>
    <%
         formHelper.renderForm(form, currentPage, slingRequest, slingResponse);

    } else {
		// Save the data; or whatever you want
        slingResponse.sendRedirect("/content/success.html");
    }
    %>