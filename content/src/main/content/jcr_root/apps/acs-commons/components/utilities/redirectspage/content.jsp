<%@ page session="false" contentType="text/html" pageEncoding="utf-8" %>
<%@include file="/libs/foundation/global.jsp" %>
<%@taglib prefix="sling2" uri="http://sling.apache.org/taglibs/sling" %>
<cq:setContentBundle />
<div ng-controller="MainCtrl" ng-init="init();">
	<br/><hr/><br/>
	<h2 class="coral-Heading coral-Heading--2">Redirect Map</h2>
	<form action="${resource.path}" method="post" class="coral-Form--aligned" id="fn-acsCommons-update-redirect" ng-submit="updateRedirectMap()" enctype="multipart/form-data">
		
    	<input type="hidden" name="*@TypeHint" value="nt:file" />
		
		<div class="coral-Form-fieldwrapper">
			<label class="coral-Form-fieldlabel">
				Redirect Map *
			</label>
			<input type="file" class="coral-Textfield" name="./redirectMap.txt" />
		</div>
				
		<div class="coral-Form-fieldwrapper" >
			<button class="coral-Button coral-Button--primary">Update</button>
		</div>
	</form>
	<c:set var="redirectMap" value="${sling2:getRelativeResource(resource, 'redirectMap.txt')}" />
	<c:if test="${redirectMap != null}">
		<a href="${redirectMap.path}">
			Download Redirect Map File
		</a>
	</c:if>
	<br/><hr/><br/>
	<h2 class="coral-Heading coral-Heading--2">Redirect Configuration</h2>
	<br/>
	<c:set var="redirectParent" value="${sling2:getRelativeResource(resource, 'redirects')}" />
	<c:forEach var="redirects" items="${sling2:listChildren(redirectParent)}">
    	<cq:include path="${redirects.path}" resourceType="${redirects.resourceType}" />
	</c:forEach>
	<form action="${resource.path}/redirects/*" method="post" class="coral-Form--aligned" id="fn-acsCommons-add-redirectconfig" ng-submit="postValues('fn-acsCommons-add-redirectconfig')">
		<input type="hidden"  name="sling:resourceType" value="acs-commons/components/utilities/redirects" />
		<div class="coral-Form-fieldwrapper" >
			<button class="coral-Button coral-Button--primary">+ Redirect Configuration</button>
		</div>
	</form>
	
	<br/><hr/><br/>
	
	<div class="coral-Well">
		<h2 class="coral-Heading coral-Heading--2">Redirect Map Preview</h2>
		<br/>
		<sling2:adaptTo adaptable="${resource}" adaptTo="com.adobe.acs.commons.redirects.RedirectMapModel" var="redirectMapModel" />
		<c:if test="${fn:length(redirectMapModel.invalidEntries) > 0}">
			<coral-alert size="L" variant="error">
				<coral-alert-header>Invalid Redirect Sources</coral-alert-header>
				<coral-alert-content>
					<ul>
						<c:forEach var="invalidEntry" items="${redirectMapModel.invalidEntries}">
							<li>
								Entry: ${invalidEntry}<br/>
							</li>
						</c:forEach>
					</ul>
				</coral-alert-content>
			</coral-alert>
		</c:if>
		<pre>${redirectMapModel.redirectMap}</pre>
	</div>
</div>
