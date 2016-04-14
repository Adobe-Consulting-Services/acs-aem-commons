<%@page session="false" contentType="text/html; charset=utf-8" pageEncoding="UTF-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling" %>
<%@taglib prefix="cq" uri="http://www.day.com/taglibs/cq/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<cq:defineObjects />
<sling:adaptTo adaptable="${slingRequest}" adaptTo="com.adobe.acs.commons.version.model.EvolutionModel" var="model"/>

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

    <div id="acs-commons-version-comparison">
        <header acs-coral-tools-header data-context-path="${request.contextPath}" data-page-path="${currentPage.path}.html" data-title="Version Compare"></header>

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
                            <div class="options">
                                <h2 acs-coral-heading>Configuration</h2>
                                <label acs-coral-checkbox><input type="checkbox" ng-model="app.paintConnections"><span>Paint Connections</span></label>
                                <label acs-coral-checkbox><input type="checkbox" ng-model="app.hideUnchanged"><span>Hide Unchanged</span></label>
                            </div>
                            <div class="options">
                                <h2 acs-coral-heading>Hide Versions</h2>
                                <c:forEach var="evolutionItem" items="${model.evolution.evolutionItems}" varStatus="evoCounter">
                                    <label acs-coral-checkbox><input type="checkbox" ng-model="app.hideVersions['version-${evolutionItem.versionName}']"><span>${evolutionItem.versionName}</span></label>
                                </c:forEach>
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

                        <section>
                            <div class="content">
                                <div>
                                    <c:forEach var="evolutionItem" items="${model.evolution.evolutionItems}" varStatus="evoCounter" >
                                        <div class="version current-${evolutionItem.current}" id="version-${evolutionItem.versionName}" ng-show="showVersion('version-${evolutionItem.versionName}')">
                                            <div class="version-header">
                                                <div class="name"><c:out value="${evolutionItem.versionName}"/></div>
                                                <div class="date"><fmt:formatDate type="both" value="${evolutionItem.versionDate}" /></div>
                                            </div>
                                            <c:forEach var="versionEntry" items="${evolutionItem.versionEntries}" varStatus="entryCounter">
                                                <a href="#popover-${versionEntry.uniqueName}-${evoCounter.index}" data-toggle="popover" data-point-from="right" data-align-from="left">
                                                    <div class="version-entry type-${versionEntry.resource} status-${versionEntry.status}"
                                                         id="${versionEntry.uniqueName}-${evoCounter.index}" 
                                                         ${versionEntry.status == "" ? "ng-show='!app.hideUnchanged'" : ""} 
                                                         ng-init="addConnection({'source':'${versionEntry.uniqueName}-${evoCounter.index}', 'target':'${versionEntry.uniqueName}-${evoCounter.index + 1}', 'isCurrent':${evolutionItem.current}})">
                                                        <div class="inner-version-entry depth-${versionEntry.depth}">
                                                            <span class="key"><c:out value="${versionEntry.name}"/>:</span>
                                                            <span class="value"><c:out value="${versionEntry.valueStringShort}"/></span>
                                                            <div id="popover-${versionEntry.uniqueName}-${evoCounter.index}" class="coral-Popover">
                                                                <div class="coral-Popover-content u-coral-padding">
                                                                    <c:out value="${versionEntry.valueString}"/>
                                                                </div>
                                                            </div>
                                                        </div>
                                                    </div>
                                                </a> 
                                            </c:forEach>
                                        </div>
                                    </c:forEach>
                                    <c:if test="${empty model.evolution.evolutionItems}">
                                        <h1 acs-coral-heading>No versions could be found for this item.</h1>
                                    </c:if>
                                </div>
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