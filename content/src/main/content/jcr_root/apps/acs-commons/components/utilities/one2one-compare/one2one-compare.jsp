<%@page session="false" contentType="text/html; charset=utf-8" pageEncoding="UTF-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling" %>
<%@taglib prefix="cq" uri="http://www.day.com/taglibs/cq/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<cq:defineObjects />
<sling:adaptTo adaptable="${slingRequest}" adaptTo="com.adobe.acs.commons.one2one.model.One2OneCompareModel" var="model"/>

<!doctype html>
<html class="coral-App">
<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">

    <title>One-to-one Version Compare | ACS AEM Commons</title>

    <link rel="shortcut icon" href="${favicon}"/>

    <cq:includeClientLib css="acs-commons.one2one-compare"/>
    <cq:includeClientLib js="acs-commons.one2one-compare"/>

</head>

<body class="coral--light">

    <div id="acs-commons-one2one-compare">
        <header acs-coral-tools-header data-context-path="${request.contextPath}" data-page-path="${currentPage.path}.html" data-title="One-to-one Compare"></header>

        <div class="page" role="main"
                 ng-controller="MainCtrl"
                 ng-init="app.resource = '${model.resourcePathA}'; app.resourceB = '${model.resourcePathB}'; app.home = '${request.contextPath}${currentPage.path}.html'; app.a = '${model.versionA}'; app.b = '${model.versionB}'; init();">

            <div ng-show="notifications.length > 0"
                 class="notifications">
                <div ng-repeat="notification in notifications">
                    <div acs-coral-alert data-alert-type="{{ notification.type }}"
                        data-alert-title="{{ notification.title }}"
                        data-alert-message="{{ notification.message }}"></div>
                </div>
            </div>

            <div class="content">
                <div class="content-container">
                    <div class="content-container-inner">

                        <h1 acs-coral-heading>One-to-one Compare</h1>
                        
                        <div class="search">
                            <div class="half">
                                <div class="half-inner border">
                                    <h2 acs-coral-heading>A</h2>
                                    <input type="text" class="coral-Textfield" placeholder="Enter path to resource" ng-model="app.resource" ng-change="dirty()" ng-blur="blur()">
                                    <c:if test="${!empty model.resourcePathA && !empty model.versionSelectionsA}">
                                        <select class="select" ng-model="app.a" ng-change="analyse()">
                                            <c:forEach var="versionSelection" items="${model.versionSelectionsA}">
                                                <option value="${versionSelection.name}"><c:out value="${versionSelection.name}" />, <fmt:formatDate type="both" value="${versionSelection.date}" /></option>
                                            </c:forEach>
                                        </select>
                                    </c:if>
                                </div>
                            </div>
                            <div class="half">
                                <div class="half-inner border">
                                    <h2 acs-coral-heading>B</h2>
                                    <input type="text" class="coral-Textfield" placeholder="Enter optional path to second resource" ng-model="app.resourceB" ng-change="dirty()" ng-blur="blur()">
                                    <c:if test="${!empty model.resourcePathA && !empty model.versionSelectionsB}">
                                        <select class="select" name="b" ng-model="app.b" ng-change="analyse()">
                                            <c:forEach var="versionSelection" items="${model.versionSelectionsB}">
                                                <option value="${versionSelection.name}"><c:out value="${versionSelection.name}" />, <fmt:formatDate type="both" value="${versionSelection.date}" /></option>
                                            </c:forEach>
                                        </select>
                                    </c:if>
                                </div>
                            </div>
                            <!--<div class="clearer"></div>
                            <div class="button-row">
                                <button class="update coral-Button coral-Button--primary" ng-click="analyse()">Update</button>
                            </div>-->
                        </div>

                        <section class="differentResources-{{app.compareDifferentResources()}}">
                            <div class="content">
                                <div>


                                    <c:set var="path" value="${model.resourcePathA}" />
                                    <c:set var="evolutionItem" value="${model.evolutionA}" />
                                    <c:set var="evoCounter" value="1" />

                                    <c:if test="${!empty evolutionItem}">
                                        <%@include file="one.jsp" %>
                                    </c:if>

                                    <c:if test="${empty model.evolutionA}">
                                        <div class="half">
                                            <div class="half-inner">
                                                <h1 acs-coral-heading>No versions could be found for this item.</h1>
                                            </div>
                                        </div>
                                    </c:if>

                                    <c:if test="${not empty model.resourcePathB}">
                                        <c:set var="path" value="${model.resourcePathB}" />
                                    </c:if>
                                    <c:set var="evolutionItem" value="${model.evolutionB}" />
                                    <c:set var="evoCounter" value="2" />
                                    <c:if test="${!empty evolutionItem}">
                                        <%@include file="one.jsp" %>
                                    </c:if>

                                    <c:if test="${empty model.evolutionB}">
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

                        <section class="coral-Well differentResources-{{app.compareDifferentResources()}}">
                            <div class="options">
                                <h2 acs-coral-heading>Configuration</h2>
                                <label acs-coral-checkbox><input type="checkbox" ng-model="app.paintConnections"><span>Paint Connections</span></label>
                                <label acs-coral-checkbox class="hideUnchanged"><input type="checkbox" ng-model="app.hideUnchanged"><span>Hide Unchanged</span></label>
                            </div>
                            <div class="legend">
                                <h2 acs-coral-heading>Legend</h2>
                                <div class="status-added">added</div>
                                <div class="status-changed">changed</div>
                                <div class="status-removed">removed in next version</div>
                                <div class="status-added-removed"><div class="inner-version-entry">added and removed in next version</div></div>
                                <div class="status-changed-removed"><div class="inner-version-entry">changed and removed in next version</div></div>
                            </div>
                        </section>

                    </div>
                </div>
            </div>

            <%-- Register angular app; Decreases chances of collisions w other angular apps on the page (ex. via injection) --%>
            <script type="text/javascript">
                angular.bootstrap(document.getElementById('acs-commons-one2one-compare'),
                        ['One2OneCompare']);
            </script>
        </div>

    </div>
    
</body>
</html>