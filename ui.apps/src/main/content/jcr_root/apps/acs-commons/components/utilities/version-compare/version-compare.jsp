<%@page session="false" contentType="text/html; charset=utf-8" pageEncoding="UTF-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling" %>
<%@taglib prefix="cq" uri="http://www.day.com/taglibs/cq/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<cq:defineObjects />
<sling:adaptTo var="model" adaptable="${slingRequest}" adaptTo="com.adobe.acs.commons.version.model.EvolutionModel"/>

<!doctype html>
<html class="coral-App">
<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">

    <title>Version Compare | ACS AEM Commons</title>

    <link rel="shortcut icon" href="${favicon}"/>

    <cq:includeClientLib css="acs-commons.version-compare"/>
    <cq:includeClientLib js="acs-commons.version-compare"/>
</head>

<body class="coral--light">
    <%@include file="page-header.jsp" %>

    <div id="acs-commons-version-comparison">

        <div class="page" role="main"
                 ng-controller="MainCtrl"
                 ng-init="app.resource = '${model.resourcePath}'; app.home = '${request.contextPath}${currentPage.path}.html'; init();">

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

                    <div acs-coral-alert data-alert-type="notice"
                        data-alert-title="Notice"
                        data-alert-message="Please note, the current state of this tool only shows linear version paths."></div>

                        <h1 acs-coral-heading>Version Compare</h1>
                        
                        <div class="search">
                            <input type="text" class="coral-Textfield" placeholder="Enter path to resource" ng-model="app.resource">
                            <button class="coral-Button coral-Button--primary" ng-click="analyse()">Show Versions</button>
                        </div>

                        
                   <c:if test="${!empty model.resourcePath && !empty model.evolution.evolutionItems}">
                        <section class="coral-Well">
                            <%@include file="options.jsp" %>
                        </section>

                        <section>
                            <div class="content">
                                <%@include file="graph.jsp" %>
                            </div>
                        </section>
                    </c:if>
                    </div>
                </div>
            </div>

            <%-- Register angular app; Decreases chances of collisions w other angular apps on the page (ex. via injection) --%>
            <script type="text/javascript">
                angular.bootstrap(document.getElementById('acs-commons-version-comparison'),
                        ['versionComparator']);
            </script>
        </div>

    </div>
    
</body>
</html>