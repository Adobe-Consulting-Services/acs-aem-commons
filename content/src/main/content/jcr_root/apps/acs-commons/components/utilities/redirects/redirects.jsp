<%@ page session="false" contentType="text/html" pageEncoding="utf-8" %>
<%@include file="/libs/foundation/global.jsp" %>
<%@taglib prefix="sling2" uri="http://sling.apache.org/taglibs/sling" %>
<cq:setContentBundle />
<div class="coral-Well">
	<form action="${resource.path}" method="post" class="coral-Form--aligned" id="fn-acsCommons-remove_redirects_${resource.name}" ng-submit="postValues('fn-acsCommons-remove_redirects_${resource.name}')">
		
		<div class="coral-Form-fieldwrapper">
			<label class="coral-Form-fieldlabel">
				Scheme *
			</label>
			<div class="coral-InputGroup coral-Form-field">
				<input type="text" class="coral-Textfield" name="./protocol" value="${resource.valueMap.protocol}" required="required" />
			</div>
			<coral-icon class="coral-Form-fieldinfo coral-Icon coral-Icon--infoCircle coral-Icon--sizeS" icon="infoCircle" size="S" id="scheme-info" role="img" aria-label="info circle"></coral-icon>
			<coral-tooltip variant="info" placement="right" target="#scheme-info" class="coral3-Tooltip coral3-Tooltip--info" aria-hidden="true" tabindex="-1" role="tooltip" style="display: none;">
				<coral-tooltip-content>
					<fmt:message key="The request scheme, e.g. http or https, used to map the paths into published URLs" />
				</coral-tooltip-content>
			</coral-tooltip>
		</div>
		
		<div class="coral-Form-fieldwrapper">
			<label class="coral-Form-fieldlabel">
				Domain *
			</label>
			<div class="coral-InputGroup coral-Form-field">
				<input type="text" class="coral-Textfield" name="./domain" value="${resource.valueMap.domain}" required="required" />
			</div>
			<coral-icon class="coral-Form-fieldinfo coral-Icon coral-Icon--infoCircle coral-Icon--sizeS" icon="infoCircle" size="S" id="domain-info" role="img" aria-label="info circle"></coral-icon>
			<coral-tooltip variant="info" placement="right" target="#domain-info" class="coral3-Tooltip coral3-Tooltip--info" aria-hidden="true" tabindex="-1" role="tooltip" style="display: none;">
				<coral-tooltip-content>
					<fmt:message key="The domain name for the requests, used to map the paths into published URLs" />
				</coral-tooltip-content>
			</coral-tooltip>
		</div>
		
		<div class="coral-Form-fieldwrapper">
			<label class="coral-Form-fieldlabel">
				Path *
			</label>
			<div class="coral-InputGroup coral-Form-field">
				<input type="text" class="coral-Textfield" name="./path" value="${resource.valueMap.path}" required="required" />
			</div>
			<coral-icon class="coral-Form-fieldinfo coral-Icon coral-Icon--infoCircle coral-Icon--sizeS" icon="infoCircle" size="S" id="path-info" role="img" aria-label="info circle"></coral-icon>
			<coral-tooltip variant="info" placement="right" target="#path-info" class="coral3-Tooltip coral3-Tooltip--info" aria-hidden="true" tabindex="-1" role="tooltip" style="display: none;">
				<coral-tooltip-content>
					<fmt:message key="The path this configuration will look under in AEM to find cq:Page and dam:Assets with a non-null value for the redirect property" />
				</coral-tooltip-content>
			</coral-tooltip>
		</div>
		
		<div class="coral-Form-fieldwrapper">
			<label class="coral-Form-fieldlabel">
				Property *
			</label>
			<div class="coral-InputGroup coral-Form-field">
				<input type="text" class="coral-Textfield" name="./property" value="${resource.valueMap.property}" required="required" />
			</div>
			<coral-icon class="coral-Form-fieldinfo coral-Icon coral-Icon--infoCircle coral-Icon--sizeS" icon="infoCircle" size="S" id="property-info" role="img" aria-label="info circle"></coral-icon>
			<coral-tooltip variant="info" placement="right" target="#property-info" class="coral3-Tooltip coral3-Tooltip--info" aria-hidden="true" tabindex="-1" role="tooltip" style="display: none;">
				<coral-tooltip-content>
					<fmt:message key="Any non-null value will be treated as multi-valued and used as the redirect source for redirecting to the page / asset" />
				</coral-tooltip-content>
			</coral-tooltip>
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