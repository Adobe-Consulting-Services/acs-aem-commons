<c:if test="${empty model.evolution.evolutionItems}">
    <h1 acs-coral-heading>No versions could be found for this item.</h1>
</c:if>
<c:forEach var="evolutionItem" items="${model.evolution.evolutionItems}" varStatus="evoCounter">
    <div class="version current-${evolutionItem.current}" id="version-${evoCounter.index}"
         ng-init="addVersion({
             'id': 'version-${evoCounter.index}',
             'index': ${evoCounter.index}
         })">
        <div class="version-header">
            <div class="name"><c:out value="${evolutionItem.versionName}"/></div>
            <div class="date"><fmt:formatDate type="both" value="${evolutionItem.versionDate}" /></div>
        </div>
        <c:forEach var="versionEntry" items="${evolutionItem.versionEntries}">
            <a href="#popover-${versionEntry.uniqueName}-${evoCounter.index}"
                 data-toggle="popover" data-point-from="right" data-align-from="left">
                <div class="version-entry type-${versionEntry.resource} status-${versionEntry.status} ${versionEntry.status == '' ? 'unchanged' : ''}"
                     id="${versionEntry.uniqueName}-${evoCounter.index}"
                     ng-init="addNode({
                         'id': '${versionEntry.uniqueName}-${evoCounter.index}',
                         'version': ${evoCounter.index},
                         'name': '${versionEntry.uniqueName}',
                         'isCurrent': ${evolutionItem.current},
                         'changed': ${not empty versionEntry.status}
                     })">
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