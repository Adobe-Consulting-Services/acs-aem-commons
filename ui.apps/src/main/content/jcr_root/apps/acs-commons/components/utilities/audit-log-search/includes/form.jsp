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
<form ng-submit="search()" id="audit-log-search-form">
	<div class="form-row">
		<label acs-coral-heading>
			Content Root
		</label>
		<span>
			<input type="text" name="contentRoot" class="coral-Textfield"  ng-required="true" ng-pattern="/^\/.+$/" ng-model="form.contentRoot" placeholder="Root path [ Default: /content ]"/>
		</span>
	</div>
	
	<div class="form-row">
		<label acs-coral-heading>
			Include Children?
		</label>
		<span>
			<div class="coral-Selector">
				<label class="coral-Selector-option">
					<input ng-model="form.includeChildren" type="radio" class="coral-Selector-input" name="includeChildren" value="true" />
					<span class="coral-Selector-description">Yes</span>
				</label>
				<label class="coral-Selector-option">
					<input ng-model="form.includeChildren" type="radio" class="coral-Selector-input" name="includeChildren" value="false" />
					<span class="coral-Selector-description">No</span>
				</label>
			</div>
		</span>
	</div>
	
	<div class="form-row">
		<label acs-coral-heading>
			Event Type
		</label>
		<span>
			<input type="text" name="type" list="events" class="coral-Textfield" ng-model="form.type" placeholder="Event Type [ Optional, example: PageCreated ]"/>
			<datalist id="events">
				<option>Activate</option>
				<option>ASSET_CREATED</option>
				<option>ASSET_REMOVED</option>
				<option>Deactivate</option>
				<option>Delete</option>
				<option>DOWNLOADED</option>
				<option>PageCreated</option>
				<option>PageDeleted</option>
				<option>PageModified</option>
				<option>PageMoved</option>
				<option>VersionCreated</option>
			</datalist>
		</span>
	</div>
	
	<div class="form-row">
		<label acs-coral-heading>
			Event User
		</label>
		<span>
			<input type="text" list="users" name="user" class="coral-Textfield" ng-model="form.user" placeholder="Event User [ Optional ]" />
			<datalist id="users">
				<c:forEach var="user" items="${sling2:findResources(resourceResolver,'SELECT * FROM [rep:User] AS s ORDER BY [profile/familyName], [profile/givenName]','JCR-SQL2')}">
					<option value="${user.valueMap['rep:authorizableId']}">
						<c:set var="profile" value="${sling2:getRelativeResource(user,'profile')}" />
						<c:choose>
							<c:when test="${profile != null && not empty profile.valueMap.givenName}">
								<c:out value="${profile.valueMap.givenName}" /> <c:out value="${profile.valueMap.familyName}" />
							</c:when>
							<c:otherwise>
								${user.valueMap['rep:authorizableId']}
							</c:otherwise>
						</c:choose>
					</option>
				</c:forEach>
			</datalist>
		</span>
	</div>
	
	<div class="form-row">
		<label acs-coral-heading>
			Event Start Date
		</label>
		<span>
			<input type="datetime-local" name="startDate" class="coral-Textfield" ng-model="form.startDate" placeholder="Event Start Date [ Optional ]" />
			<small>(UTC)</small>
		</span>
	</div>
	
	<div class="form-row">
		<label acs-coral-heading>
			Event End Date
		</label>
		<span>
			<input type="datetime-local" name="endDate" class="coral-Textfield" ng-model="form.endDate" placeholder="Event End Date [ Optional ]" />
			<small>(UTC)</small>
		</span>
	</div>
	
	<div class="form-row">
		<label acs-coral-heading>
			Order By
		</label>
		<span>
			<input type="text" name="order" list="order" class="coral-Textfield" ng-model="form.order" placeholder="Order By [ Optional, [cq:time] ASC ]"/>
			<datalist id="order">
					<option value="[cq:time] ASC">Oldest</option>
					<option value="[cq:time] DESC">Most Recent</option>
					<option value="[cq:path] ASC">Content Path (Ascending)</option>
					<option value="[cq:path] DESC">Content Path (Descending)</option>
					<option value="[cq:userid] ASC">User ID (Ascending)</option>
					<option value="[cq:userid] DESC">User ID (Descending)</option>
			</datalist>
		</span>
	</div>
	
	<div class="form-row">
		<label acs-coral-heading>
			Limit
		</label>
		<span>
			<input type="number" name="limit" class="coral-Textfield" ng-model="form.limit" placeholder="Limit [ Optional, 10 ]"/>
		</span>
	</div>
	
	<div class="form-row">
		<div class="form-left-cell">&nbsp;</div>
		<button class="coral-Button coral-Button--primary">Search Audit Log</button>
	</div>
</form>
