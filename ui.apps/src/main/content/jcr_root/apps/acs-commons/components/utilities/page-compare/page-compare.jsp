<%--
  #%L
  ACS AEM Commons Package
  %%
  Copyright (C) 2013 - 2018 Adobe
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
<%@page session="false" contentType="text/html; charset=utf-8" pageEncoding="UTF-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling" %>
<%@taglib prefix="cq" uri="http://www.day.com/taglibs/cq/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<cq:defineObjects />
<sling:adaptTo adaptable="${slingRequest}" adaptTo="com.adobe.acs.commons.wcm.comparisons.model.PageCompareModel" var="model"/>

<!doctype html>
<html class="coral-App">
<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">

    <title>Page Compare | ACS AEM Commons</title>

    <link rel="shortcut icon" href="${favicon}"/>

    <cq:includeClientLib css="acs-commons.page-compare"/>
    <cq:includeClientLib js="acs-commons.page-compare"/>
    <cq:includeClientLib categories="cq.widgets" />

</head>

<body class="coral--light">

<div id="acs-commons-page-compare">
    <coral-shell-header
            class="coral--dark granite-shell-header coral3-Shell-header"
            role="region"
            aria-label="Header Bar"
            aria-hidden="false">
        <coral-shell-header-home class="globalnav-toggle"
                                 data-globalnav-toggle-href="/"
                                 role="heading"
                                 aria-level="2">
            <a is="coral-shell-homeanchor"
               style="display: inline-block; padding-right: 0;"
               icon="adobeExperienceManagerColor"
               href="/"
               class="coral3-Shell-homeAnchor"><coral-icon class="coral3-Icon coral3-Shell-homeAnchor-icon coral3-Icon--sizeM coral3-Icon--adobeExperienceManagerColor" icon="adobeExperienceManagerColor" size="M" role="img" aria-label="adobe experience manager color"></coral-icon>
                <coral-shell-homeanchor-label>Adobe Experience Manager</coral-shell-homeanchor-label>
            </a>
            <span style="line-height: 2.375rem;">/ ACS AEM Commons / Page Compare</span>
        </coral-shell-header-home>
    </coral-shell-header>

    <div class="page" role="main"
         ng-controller="MainCtrl"
         ng-init="app.resource = '${model.pathA}'; app.resourceB = '${model.pathB}'; app.home = '${request.contextPath}${currentPage.path}.html'; app.a = '${model.versionA}'; app.b = '${model.versionB}'; init();">

        <div class="content">
            <div class="content-container">
                <div class="content-container-inner">

                    <h1 acs-coral-heading>Page Compare</h1>

                    <div class="legende">
                        <div class="definition" data-legend="EQUAL">
                            <div class="color"></div>
                            <div class="text">Equals</div>
                        </div>
                        <div class="definition" data-legend="NOT_EQUAL">
                            <div class="color color-NOT_EQUAL"></div>
                            <div class="text">Changes</div>
                        </div>
                        <div class="definition" data-legend="ONLY_LEFT">
                            <div class="color color-ONLY_LEFT"></div>
                            <div class="color empty second"></div>
                            <div class="text">Only left</div>
                        </div>
                        <div class="definition" data-legend="ONLY_RIGHT">
                            <div class="color empty"></div>
                            <div class="color color-ONLY_RIGHT second"></div>
                            <div class="text">Only right</div>
                        </div>
                        <div class="clearer"></div>
                    </div>

                    <div class="search">

                        <div class="half">
                            <div class="half-inner border">
                                <h2 acs-coral-heading ng-bind="app.resource"></h2>

                                <span data-init="pathbrowser" data-root-path="/" data-option-loader="" data-picker-src="/libs/wcm/core/content/common/pathbrowser/column.html/?predicate=hierarchyNotFile" data-crumb-root="">
                                        <input class="coral-InputGroup-input coral-Textfield js-coral-pathbrowser-input" placeholder="Enter path to resource" type="text" value="" autocomplete="off" aria-owns="coral-1" ng-model="app.resource" ng-change="dirty()" ng-blur="blur()">
                                        <button class="coral-Button coral-Button--secondary coral-Button--square js-coral-pathbrowser-button" type="button" title="Browse">
                                            <i class="coral-Icon coral-Icon--sizeS coral-Icon--folderSearch"></i>
                                        </button>
                                    </span>

                                <c:if test="${!empty model.a && !empty model.a.versions}">
                                    <select class="select" ng-model="app.a" ng-change="analyse()">
                                        <c:forEach var="versionSelection" items="${model.a.versions}">
                                            <option value="${versionSelection.name}"><c:out value="${versionSelection.name}" />, <fmt:formatDate type="both" value="${versionSelection.date}" /></option>
                                        </c:forEach>
                                    </select>
                                </c:if>


                            </div>
                        </div>
                        <div class="half">
                            <div class="half-inner border">
                                <h2 acs-coral-heading ng-bind="app.resourceB ? app.resourceB : app.resource"></h2>

                                <span data-init="pathbrowser" data-root-path="/" data-option-loader="" data-picker-src="/libs/wcm/core/content/common/pathbrowser/column.html/?predicate=hierarchyNotFile" data-crumb-root="">
                                        <input class="coral-InputGroup-input coral-Textfield js-coral-pathbrowser-input" placeholder="Enter optional path to second resource" type="text" value="" autocomplete="off" aria-owns="coral-1" ng-model="app.resourceB" ng-change="dirty()" ng-blur="blur()">
                                        <button class="coral-Button coral-Button--secondary coral-Button--square js-coral-pathbrowser-button" type="button" title="Browse">
                                            <i class="coral-Icon coral-Icon--sizeS coral-Icon--folderSearch"></i>
                                        </button>
                                    </span>

                                <c:if test="${!empty model.b && !empty model.b.versions}">
                                    <select class="select" name="b" ng-model="app.b" ng-change="analyse()">
                                        <c:forEach var="versionSelection" items="${model.b.versions}">
                                            <option value="${versionSelection.name}"><c:out value="${versionSelection.name}" />, <fmt:formatDate type="both" value="${versionSelection.date}" /></option>
                                        </c:forEach>
                                    </select>
                                </c:if>
                            </div>
                        </div>
                    </div>

                    <section class="differentResources-{{app.compareDifferentResources()}}">
                        <div class="content">
                            <div>
                                <c:if test="${!empty model.data}">
                                    <div class="half">
                                        <div class="half-inner">
                                            <div class="version" id="version-${model.a.version}">
                                                <div class="version-header">
                                                    <div class="name"><c:out value="${path}"/></div>
                                                    <div class="v"><c:out value="${model.a.version}"/></div>
                                                    <div class="date"><fmt:formatDate type="both" value="${model.a.versionDate}" /></div>
                                                </div>
                                                <table>
                                                    <c:forEach var="line" items="${model.data}" varStatus="forStatus">
                                                        <c:set var="versionEntry" value="${line.left}" />
                                                        <c:if test="${!empty versionEntry}">
                                                            <%@include file="one.jsp" %>
                                                        </c:if>
                                                        <c:if test="${empty versionEntry}">
                                                            <div class="version-entry elem-${line.state}"><div class="inner-version-entry color-${line.state} empty">&nbsp;</div></div>
                                                        </c:if>
                                                    </c:forEach>
                                                </table>
                                            </div>
                                            <div class="clearer"></div>
                                        </div>
                                    </div>
                                </c:if>

                                <c:if test="${empty model.data}">
                                    <div class="half">
                                        <div class="half-inner">
                                            <h1 acs-coral-heading>No versions could be found for this item.</h1>
                                        </div>
                                    </div>
                                </c:if>


                                <c:if test="${!empty model.data}">
                                    <div class="half">
                                        <div class="half-inner">
                                            <div class="version" id="version-${model.b.version}">
                                                <div class="version-header">
                                                    <div class="name"><c:out value="${path}"/></div>
                                                    <div class="v"><c:out value="${model.b.version}"/></div>
                                                    <div class="date"><fmt:formatDate type="both" value="${model.b.versionDate}" /></div>
                                                </div>
                                                <table>
                                                    <c:forEach var="line" items="${model.data}" varStatus="forStatus">
                                                        <c:set var="versionEntry" value="${line.right}" />
                                                        <c:if test="${!empty versionEntry}">
                                                            <%@include file="one.jsp" %>
                                                        </c:if>
                                                        <c:if test="${empty versionEntry}">
                                                            <div class="version-entry elem-${line.state}"><div class="inner-version-entry color-${line.state} empty">&nbsp;</div></div>
                                                        </c:if>
                                                    </c:forEach>
                                                </table>
                                            </div>
                                            <div class="clearer"></div>
                                        </div>
                                    </div>
                                </c:if>

                                <c:if test="${empty model.data}">
                                    <div class="half">
                                        <div class="half-inner">
                                            <h1 acs-coral-heading>No versions could be found for this item.</h1>
                                        </div>
                                    </div>
                                </c:if>

                            </div>
                        </div>

                        <div class="clearer"></div>
                    </section>

                </div>
            </div>
        </div>

        <%-- Register angular app; Decreases chances of collisions w other angular apps on the page (ex. via injection) --%>
        <script type="text/javascript">
            angular.bootstrap(document.getElementById('acs-commons-page-compare'),
                ['PageCompare']);
        </script>
    </div>

</div>

</body>
</html>