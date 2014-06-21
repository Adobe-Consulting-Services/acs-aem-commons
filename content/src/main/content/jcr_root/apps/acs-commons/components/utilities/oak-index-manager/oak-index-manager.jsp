<%--
  #%L
  ACS AEM Tools Package
  %%
  Copyright (C) 2014 Adobe
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
<%@include file="/libs/foundation/global.jsp" %><%
%><%@page session="false" %><%

    pageContext.setAttribute("pagePath", resourceResolver.map(currentPage.getPath()));
    pageContext.setAttribute("resourcePath", resourceResolver.map(resource.getPath()));
    pageContext.setAttribute("hasOakIndex", resourceResolver.getResource("/oak:index") != null);
    pageContext.setAttribute("favicon", component.getPath() + "/clientlibs/images/favicon.ico");

%><!doctype html><html>
<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">

    <title>Oak Index Manager | ACS AEM Commons</title>

    <link rel="shortcut icon" href="${favicon}"/>

    <cq:includeClientLib css="acs-commons.oak-index-manager.app"/>
</head>

<body id="acs-commons-oak-index-manager-app">

    <header class="top">

        <div class="logo">
            <a href="/"><i class="icon-marketingcloud medium"></i></a>
        </div>

        <nav class="crumbs">
            <a href="/miscadmin">Tools</a>
            <a href="${pagePath}.html">Oak Index Manager</a>
        </nav>
    </header>

<c:choose>
    <c:when test="${hasOakIndex}">
        <div class="page" role="main"
     ng-controller="MainCtrl"
     ng-init="app.resource = '${resourcePath}'; init();">

    <div ng-show="notifications.length > 0"
         class="notifications">
        <div ng-repeat="notification in notifications">
            <div class="alert {{ notification.type }}">
                <button class="close" data-dismiss="alert">&times;</button>
                <strong>{{ notification.title }}</strong>

                <div>{{ notification.message }}</div>
            </div>
        </div>
    </div>

    <div class="content">
        <div class="content-container">

            <h1>Oak Index Manager</h1>

            <div class="search">
                <input ng-model="keyword" type="text" placeholder="Filter" class="filter-input">
                <button>Filter</button>
            </div>

            <div style="float: right;">
                <div ng-show="app.running"
                     class="running-indicator spinner large"></div>
                <button class="primary"
                        ng-click="bulkReindex( ( filtered | indexCheckedFilter : true ) )">Bulk Reindex</button>
            </div>

            <table class="data index-table">
                <thead>
                    <tr>
                        <th class="check"><label><input type="checkbox" ng-model="toggleChecks"><span></span></label></th>
                        <th>Node Name</th>
                        <th>Declaring Node Types</th>
                        <th>Property Names</th>
                        <th>Include Property Types</th>
                        <th>Exclude Property Types</th>
                        <th>Type</th>
                        <th>Unique</th>
                        <th>Async</th>
                        <th>Reindex</th>
                    </tr>
                </thead>
                <tbody>
                    <tr ng-repeat="(name, index) in filtered = ( indexes | indexKeywordFilter : keyword )"
                        ng-class="{ reindexing: index.reindex }">

                        <td>
                            <label><input type="checkbox" ng-model="index.checked"><span></span></label>
                        </td>
                        <td>
                            {{ name }}
                        </td>
                        <td>
                            <div ng-repeat="declaringNodeType in index.declaringNodeTypes">{{ declaringNodeType }}</div>
                        </td>
                        <td>
                            <div ng-repeat="propertyName in index.propertyNames">{{ propertyName }}</div>
                        </td>
                        <td>
                            <div ng-repeat="includePropertyType in index.includePropertyTypes">{{
                                includePropertyType }}</div>
                        </td>
                        <td>
                            <div ng-repeat="excludePropertyName in index.excludePropertyNames">{{
                                excludePropertyName }}</div>
                        </td>
                        <td>
                            {{ index.type }}
                        </td>
                        <td>
                            {{ index.unique }}
                        </td>
                        <td>
                            {{ index.async }}
                        </td>
                        <td class="reindex-status">
                            <a href="#"
                               class="icon-refresh reindex-button"
                               ng-show="!index.reindex"
                               ng-click="reindex(index)">\U00F0AB</a>
                            <div ng-show="index.reindex" class="spinner"></div>
                        </td>
                    </tr>
                </tbody>
            </table>
    </div>
</div>

    <cq:includeClientLib js="acs-commons.oak-index-manager.app"/>

    <%-- Register angular app; Decreases chances of collisions w other angular apps on the page (ex. via injection) --%>
    <script type="text/javascript">
        angular.bootstrap(document.getElementById('acs-commons-oak-index-manager-app'),
                ['oakIndexManager']);
    </script>

</div>
    </c:when>
    <c:otherwise>
        <div class="unsupported large alert notice">
            <strong>Unsupported version of AEM</strong>

            <div>
                ACS AEM Commons Oak Index Manager requires an Apache Jackrabbit Oak based repository.
            </div>
        </div>
    </c:otherwise>
</c:choose>
</body>
</html>