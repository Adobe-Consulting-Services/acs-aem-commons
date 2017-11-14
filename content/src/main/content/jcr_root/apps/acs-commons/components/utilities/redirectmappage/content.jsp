<%@ page session="false" contentType="text/html" pageEncoding="utf-8" %>
<%@include file="/libs/foundation/global.jsp" %>
<%@taglib prefix="sling2" uri="http://sling.apache.org/taglibs/sling" %>
<cq:setContentBundle />
<div ng-controller="MainCtrl" ng-init="init();">

	<br/><hr/><br/>
	
	<c:set var="redirectMap" value="${sling2:getRelativeResource(resource, 'redirectMap.txt')}" />
	<coral-tabview>
		<coral-tablist target="main-panel-1">
			<coral-tab><fmt:message key="Configure" /></coral-tab>
			<coral-tab><fmt:message key="Preview" /></coral-tab>
		</coral-tablist>
		<coral-panelstack id="main-panel-1">
			<coral-panel class="coral-Well">
				<section>
					<h2 class="coral-Heading coral-Heading--2">
						<fmt:message key="Configure Redirect Map" />
					</h2>
					<p>
						<fmt:message key="The redirect map file will be combined with the redirects configured in AEM to create the final set of redirects." />
					</p>
					<form action="${resource.path}" method="post" class="coral-Form--aligned" id="fn-acsCommons-update-redirect" ng-submit="updateRedirectMap()" enctype="multipart/form-data">
				    	<input type="hidden" name="./redirectMap.txt@TypeHint" value="nt:file" />
						
						<div class="coral-Form-fieldwrapper">
							<label class="coral-Form-fieldlabel" id="label-vertical-inputgroup-1">
								<fmt:message key="Redirect Map File" /> *
							</label>
							<div class="coral-InputGroup coral-Form-field">
								<input is="coral-Textfield" class="coral-InputGroup-input coral-Textfield" type="file" name="./redirectMap.txt" />
							</div>
							<coral-icon class="coral-Form-fieldinfo coral-Icon coral-Icon--infoCircle coral-Icon--sizeS" icon="infoCircle" size="S" id="file-info" role="img" aria-label="info circle"></coral-icon>
							<coral-tooltip variant="info" placement="right" target="#file-info" class="coral3-Tooltip coral3-Tooltip--info" aria-hidden="true" tabindex="-1" role="tooltip" style="display: none;">
								<coral-tooltip-content>
									<fmt:message key="This file should be a space-delimited file with the first column containing the source path and the second column containing the redirect destination." />
								</coral-tooltip-content>
							</coral-tooltip>
						</div>
						<c:if test="${redirectMap != null}">
							<a class="coral-Link" href="${redirectMap.path}">
								<fmt:message key="Download Current Redirect Map File" />
							</a>
						</c:if><br/><br/>
						<div class="coral-Form-fieldwrapper" >
							<button class="coral-Button coral-Button--primary"><fmt:message key="Upload" /></button>
						</div>
					</form>
				</section>
				<section>
					<h2 class="coral-Heading coral-Heading--2">Redirect Configuration</h2>
					<p>
						<fmt:message key="Redirect configurations are used to gather vanity redirects to AEM pages based on a multivalued property and the mapping configuration specified. The property and path fields are used to form a query to find available redirects. For example, specifying / as the path and sling:vanityPath as the property will load all vanity paths in the system." />
					</p>
					<c:set var="redirectParent" value="${sling2:getRelativeResource(resource, 'redirects')}" />
					<c:forEach var="redirects" items="${sling2:listChildren(redirectParent)}">
				    	<cq:include path="${redirects.path}" resourceType="${redirects.resourceType}" />
					</c:forEach>
					<form action="${resource.path}/redirects/*" method="post" class="coral-Form--aligned" id="fn-acsCommons-add-redirectconfig" ng-submit="postValues('fn-acsCommons-add-redirectconfig')">
						<input type="hidden" name="sling:resourceType" value="acs-commons/components/utilities/redirects" />
						<input type="hidden" name="jcr:created" />
						<input type="hidden" name="jcr:createdBy" />
						<div class="coral-Form-fieldwrapper" >
							<button class="coral-Button coral-Button--primary">+ <fmt:message key="Redirect Configuration" /></button>
						</div>
					</form>
				</section>
			</coral-panel>
			<coral-panel class="coral-Well">
				<section>
					<h2 class="coral-Heading coral-Heading--2"><fmt:message key="Redirect Preview" /></h2>
					<c:if test="${redirectMap != null}">
						<a class="coral-Link" href="${resource.path}.redirectmap.txt">
							<fmt:message key="Download Combined Redirect Map File" />
						</a>
						<br/>
						Published Path: ${resource.path}.redirectmap.txt
					</c:if>
					<sling2:adaptTo adaptable="${resource}" adaptTo="com.adobe.acs.commons.redirectmaps.models.RedirectMapModel" var="redirectMapModel" />
					<c:if test="${fn:length(redirectMapModel.invalidEntries) > 0}">
						<coral-alert size="L" variant="error">
							<coral-alert-header><fmt:message key="Invalid Redirect Sources "/></coral-alert-header>
							<coral-alert-content>
								<ul>
									<c:forEach var="invalidEntry" items="${redirectMapModel.invalidEntries}">
										<li>
											<fmt:message key="Entry "/> <strong>${invalidEntry.source}</strong> <fmt:message key=" on page "/> <a class="coral-Link" target="_blank" href="/sites.html${invalidEntry.resource.path}">${invalidEntry.resource.path}</a> <fmt:message key=" contains whitespace "/>
										</li>
									</c:forEach>
								</ul>
							</coral-alert-content>
						</coral-alert>
					</c:if>
					<pre>${redirectMapModel.redirectMap}</pre>
				</section>
			</coral-panel>
    	</coral-panelstack>
	</coral-tabview>
</div>
