<%--
  ~ #%L
  ~ ACS AEM Commons Bundle
  ~ %%
  ~ Copyright (C) 2013 Adobe
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
<%@include file="/libs/foundation/global.jsp" %><%
%><%@page session="false" %><%

    pageContext.setAttribute("pagePath", resourceResolver.map(slingRequest, currentPage.getPath()));
    pageContext.setAttribute("resourcePath", resourceResolver.map(slingRequest, resource.getPath()));
    pageContext.setAttribute("favicon", resourceResolver.map(slingRequest, component.getPath() + "/clientlibs/images/favicon.ico"));

%><!doctype html>
<html class="coral-App">
<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">

    <title>Workflow Remover | ACS AEM Commons</title>

    <link rel="shortcut icon" href="${favicon}"/>

    <cq:includeClientLib css="acs-commons.workflow-remover.app"/>
</head>

<body class="coral--light" id="acs-commons-workflow-remover-app">

    <header acs-coral-tools-header data-context-path="${request.contextPath}" data-page-path="${currentPage.path}.html" data-title="Workflow Remover"></header>

     <div class="page" role="main"
             ng-controller="MainCtrl"
             ng-init="app.resource = '${resourcePath}'; init();">


        <div class="content">
            <div class="content-container" id="scroll-top">
                <div class="content-container-inner">

                    <h1 acs-coral-heading>Workflow Remover</h1>

                    <cq:include script="includes/notifications.jsp"/>

                    <cq:include script="includes/status.jsp"/>

                    <cq:include script="includes/form.jsp"/>

                </div>
            </div>
        </div>

        <cq:includeClientLib js="acs-commons.workflow-remover.app"/>

        <%-- Register angular app; Decreases chances of collisions w other angular apps on the page (ex. via injection) --%>
        <script type="text/javascript">
            angular.bootstrap(document.getElementById('acs-commons-workflow-remover-app'),
                    ['workflowRemover']);
        </script>

    </div>
</body>
</html>