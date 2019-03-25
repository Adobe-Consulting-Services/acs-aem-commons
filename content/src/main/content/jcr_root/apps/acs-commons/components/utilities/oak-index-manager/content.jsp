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

    pageContext.setAttribute("hasOakIndex", resourceResolver.getResource("/oak:index") != null);

%><c:choose>
    <c:when test="${hasOakIndex}">
        
         <div ng-controller="MainCtrl"
              ng-init="app.resource = '${resourcePath}'; init();">


             <div class="coral-Alert coral-Alert--error coral-Alert--large">
                 <i class="coral-Alert-typeIcon coral-Icon coral-Icon--sizeS coral-Icon--alert"></i>
                 <strong class="coral-Alert-title">ACS AEM Commons Oak Index Manager is deprecated!</strong>
                 <div class="coral-Alert-message">Please use the official <a
                         x-cq-linkchecker="skip"
                         href="/libs/granite/operations/content/diagnosistools/indexManager.html">Oak Index Manager</a> provided by Adobe Experience Manager.</div>
             </div>

             <div class="acs-section">
                 <%-- Bulk Reindex --%>
                 <div style="float: right;">
                     <button class="coral-Button coral-Button--primary"
                             ng-click="bulkReindex( (filtered | filter: { checked: true }) )">Bulk Reindex</button>
                 </div>

                <%-- Filter --%>
                <div class="search">
                    <span class="coral-DecoratedTextfield filter-input">
                        <i class="coral-DecoratedTextfield-icon coral-Icon coral-Icon--sizeXS coral-Icon--search"></i>
                        <input ng-model="keyword" 
                               placeholder="Filter" 
                               type="text" 
                               style="width: 500px"
                               class="coral-DecoratedTextfield-input coral-Textfield">
                    </span>
                </div>
             </div>
             
            <%-- Index Table --%>
            <table class="coral-Table coral-Table--hover">
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
                        <td  class="coral-Table-cell acs-table-cell-refresh">
                            <button ng-show="!index.reindex"
                                    ng-click="reindex(index)"
                                    class="coral-Button coral-Button--square coral-Button--quiet">
                                <i class="coral-Icon coral-Icon--refresh"></i>
                            </button>
                            <div ng-show="index.reindex"
                                 class="coral-Wait"></div>
                        </td>
                    </tr>

                    <tr class="coral-Table-row" ng-show="keyword && !filtered.length">
                        <td colspan="10"
                             class="coral-Table-cell acs-empty-results">
                            No matching indexes found
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>
    </c:when>
    <c:otherwise>
        <div class="coral-Alert coral-Alert--error coral-Alert--large">
            <i class="coral-Alert-typeIcon coral-Icon coral-Icon--sizeS coral-Icon--alert"></i>
            <strong class="coral-Alert-title">Unsupported version of AEM</strong>
            <div class="coral-Alert-message">ACS AEM Commons Oak Index Manager requires an Apache Jackrabbit Oak based repository.</div>
        </div>
    </c:otherwise>
</c:choose>