<%@ page session="false" contentType="text/html" pageEncoding="utf-8" %>
<%@include file="/libs/foundation/global.jsp" %>
<%@taglib prefix="sling2" uri="http://sling.apache.org/taglibs/sling" %>
<cq:setContentBundle />
<div ng-controller="MainCtrl" ng-init="init();">
	<form action="${resource.path}" method="post" class="coral-Form--aligned" id="fn-acsCommons-APR-form" ng-submit="save()">
		<br/><hr/><br/>
		
		<div class="coral-Form-fieldwrapper">
			<label class="coral-Form-fieldlabel">
				Title *
			</label>
			<input type="text" class="coral-Textfield" name="./jcr:title" value="${properties['jcr:title']}"  required="required"/>
		</div>
		
		<div class="coral-Form-fieldwrapper">
			<label class="coral-Form-fieldlabel">
				Package Path *
			</label>
			<input type="text" list="packages" class="coral-Textfield" name="./packagePath" value="${properties.packagePath}"  required="required"/>
			<datalist id="packages">
				<sling2:findResources var="packages" query="SELECT * FROM [nt:file] AS s WHERE ISDESCENDANTNODE([/etc/packages]) AND NAME() LIKE '%.zip' AND NOT [jcr:path] LIKE '%.snapshot%'" language="JCR-SQL2" />
				<c:forEach var="package" items="${packages}">
					<option value="${package.path}">
						<c:set var="pname" value="/${package.name}" />
						${fn:replace(fn:replace(package.path,pname,''),'/etc/packages/','')}: ${package.name}
					</option>
				</c:forEach>
			</datalist>
		</div>
		
		<div class="coral-Form-fieldwrapper">
			<label class="coral-Form-fieldlabel">
				Trigger *
			</label>
			<span>
				<select id="trigger-select" name="./trigger" required="required">
					<option value="">-- Select --</option>
					<option value="event" ${properties.trigger eq "event" ? "selected" : ""}>Sling Event</option>
					<option value="cron" ${properties.trigger eq "cron" ? "selected" : ""}>Cron</option>
				</select>
			</span>
		</div>
		
		<div id="event-container" style="${properties.trigger eq "event" ? "" : "display:none"}">
			<div class="coral-Form-fieldwrapper">
				<label class="coral-Form-fieldlabel">
					Event Topic *
				</label>
				<input type="text" class="coral-Textfield" name="./eventTopic" value="${properties.eventTopic}"/>
			</div>
			
			<div class="coral-Form-fieldwrapper">
				<label class="coral-Form-fieldlabel">
					Event Filter
				</label>
				<input type="text" class="coral-Textfield" name="./eventFilter" value="${properties.eventFilter}"/>
			</div>
		</div>
		
		<div class="coral-Form-fieldwrapper" id="cron-container" style="${properties.trigger eq "cron" ? "" : "display:none"}">
			<label class="coral-Form-fieldlabel">
				Cron Trigger *
			</label>
			<input type="text" class="coral-Textfield" name="./cronTrigger" value="${properties.cronTrigger}"/>
		</div>
		
		
		<div class="coral-Form-fieldwrapper" >
			<button class="coral-Button coral-Button--primary" >Save</button>
		</div>
		
	</form>
</div>
