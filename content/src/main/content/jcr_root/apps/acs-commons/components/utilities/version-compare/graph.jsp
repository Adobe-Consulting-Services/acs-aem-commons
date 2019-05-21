<c:if test="${empty model.evolution.evolutionItems}">
    <h1 acs-coral-heading>No versions could be found for this item.</h1>
</c:if>
<c:forEach var="evolutionItem" items="${model.evolution.evolutionItems}" varStatus="evoCounter">
    <div class="version current-${evolutionItem.current}" id="version-${evolutionItem.versionName}"
        ng-show="showVersion('version-${evolutionItem.versionName}')">
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