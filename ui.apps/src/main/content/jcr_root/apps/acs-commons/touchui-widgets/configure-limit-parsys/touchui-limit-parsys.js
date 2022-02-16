/*
 * #%L
 * ACS AEM Commons Package
 * %%
 * Copyright (C) 2013 Adobe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 *
 * Extends /libs/foundation/components/parsys to limit the components that be added
 * using drag/drop, copy/paste or insert actions
 * To enable limit feature set the property acsComponentsLimit with required limit on design node
 * eg. to limit the components to 4 on rightpar of /content/geometrixx/en.html
 * set acsComponentsLimit=4 on /etc/designs/geometrixx/jcr:content/homepage/rightpar
 */
(function ($, $document) {
    "use strict";

    var ACS_COMPONENTS_LIMIT = "acsComponentsLimit";
    /** AEM 6.2 does not have the function resolveProperty in util.js and thus
    * breaks authoring on a supported version. To deal with this we need to detect
    * if the function is available and fallback to 6.2 functions if it is not.
    */
    function correctlyResolveProperty(design, path){
      if ("resolveProperty" in Granite.author.util) {
        //function was found, use it.
        return Granite.author.util.resolveProperty(design, path);
      }else{
        //didn't find the function in util.js, we'll use _discover instead.
        return Granite.author.components._discover(design, path);
      }
     }
    /**
     * mostly taken over from /libs/cq/gui/components/authoring/editors/clientlibs/core/js/storage/components.js _findAllowedComponentsFromPolicy
     */
    function _findPropertyFromPolicy(editable, design, propertyName) {
        var cell = correctlyResolveProperty(design, editable.config.policyPath);

        if (!cell || !cell[propertyName]) {
            // Inherit property also from its parent (if not set in the local policy path)
            var parent = Granite.author.editables.getParent(editable);

            while (parent && !(cell && cell[propertyName])) {
                cell = correctlyResolveProperty(design, parent.config.policyPath);
                parent = Granite.author.editables.getParent(parent);
            }
        }
        if (cell && cell[propertyName]) {
            return cell[propertyName];
        }
        return null;
    }

    /**
     * mostly taken over from /libs/cq/gui/components/authoring/editors/clientlibs/core/js/storage/components.js _findAllowedComponentsFromDesign
     * Returns the value of the given property name extracted from the given design configuration object (also supports content policies)
     */
     function _findPropertyFromDesign(editable, design, propertyName) {
        if (editable && editable.config) {
            if (editable.config.policyPath) {
                return _findPropertyFromPolicy(editable, design, propertyName);
            } else {
                // All cell search paths
                var cellSearchPaths = editable.config.cellSearchPath;

                if (cellSearchPaths) {
                    for (var i = 0; i < cellSearchPaths.length; i++) {
                        var cell = correctlyResolveProperty(design, cellSearchPaths[i]);

                        if (cell && cell[propertyName]) {
                            return cell[propertyName];
                        }
                    }
                }
            }
        }
        return null;
    }

    function showErrorAlert(message, title){
        var fui = $(window).adaptTo("foundation-ui"),
            options = [{
                text: "OK",
                warning: true
            }];

        message = message || "Unknown Error";
        title = title || "Error";

        fui.prompt(title, message, "error", options);
    }

    function getChildEditables(parsys){
        var editables = Granite.author.edit.findEditables(),
            children = [], parent;

        _.each(editables, function(editable){
            parent = editable.getParent();

            if(parent && (parent.path === parsys.path)){
                children.push(editable);
            }
        });

        return children;
    }

    function isWithinLimit(parsysEditable, itemsToAdd){
        var isWithin = true, currentLimit = "";

        currentLimit = _findPropertyFromDesign(parsysEditable, Granite.author.pageDesign, ACS_COMPONENTS_LIMIT);
        if (currentLimit === null) {
            return true;
        }
        var limit = parseInt(currentLimit);
        var children = getChildEditables(parsysEditable);
        itemsToAdd = itemsToAdd ? itemsToAdd : 1;
        isWithin = children.length - 1 + itemsToAdd <= limit;

        if(!isWithin){
            showErrorAlert("Limit of paragraphs within this paragraph system exceeded, allowed only up to " + currentLimit + " paragraphs.");
        }

        return isWithin;
    }

    function extendComponentDrop(){
        var dropController = Granite.author.ui.dropController,
            compDragDrop;

        if (dropController !== undefined) {
            compDragDrop = dropController.get(Granite.author.Component.prototype.getTypeName());

            //handle drop action
            if (compDragDrop !== undefined) {
                //handle drop action
                compDragDrop.handleDrop = function(dropFn){
                    return function (event) {
                        if(!isWithinLimit(event.currentDropTarget.targetEditable.getParent())){
                            return;
                        }
                        return dropFn.call(this, event);
                    };
                }(compDragDrop.handleDrop);
            }

            //handle paste action
            var pasteAction = Granite.author.edit.Toolbar.defaultActions.PASTE;
            // overwrite both execute and handler as both seem to be used
            pasteAction.execute = pasteAction.handler = function(pasteHandlerFn){
                return function (editableBefore) {
                    // only prevent copy but not move operations (if previous operation was cut)
                    if(!Granite.author.clipboard.shouldCut()) {
                        if(!isWithinLimit(editableBefore.getParent(), Granite.author.clipboard.length)){
                            return;
                        }
                    }
                    return pasteHandlerFn.call(this, editableBefore);
                };
            }(pasteAction.execute);

            // handle insert action
            var insertAction = Granite.author.edit.Toolbar.defaultActions.INSERT;
            // overwrite both execute and handler (for doubleclick and "+" icon click functionality)
            insertAction.execute = insertAction.handler = function(insertHandlerFn){
                return function(editableBefore, param, target){
                    if(!isWithinLimit(editableBefore.getParent())){
                        return;
                    }
                    return insertHandlerFn.call(this, editableBefore, param, target);
                };
            }(insertAction.execute);
        }
    }

    $(function() {
        if (Granite && Granite.author && Granite.author.edit && Granite.author.Component &&
                Granite.author.ui && Granite.author.ui.dropController) {
            extendComponentDrop();
        }
    });
}(jQuery, jQuery(document)));
