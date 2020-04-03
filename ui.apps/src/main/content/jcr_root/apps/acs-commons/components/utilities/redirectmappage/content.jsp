<%@ page session="false" contentType="text/html" pageEncoding="utf-8" %>
<%@include file="/libs/foundation/global.jsp" %>
<%@taglib prefix="sling2" uri="http://sling.apache.org/taglibs/sling" %>
<cq:setContentBundle />
<div ng-controller="MainCtrl" ng-init="app.uri = '${resourcePath}'; init();">

    <br/><hr/><br/>
    
    <c:set var="redirectMap" value="${sling2:getRelativeResource(resource, 'redirectMap.txt')}" />
    <coral-tabview>
        <coral-tablist target="main-panel-1">
            <coral-tab><fmt:message key="Configure" /></coral-tab>
            <coral-tab><fmt:message key="Edit Entries" /></coral-tab>
            <coral-tab><fmt:message key="Preview" /></coral-tab>
        </coral-tablist>
        <coral-panelstack id="main-panel-1">
            <coral-panel class="coral-Well">
                <section>
                    <h2 class="coral-Heading coral-Heading--2">
                        <fmt:message key="Configure Redirect Map" />
                    </h2>
                    <p>
                        <fmt:message key="The redirect map file will be combined with the redirects configured in AEM to create the final set of redirects." />
                    </p>
                    <form action="${resource.path}" method="post" class="coral-Form--aligned" id="fn-acsCommons-update-redirect" ng-submit="updateRedirectMap($event)" enctype="multipart/form-data">
                        <input type="hidden" name="./redirectMap.txt@TypeHint" value="nt:file" />
                        
                        <div class="coral-Form-fieldwrapper">
                            <label class="coral-Form-fieldlabel" id="label-vertical-inputgroup-1">
                                <fmt:message key="Redirect Map File" /> *
                            </label>
                            <div class="coral-InputGroup coral-Form-field">
                                <input is="coral-Textfield" class="coral-InputGroup-input coral-Textfield" type="file" name="./redirectMap.txt" accept=".txt" />
                            </div>
                            <coral-icon class="coral-Form-fieldinfo coral-Icon coral-Icon--infoCircle coral-Icon--sizeS" icon="infoCircle" size="S" id="file-info" role="img" aria-label="info circle"></coral-icon>
                            <coral-tooltip variant="info" placement="right" target="#file-info" class="coral3-Tooltip coral3-Tooltip--info" aria-hidden="true" tabindex="-1" role="tooltip" style="display: none;">
                                <coral-tooltip-content>
                                    <fmt:message key="This file should be a space-delimited file with the first column containing the source path and the second column containing the redirect destination." />
                                </coral-tooltip-content>
                            </coral-tooltip>
                        </div>
                        <c:if test="${redirectMap != null}">
                            <a class="coral-Link" href="${redirectMap.path}">
                                <fmt:message key="Download Current Redirect Map File" />
                            </a>
                        </c:if><br/><br/>
                        <div class="coral-Form-fieldwrapper" >
                            <button class="coral-Button coral-Button--primary"><fmt:message key="Upload" /></button>
                        </div>
                    </form>
                </section>
                <section>
                    <h2 class="coral-Heading coral-Heading--2">Redirect Configuration</h2>
                    <p>
                        <fmt:message key="Redirect configurations are used to gather vanity redirects to AEM pages based on a multivalued property and the mapping configuration specified. The property and path fields are used to form a query to find available redirects. For example, specifying / as the path and sling:vanityPath as the property will load all vanity paths in the system." />
                    </p>
                    <c:set var="redirectParent" value="${sling2:getRelativeResource(resource, 'redirects')}" />
                    <c:forEach var="redirects" items="${sling2:listChildren(redirectParent)}">
                        <cq:include path="${redirects.path}" resourceType="${redirects.resourceType}" />
                    </c:forEach>
                    <form action="${resource.path}/redirects/*" method="post" class="coral-Form--aligned" id="fn-acsCommons-add-redirectconfig" ng-submit="postValues($event,'fn-acsCommons-add-redirectconfig')">
                        <input type="hidden" name="sling:resourceType" value="acs-commons/components/utilities/redirects" />
                        <input type="hidden" name="jcr:created" />
                        <input type="hidden" name="jcr:createdBy" />
                        <div class="coral-Form-fieldwrapper" >
                            <button class="coral-Button coral-Button--primary">+ <fmt:message key="Redirect Configuration" /></button>
                        </div>
                    </form>
                </section>
            </coral-panel>
            <sling2:adaptTo adaptable="${resource}" adaptTo="com.adobe.acs.commons.redirectmaps.models.RedirectMapModel" var="redirectMapModel" />
            <coral-panel class="coral-Well">
                <section>
                    <h2 class="coral-Heading coral-Heading--2"><fmt:message key="Add Entry" /></h2>
                    <p>
                        <fmt:message key="Add an entry into the redirect map file in the specified location." />
                    </p>
                    <form ng-submit="addEntry()" id="entry-form">
                        <div class="form-row">
                            <label acs-coral-heading>
                                Source
                            </label>
                            <span>
                                <input type="text" name="source" class="coral-Textfield"  ng-required="true" placeholder="Path to redirect"/>
                            </span>
                        </div>
                        <div class="form-row">
                            <label acs-coral-heading>
                                Target
                            </label>
                            <span>
                                <input type="text" name="target" class="coral-Textfield"  ng-required="true" placeholder="URL to redirect to"/>
                            </span>
                        </div>
                        <button is="coral-button" iconsize="S">
                            Add Entry
                        </button>
                    </form>
                </section>
                <section ng-if="invalidEntries.length">
                    <div class="invalid-entries">
                        <h4><fmt:message key="Invalid Redirect Entries"/></h4>
                        <ul>
                            <li ng-repeat="entry in invalidEntries">
                                {{entry.status}}
                            </li>
                        </ul>
                    </div>
                </section>
                <section>
                    <h2 class="coral-Heading coral-Heading--2"><fmt:message key="View Entries" /></h2>
                    <p>
                        <fmt:message key="Select Find Entries to load a list of the entries in the redirect map files and gathered from the configuration. Click the button to remove / edit." />
                    </p>
                    <form ng-submit="filterEntries()" id="filter-form">
                        <input is="coral-textfield" placeholder="* for all or search term" name="filter" value="">
                        <button is="coral-button" iconsize="S">
                            Find Entries
                        </button>
                    </form>
                    <br/>
                    <div class="fixed-height">
                        <table id="entry-table">
                            <thead>
                                <tr>
                                    <th class="narrow-cell">ID</th>
                                    <th>Source</th>
                                    <th>Target</th>
                                    <th>Status</th>
                                    <th>Origin</th>
                                    <th>Edit</th>
                                </tr>
                            </thead>
                            <tbody >
                                <tr ng-repeat="entry in filteredEntries" class="{{entry.valid ? '' : 'entry-invalid'}}">
                                    <td class="narrow-cell">{{entry.id}}</td>
                                    <td title="{{entry.source}}">{{entry.source}}</td>
                                    <td title="{{entry.target}}">
                                        {{entry.target}}
                                    </td>
                                    <td title="{{entry.status}}">{{entry.status}}</td>
                                    <td title="{{entry.origin}}">{{entry.origin}}</td>
                                    <td>
                                        <div ng-switch on="entry.origin">
                                            <div ng-switch-when="File">
                                                <button is="coral-button" icon="edit" iconsize="S" ng-click="editItem(entry.id)"></button>
                                                <button is="coral-button" icon="delete" iconsize="S" ng-click="removeAlert(entry.id)"></button>
                                            </div>
                                            <div ng-switch-default>
                                                <button is="coral-button" icon="edit" iconsize="S" ng-click="openEditor(entry.origin)"></button>
                                            </div>
                                        </div>
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </section>
                <coral-dialog id="edit-entry" closable="on">
                    <coral-dialog-header>Edit Entry</coral-dialog-header>
                    <coral-dialog-content>
                        <form class="coral-Form coral-Form--vertical" id="update-form">
                            <section class="coral-Form-fieldset">
                                <input type="hidden" name="edit-id" />
                                <div class="coral-Form-fieldwrapper">
                                    <label class="coral-Form-fieldlabel" id="label-source" for="edit-source">Source</label>
                                    <input is="coral-textfield" class="coral-Form-field" placeholder="Source path" name="edit-source" />
                                </div>
                                <div class="coral-Form-fieldwrapper">
                                    <label class="coral-Form-fieldlabel" id="label-target" for="edit-target">Target</label>
                                    <input is="coral-textfield" class="coral-Form-field" placeholder="Source path" name="edit-target" />
                                </div>
                                <button is="coral-button" variant="primary" ng-click="saveLine()">Save</button>
                            </section>
                        </form>
                    </coral-dialog-content>
                </coral-dialog>
                <coral-dialog id="remove-confirm" closable="on">
                    <coral-dialog-header>Delete Entry</coral-dialog-header>
                    <coral-dialog-content>
                        Are you sure you want to delete this entry?
                    </coral-dialog-content>
                    <coral-dialog-footer>
                        <button is="coral-button" variant="primary" coral-close="" ng-click="removeLine()">Yes</button>
                    </coral-dialog-footer>
                </coral-dialog>
            </coral-panel>
            <coral-panel class="coral-Well">
                <section>
                    <h2 class="coral-Heading coral-Heading--2"><fmt:message key="Download Preview" /></h2>
                    <a class="coral-Link" href="${resource.path}.redirectmap.txt">
                        <fmt:message key="Download Combined Redirect Map File" />
                    </a>
                    <br/>
                    Published Path: ${resource.path}.redirectmap.txt
                </section>
                <section>
                    <h2 class="coral-Heading coral-Heading--2"><fmt:message key="Preview" /></h2>
                    <pre class="fixed-height">{{redirectMap}}</pre>
                </section>
            </coral-panel>
        </coral-panelstack>
    </coral-tabview>
</div>
