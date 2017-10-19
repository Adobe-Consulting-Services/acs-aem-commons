<%@ page session="false" contentType="text/html" pageEncoding="utf-8" %>
<%@include file="/libs/foundation/global.jsp" %>
<%@taglib prefix="sling2" uri="http://sling.apache.org/taglibs/sling" %>
<cq:setContentBundle />
<div class="coral-Well">
	<form action="${resource.path}" method="post" class="coral-Form--aligned" id="fn-acsCommons-remove_redirects_${resource.name}" ng-submit="postValues('fn-acsCommons-remove_redirects_${resource.name}')">
		
		<div class="coral-Form-fieldwrapper">
			<label class="coral-Form-fieldlabel">
				Protocol *
			</label>
			<input type="text" class="coral-Textfield" name="./protocol" value="${resource.valueMap.protocol}" required="required" />
		</div>
		
		<div class="coral-Form-fieldwrapper">
			<label class="coral-Form-fieldlabel">
				Domain *
			</label>
			<input type="text" class="coral-Textfield" name="./domain" value="${resource.valueMap.domain}" required="required" />
		</div>
		
		<div class="coral-Form-fieldwrapper">
			<label class="coral-Form-fieldlabel">
				Path *
			</label>
			<input type="text" class="coral-Textfield" name="./path" value="${resource.valueMap.path}" required="required" />
		</div>
		
		<div class="coral-Form-fieldwrapper">
			<label class="coral-Form-fieldlabel">
				Property *
			</label>
			<input type="text" class="coral-Textfield" name="./property" value="${resource.valueMap.property}" required="required" />
		</div>
		
		<div class="coral-Form-fieldwrapper" >
			<button class="coral-Button coral-Button--primary">Save</button>
		</div>
	</form>
	<form action="${resource.path}" method="post" class="coral-Form--aligned" id="fn-acsCommons-remove_redirects_${resource.name}" ng-submit="postValues('fn-acsCommons-remove_redirects_${resource.name}')">
		<input type="hidden"  name=":operation" value="delete" />
		<div class="coral-Form-fieldwrapper" >
			<button class="coral-Button coral-Button--warning">Remove</button>
		</div>
	</form>
</div>
<br/>