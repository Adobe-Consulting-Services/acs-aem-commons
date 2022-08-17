<%--
  ~ #%L
  ~ ACS AEM Commons Bundle
  ~ %%
  ~ Copyright (C) 2014 Adobe
  ~ %%
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~      http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  ~ #L%
  --%>

<%@include file="/libs/foundation/global.jsp"%><%
%><%@page session="false" contentType="text/html" pageEncoding="utf-8"
          import="com.adobe.acs.commons.util.TextUtil"%><%

    /* Page Properties */
    final String pageTitle = TextUtil.getFirstNonEmpty(
            currentPage.getPageTitle(),
            currentPage.getTitle(),
            currentPage.getName());
%>

<div class="notifications">



    <sp-banner size="l" type="info">
        <div slot="header">Preview</div>
        <div slot="content">

            <p>The following filter paths will be used in the package definition:</p>

            <ul class="filters"></ul>

            <p>If the above filter paths appear satisfactory, press the &quot;Create Package&quot; button below to
                create the actual package definition in <a target="_blank" x-cq-linkchecker="skip" href="/crx/packmgr/index.jsp">CRX Package
                    Manager</a>.
            </p>

        </div>
    </sp-banner>


    <sp-banner type="info">
        <div slot="header">Success</div>
        <div slot="content">

            <p>A new package has been created at: <a class="package-manager-link" target="_blank" x-cq-linkchecker="skip" href=""><span
                    class="package-path"></span></a></p>

            <ul class="filters"></ul>

            <p>Go to the <a class="package-manager-link" target="_blank" x-cq-linkchecker="skip" href="">CRX Package manager</a> to build and
                download this package.</p>

        </div>
    </sp-banner>



    <sp-banner type="error">
        <div slot="header">Error</div>
        <div slot="content">


            <p>An error occurred while building the ACL Package.</p>

            <p class="msg"></p>

            <p>Please check the following</p>
            <ul>
                <li>Review your packaging settings on this page (especially Conflict Resolution)</li>
                <li>Verify you have read and write access to /etc/packages</li>
            </ul>

        </div>
    </sp-banner>


</div>

<%-- Custom impl of the packager configuration --%>
<cq:include path="configuration" resourceType="<%= component.getPath() + "/configuration" %>"/>

<%-- JavaScript supporting Form Submissions/Response behaviors --%>
<cq:includeClientLib js="acs-commons.utilities.packager"/>
