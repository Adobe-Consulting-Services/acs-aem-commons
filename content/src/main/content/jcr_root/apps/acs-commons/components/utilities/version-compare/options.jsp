<div class="options">
    <h2 acs-coral-heading>Configuration</h2>
    <label acs-coral-checkbox><input type="checkbox" ng-model="app.paintConnections"/><span>Paint Connections</span></label>
    <label acs-coral-checkbox><input type="checkbox" ng-model="app.hideUnchanged"/><span>Hide Unchanged</span></label>
</div>
<div class="options">
    <h2 acs-coral-heading>Hide Versions</h2>
    <c:forEach var="evolutionItem" items="${model.evolution.evolutionItems}" varStatus="evoCounter">
        <label acs-coral-checkbox>
            <input type="checkbox" ng-model="app.hideVersions['${evoCounter.index}']"/>
            <span>${evolutionItem.versionName}</span>
        </label>
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