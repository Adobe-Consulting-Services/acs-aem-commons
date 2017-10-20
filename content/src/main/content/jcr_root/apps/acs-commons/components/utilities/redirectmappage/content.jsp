<%@ page session="false" contentType="text/html" pageEncoding="utf-8" %>
<%@include file="/libs/foundation/global.jsp" %>
<%@taglib prefix="sling2" uri="http://sling.apache.org/taglibs/sling" %>
<cq:setContentBundle />
<div ng-controller="MainCtrl" ng-init="init();">

	<br/><hr/><br/>
	
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
							<label class="coral-Form-fieldlabel">
								<fmt:message key="Redirect Map File" /> *
							</label>
							<input type="file" class="coral-Textfield" name="./redirectMap.txt" />
						</div>
						<div class="coral-Form-fieldwrapper" >
							<button class="coral-Button coral-Button--primary"><fmt:message key="Upload" /></button>
						</div>
					</form>
					<c:set var="redirectMap" value="${sling2:getRelativeResource(resource, 'redirectMap.txt')}" />
					<c:if test="${redirectMap != null}">
						<a class="coral-Link" href="${redirectMap.path}">
							<fmt:message key="Download Redirect Map File" />
						</a>
					</c:if>
				</section>
				<section>
					<h2 class="coral-Heading coral-Heading--2">Redirect Configuration</h2>
					<p>
						<fmt:message key="Redirect configurations are used to gather vanity redirects to AEM pages based on a multivalued property and the mapping configuration specified." />
					</p>
					<c:set var="redirectParent" value="${sling2:getRelativeResource(resource, 'redirects')}" />
					<c:forEach var="redirects" items="${sling2:listChildren(redirectParent)}">
				    	<cq:include path="${redirects.path}" resourceType="${redirects.resourceType}" />
					</c:forEach>
					<form action="${resource.path}/redirects/*" method="post" class="coral-Form--aligned" id="fn-acsCommons-add-redirectconfig" ng-submit="postValues('fn-acsCommons-add-redirectconfig')">
						<input type="hidden"  name="sling:resourceType" value="acs-commons/components/utilities/redirects" />
						<div class="coral-Form-fieldwrapper" >
							<button class="coral-Button coral-Button--primary">+ <fmt:message key="Redirect Configuration" /></button>
						</div>
					</form>
				</section>
			</coral-panel>
			<coral-panel class="coral-Well">
				<section>
					<h2 class="coral-Heading coral-Heading--2"><fmt:message key="Redirect Preview" /></h2>
					<sling2:adaptTo adaptable="${resource}" adaptTo="com.adobe.acs.commons.redirectmaps.RedirectMapModel" var="redirectMapModel" />
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
