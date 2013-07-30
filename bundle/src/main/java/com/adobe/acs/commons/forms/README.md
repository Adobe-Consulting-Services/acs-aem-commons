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

The Forward Form Helper is used when Forms need to redirect internally to render either

1. The same form but with error messages
2. A multi-step form (form wizard)

Forward Form Helper requests the target resource as an internal Synthetic GET Request, and passes the Form object as a HttpServletRequest attribute.

*Note: BrowserMap JS existing on the page with the form seems to have odd side-effects (JS-based auto-redirect) when using FowardFormHelper. PRGFormHelper works fine.

#### PRG Form Helper (POST-Redirect-GET)

The PRG Form Helper is used when Forms need to redirect externally (302) to render either

1. The same form but with error messages
2. A multi-step form (form wizard)

PRG Form Helper requests the target resource as a 302 Redirect, serialized the Form data, data pass it as GET Query Parameters. This works well when Form data is under 2000 characters in total.

*** Remember: GET Query Parameters are passed WITHIN the HTTPS envelope, so they are not visible on the wire ***



## Sample Implementation

This example used the PRGFormHelper, however this can easily be swapped out for the ForwardFormHelper; The main change would be calling: `formHelper.forwardAsGet(form, currentPage, slingRequest, slingResponse)` in `post.POST.jsp` (see below).

### CQ5 Component

#### /apps/acme/components/demo

#### demo.jsp

    <%@include file="/libs/foundation/global.jsp"%>
    <%@page session="false"
        import="com.adobe.acs.commons.forms.*,
                com.adobe.acs.commons.forms.helpers.*"%><%
    %><%--
    	 Initialize the Form object with the slingRequest.

    	 This will intelligent look for the Form data from POST Parameters,
    	 GET Query Parameters, or HttpServletRequest Attributes,
    	 depending on the FormHelper used.

    	 If the request is a "fresh" request to the page,
    	 the form and its errors will be empty.

         Changing between Form strategies (PRG vs Fwd-as-GET) is as
         easy as swapping out the FormHelper as show below.
         Also, don't forget to make them the same in post.POST.jsp.
    --%>

    <%
        //ForwardFormHelper formHelper = sling.getService(ForwardFormHelper.class);
        PRGFormHelper formHelper = sling.getService(PRGFormHelper.class);
    	Form form = formHelper.getForm("demo", slingRequest);
    %>

    <%--
    	Check if the form has any errors,
    	and display a list of all the error messages.
    --%>
    <% if(form.hasErrors()) { %>
    	<h2 class="alert">Your form has errors!</h2><%	}
    %>

    <%--
    	Set the form to POST back to the component
    --%>
    <form method="post" action="<%= formHelper.getAction(resource) %>">
    <%= formHelper.getFormInputsHTML(form) %>
    <fieldset>
    	<legend>Form Demo</legend>

    	<%--
    	    You can check for specific error messages..

            Below we use Server-provided error messages via: form.getError("myField")
        	and apply an "error" CSS Class to the label.
    	--%>

        <div><%= form.getError("myField") %></div>
    	<label <%= form.hasError("myField") ? "class=\"error\"" : "" %>>My Field:</label>

    	<%--
    		The form fields can be "re-populated" with submitted values by setting the
            `value` attribute of the input field.
    	--%>
    	<input type="text" name="myField" value="<%= form.get("myField") %>"/>

    	<input type="submit" value="Submit"/>
    </fieldset>
    </form>


#### post.POST.jsp

Note the naming convention of post.POST.jsp; `form.getAction(resource)` returns: `resource.getPath() + ".post.html"`

    <%@include file="/libs/foundation/global.jsp"%>
    <%@page session="false"
        import="com.adobe.acs.commons.forms.*,
    	    	com.adobe.acs.commons.forms.helpers.*"%><%

    <%--
         Changing between Form strategies (PRG vs Fwd-as-GET) is as
         easy as swapping out the FormHelper as show below.
         Also, don't forget to make them the same in post.POST.jsp.
    --%>

    //ForwardFormHelper formHelper = sling.getService(ForwardFormHelper.class);
	PRGFormHelper formHelper = sling.getService(PRGFormHelper.class);

    Form form = formHelper.getForm("demo", slingRequest);

	if(form.get("myField") != null && form.get("myField").length() > 100) {
    	// Data is all good!
    } else {
        form.setError("myField", "What kind of answer is:" + form.get("myField"));
    }

    if(form.hasErrors()) {
        <%--
            Choose the return-to-form method based on the Form strategy.
            Uncommented line used PRG, commented uses Fwd-as-GET
        --%>
        formHelper.sendRedirect(form, currentPage, slingResponse);
        //formHelper.forwardAsGet(form, currentPage, slingRequest, slingResponse);

    } else {
		// Save the data; or whatever you want with it
        slingResponse.sendRedirect("/content/success.html");
    }

    return;
    %>
