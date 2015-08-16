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

    pageContext.setAttribute("pagePath", resourceResolver.map(slingRequest, currentPage.getPath()));
    pageContext.setAttribute("resourcePath", resourceResolver.map(slingRequest, resource.getPath()));
    pageContext.setAttribute("hasOakIndex", resourceResolver.getResource("/oak:index") != null);
    pageContext.setAttribute("favicon", resourceResolver.map(slingRequest, 
            component.getPath() + "/clientlibs/images/favicon.ico"));

%><!doctype html><html class="coral-App">
<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">

    <title>Oak Index Manager | ACS AEM Commons</title>

    <link rel="shortcut icon" href="${favicon}"/>

    <cq:includeClientLib css="acs-commons.oak-index-manager.app"/>
</head>

<body class="coral--light">
    <div id="acs-commons-oak-index-manager-app">
        <header acs-coral-tools-header data-context-path="${request.contextPath}" data-page-path="${currentPage.path}.html" data-title="Oak Index Manager"></header>

<c:choose>
    <c:when test="${hasOakIndex}">
         <div class="page" role="main"
                 ng-controller="MainCtrl"
                 ng-init="app.resource = '${resourcePath}'; init();">

            <div ng-show="notifications.length > 0"
                 class="notifications">
                <div ng-repeat="notification in notifications">
                    <div acs-coral-alert
                         data-alert-type="{{ notification.type }}"
                         data-alert-title="{{ notification.title }}" data-alert-message="{{ notification.message }}">
                    </div>
                </div>
            </div>

            <div class="content">
                <div class="content-container">
                    <div class="content-container-inner">

                        <h1>Oak Index Manager</h1>

                        <div class="search">
                            <span class="coral-DecoratedTextfield filter-input">
                                <i class="coral-DecoratedTextfield-icon coral-Icon coral-Icon--sizeXS coral-Icon--search"></i>
                                <input ng-model="keyword"  placeholder="Filter" type="text" class="coral-DecoratedTextfield-input coral-Textfield">
                            </span>
                            <button class="coral-Button">Filter</button>
                        </div>

                        <div style="float: right;">
                            <div ng-show="app.running"
                                 class="running-indicator spinner large"></div>
                            <button class="coral-Button coral-Button--primary"
                                    ng-click="bulkReindex( (filtered | filter: { checked: true }) )">Bulk
                                Reindex</button>
                        </div>

                        <table class="data index-table coral-Table coral-Table--hover">
                            <thead>
                                <tr class="coral-Table-row">
                                    <th class="coral-Table-headerCell check"><label acs-coral-checkbox><input type="checkbox" ng-model="toggleChecks"><span></span></label></th>
                                    <th class="coral-Table-headerCell">Node Name</th>
                                    <th class="coral-Table-headerCell">Declaring Node Types</th>
                                    <th class="coral-Table-headerCell">Property Names</th>
                                    <th class="coral-Table-headerCell">Include Property Types</th>
                                    <th class="coral-Table-headerCell">Exclude Property Names</th>
                                    <th class="coral-Table-headerCell">Type</th>
                                    <th class="coral-Table-headerCell">Unique</th>
                                    <th class="coral-Table-headerCell">Async</th>
                                    <th class="coral-Table-headerCell">Reindex</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr ng-repeat="index in filtered = ( indexes | filter : { $: keyword } | orderBy: '+name' )"
                                    ng-class="{ reindexing: index.reindex }"
                                    class="coral-Table-row">

                                    <td class="coral-Table-cell">
                                        <label acs-coral-checkbox><input type="checkbox" ng-model="index.checked"><span></span></label>
                                    </td>
                                    <td class="coral-Table-cell">
                                        {{ index.name }}
                                    </td>
                                    <td class="coral-Table-cell">
                                        <div ng-repeat="declaringNodeType in index.declaringNodeTypes">{{ declaringNodeType }}</div>
                                    </td>
                                    <td class="coral-Table-cell">
                                        <div ng-repeat="propertyName in index.propertyNames">{{ propertyName }}</div>
                                    </td>
                                    <td class="coral-Table-cell">
                                        <div ng-repeat="includePropertyType in index.includePropertyTypes">{{
                                            includePropertyType }}</div>
                                    </td>
                                    <td class="coral-Table-cell">
                                        <div ng-repeat="excludePropertyName in index.excludePropertyNames">{{
                                            excludePropertyName }}</div>
                                    </td>
                                    <td class="coral-Table-cell">
                                        {{ index.type }}
                                    </td>
                                    <td class="coral-Table-cell">
                                        {{ index.unique }}
                                    </td>
                                    <td class="coral-Table-cell">
                                        {{ index.async }}
                                    </td>
                                    <td  class="coral-Table-cell reindex-status">
                                        <a href="#"
                                           class="reindex-button"
                                           ng-show="!index.reindex"
                                           ng-click="reindex(index)"><i class="coral-Icon coral-Icon--refresh"></i></a>
                                        <div ng-show="index.reindex" class="spinner"></div>
                                    </td>
                                </tr>

                                <tr class="coral-Table-row" ng-show="keyword && !filtered.length">
                                    <td colspan="10"
                                         class="coral-Table-cell empty-results">

                                        No matching indexes found

                                    </td>
                                </tr>

                            </tbody>
                        </table>

                    </div>
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

    </div>
</body>
</html>