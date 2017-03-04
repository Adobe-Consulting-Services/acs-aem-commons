<%--
  #%L
  ACS AEM Commons Package
  %%
  Copyright (C) 2017 Adobe
  %%
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

	   http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
  #L%
  --%>
<%@include file="/libs/foundation/global.jsp" %><%@taglib prefix="sling2" uri="http://sling.apache.org/taglibs/sling" %>
<c:set var="hasIndex" value="false" /><sling2:findResources var="indexes" query="SELECT * FROM [oak:QueryIndexDefinition] AS s WHERE [declaringNodeTypes]='cq:AuditEvent' AND ISCHILDNODE([/oak:index])" language="JCR-SQL2" />
<c:forEach var="i" items="${indexes}"><c:set var="hasIndex" value="true" /></c:forEach>

<div class="acs-section"> 
	<div ng-controller="MainCtrl" ng-init="app.uri = '${resourcePath}.auditlogsearch.json'; init();">
		<c:if test="${hasIndex != 'true'}"> 
			<div class="coral-Alert coral-Alert--notice index-warning">
				<button type="button" class="coral-MinimalButton coral-Alert-closeButton" title="Close" data-dismiss="alert">
					<i class="coral-Icon coral-Icon--sizeXS coral-Icon--close coral-MinimalButton-icon"></i>
				</button>
				<i class="coral-Alert-typeIcon coral-Icon coral-Icon--sizeS coral-Icon--alert"></i>
				<strong class="coral-Alert-title">Index Missing</strong>
				<div class="coral-Alert-message">
					No index found for the type <code>cq:AuditEvent</code>, this will result in very slow performance.
					<form id="create-index-form">
						<input type="hidden" name="jcr:primaryType" value="oak:QueryIndexDefinition" />
						<input type="hidden" name="reindex" value="true" />
						<input type="hidden" name="reindex@TypeHint" value="Boolean" />
						<input type="hidden" name="type" value="lucene" />
						<input type="hidden" name="async" value="async" />
						<input type="hidden" name="declaringNodeTypes" value="cq:AuditEvent" />
						<input type="hidden" name="declaringNodeTypes@TypeHint" value="Name[]" />
						<input type="hidden" name="propertyNames" value="cq:userid" />
						<input type="hidden" name="propertyNames" value="cq:path" />
						<input type="hidden" name="propertyNames" value="cq:time" />
						<input type="hidden" name="propertyNames" value="cq:type" />
						<input type="hidden" name="propertyNames@TypeHint" value="String[]" />
						<br/>
						<button class="coral-Button" ng-click="createIndex()">Create Index</button>
					</form>
				</div>
			</div>
		</c:if>
	    <p>Explore the audit log at any path.</p>
	    <cq:include script="includes/form.jsp"/>
	    <cq:include script="includes/results.jsp"/>
	</div>
</div>