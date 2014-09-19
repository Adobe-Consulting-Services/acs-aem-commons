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
 */
/*global CQ: false, ACS: false */
/**
 * @class ACS.CQ.form.DDPathField
 * @extends CQ.form.PathField
 * <p>The DDPathField widget is a drop-in replacement for the normal PathField with the additional functionality of being able to receive drag-and-drop
 * paths from the Content Finder.</p>
 * @constructor
 * Creates a new DDPathField.
 * @param {Object} config The config object
 */
CQ.Ext.ns("ACS.CQ.form");
ACS.CQ.form.DDPathField = CQ.Ext.extend(CQ.form.PathField, {
    
    /**
     * @cfg {String} ddGroups
     * Drag &amp; drop which will be accepted by this widget
     * (defaults to "asset", "page")
     */

    constructor : function(config) {
        config = CQ.Util.applyDefaults(config, {
            "ddGroups" : [ CQ.wcm.EditBase.DD_GROUP_ASSET,
                          CQ.wcm.EditBase.DD_GROUP_PAGE ],
            "listeners" : {
                "render" : this.registerDragAndDrop,
                "destroy" : this.unregisterDragAndDrop
            }
            
        });
        ACS.CQ.form.DDPathField.superclass.constructor.call(this, config);
    },

    registerDragAndDrop : function() {
        var field = this,
            dialog = this.findParentByType('dialog'),
            inMultiField = CQ.Ext.isDefined(this.findParentByType('multifield')),
            target,
            i;
        if (this.ddGroups) {
            if (typeof (this.ddGroups) === "string") {
                this.ddGroups = [ this.ddGroups ];
            }
            target = new CQ.wcm.EditBase.DropTarget(this.el, {
                "notifyDrop" : function(dragObject, evt, data) {
                    var record, path;
                    if (dragObject && dragObject.clearAnimations) {
                        dragObject.clearAnimations(this);
                    }
                    if (dragObject.isDropAllowed(this)) {
                        if (data.records && data.single) {
                            record = data.records[0];
                            path = record.get("path");
                            field.setValue(path);
                            evt.stopEvent();
                            return true;
                        }
                        return false;
                    }
                }
            });

            if (inMultiField) {
                CQ.WCM.registerDropTargetComponent(field);
            }

            dialog.on("activate", function(dialog) {
                var dialogZIndex;
                if (dialog && dialog.el && this.highlight) {
                    dialogZIndex = parseInt(dialog.el.getStyle("z-index"),
                            10);
                    if (!isNaN(dialogZIndex)) {
                        this.highlight.zIndex = dialogZIndex + 1;
                    }
                }
            }, target);
            dialog.on("deactivate", function(dialog) {
                var dialogZIndex;
                if (dialog && dialog.el && this.highlight) {
                    dialogZIndex = parseInt(dialog.el.getStyle("z-index"),
                            10);
                    if (!isNaN(dialogZIndex)) {
                        this.highlight.zIndex = dialogZIndex + 1;
                    }
                }
            }, target);

            dialog.on("show", function() {
                if (!inMultiField) {
                    CQ.WCM.registerDropTargetComponent(field);
                }
            }, target);

            dialog.on("hide", function() {
                if (!inMultiField) {
                    CQ.WCM.unregisterDropTargetComponent(field);
                }
            }, target);

            for (i = 0; i < this.ddGroups.length; i++) {
                target.addToGroup(this.ddGroups[i]);
            }
            target.removeFromGroup(CQ.wcm.EditBase.DD_GROUP_DEFAULT);
            this.dropTargets = [ target ];
        }
    },

    unregisterDragAndDrop : function() {
        var field = this,
            inMultiField = CQ.Ext.isDefined(this.findParentByType('multifield'));

        if (inMultiField) {
            CQ.WCM.unregisterDropTargetComponent(field);
        }
    }
});
ACS.CQ.form.DDPathField.prototype.getDropTargets = CQ.Ext.form.Field.prototype.getDropTargets;

CQ.Ext.reg("ddpathfield", ACS.CQ.form.DDPathField);
