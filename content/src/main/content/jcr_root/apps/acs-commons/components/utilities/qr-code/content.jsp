<%--
  ~ #%L
  ~ ACS AEM Commons Bundle
  ~ %%
  ~ Copyright (C) 2015 Adobe
  ~ %%
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~   http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  ~ #L%
  --%>
<%@include file="/libs/foundation/global.jsp" %><%
%><%@page session="false" %><%

%><div ng-controller="MainCtrl"
     ng-init="app.uri = '${resourcePath}.json';">

    <p>QR Code for an AEM Page URL can be generated while Authoring. It will easily allow you to view published page in Mobile / Tablet devices</p>
    <p>More Information :-</p>
    <ul>
        <li>Once enabled, QR Icon will be available while authoring a page.</li>
        <li>Mapping for Author and Publish Host URL. Example- Assume Author Host is '<i>author-project.com</i>' and Publish Host is '<i>project.com</i>'. If the page being authored is <b>author-project.com/content/project/en/home.html</b>, then generated QR code will have URL as <b>project.com/content/project/en/home.html</b></li>
        <li>Do not add protocol <i>http</i> or <i>https</i> to the url.</li>
    </ul>

    <cq:include script="includes/form.jsp"/>
</div>
