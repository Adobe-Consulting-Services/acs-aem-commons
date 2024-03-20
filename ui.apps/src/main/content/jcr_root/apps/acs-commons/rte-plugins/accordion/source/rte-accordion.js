/*
 * #%L
 * ACS AEM Commons Package
 * %%
 * Copyright (C) 2016 Adobe
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
 * Accordion TouchUI RTE plugin
 *
 * Steps:
 *
 * 1) Create ACS Commons plugin nt:unstructured node "acs-commons"
 *      eg. /apps/<project>/components/text/dialog/items/tab1/items/text/rtePlugins/acs-commons
 * 2) Add property "features" of type String[] and single value "rte-accordion"
 * 3) Add to the fullscreen uiSettings "toolbar" property. Include "acs-commons#rte-accordion" in the desired location
 *      e.g. /apps/<project>/components/text/dialog/items/tab1/items/text/uiSettings/cui/fullscreen
 * 4) To use the provided minimal generic styling, add "acs-commons.rte-accordion" to the embed property of your Client Library Folder
 *
 */
(function($, CUI){
    var RTEAccordion = {
        GROUP: "acs-commons",
        FEATURE: "rte-accordion",
        CLASS: "acs-commons-rte-accordion"
    };
  
    // Initialize the Accordion RTE Plugin
    RTEAccordion.TouchUIAccordionPlugin = new Class({
        extend: CUI.rte.plugins.Plugin,
        accordionUI: null,
  
        getFeatures: function() {
            return [ RTEAccordion.FEATURE ];
        },

        initializeUI: function(tbGenerator) {
            var plg = CUI.rte.plugins;
  
            this.accordionUI = tbGenerator.createElement(RTEAccordion.FEATURE, this, true, "Accordion");
            tbGenerator.addElement(RTEAccordion.GROUP, plg.Plugin.SORT_FORMAT, this.accordionUI, 120);
            tbGenerator.registerIcon(RTEAccordion.GROUP + "#" + RTEAccordion.FEATURE, "coral-Icon coral-Icon--feed");
        },
  
        // Triggers on click of the RTE button
        execute: function(id) {
            // Relays this click event to the Command registered with this ID (in this case, set below in AccordionCmd)
            this.editorKernel.relayCmd(id);
        },
  
        // This marks the accordion icon as active or not based on the selected DOM element in selDef
        updateState: function(selDef) {
            // Returns true or false whether this plugin is active in the selected DOM element in selDef
            var hasUC = this.editorKernel.queryState(RTEAccordion.FEATURE, selDef);

            if ( this.accordionUI !== null ) {
                // Set as selected (Adds a darker background color to the icon)
                this.accordionUI.setSelected(hasUC);
            }

            if (hasUC === true) {
                // Change the accordion icon to use the one with a plus to indicate that this adds another accordion item to the existing accordion
                $("[data-action='" + RTEAccordion.GROUP + "#" + RTEAccordion.FEATURE + "']").removeClass("coral-Icon--feed").addClass("coral-Icon--feedAdd");
            } else {
                // Restore the original icon in case it was changed above
                $("[data-action='" + RTEAccordion.GROUP + "#" + RTEAccordion.FEATURE + "']").removeClass("coral-Icon--feedAdd").addClass("coral-Icon--feed");
            }
        }
    });

    // Accordion Plugin Commands
    RTEAccordion.AccordionCmd = new Class({
        extend: CUI.rte.commands.Command,
  
        isCommand: function(cmdStr) {
            return (cmdStr.toLowerCase() == RTEAccordion.FEATURE);
        },
  
        getProcessingOptions: function() {
            var cmd = CUI.rte.commands.Command;
            return cmd.PO_SELECTION | cmd.PO_BOOKMARK | cmd.PO_NODELIST;
        },
  
        // The base node for this plugin is a div with an accordion class, used in multiple places to check and add this element
        _getTagObject: function() {
            return {
                "tag": "div",
                "attributes": {
                    "class" : RTEAccordion.CLASS
                }
            };
        },

        // We don't want to add this plugin inside another element, this code will find the highest parent for the current context
        _getHighestParent: function ( context, node ){
            var common = CUI.rte.Common;
            // Get the nearest parent of the passed node
            var parent = common.getParentNode(context, node);

            // If the returned parent has an accordion class, this is the higest parent for this accordion so return it
            if ( parent.className == RTEAccordion.CLASS ) {
                return parent;
            } 
            // We don't want to go too far up the dom, if the next parent after this is null return this node
            else if ( common.getParentNode(context, parent) === null ) {
                return node;
            } 
            // No appropriate results were found yet, recursively call this method again to go up another level
            else {
                return this._getHighestParent(context, parent);
            }
        },

        execute: function(execDef) {
            var common = CUI.rte.Common;
            var dpr = CUI.rte.DomProcessor;

            // Get the current context that this execute method was triggered in
            var context = execDef.editContext;
            var selection = execDef.selection;
            var startNode = selection.startNode;
            var tagObj = this._getTagObject();

            if (!selection) {
                return;
            }

            // Set up our DOM elements for the Accordion, defaults to an unordered list with a single item
            var accordionItem = context.createElement('li');
            accordionItem.className = 'accordion-item';
            var accordionHTML = '';
            accordionHTML += '<h3 class="accordion-header">Accordion Header</h3>';
            accordionHTML += '<div class="accordion-content">';
            accordionHTML += '    <p>Accordion Content.</p>';
            accordionHTML += '</div>';
            accordionItem.innerHTML = accordionHTML;
            
            // Find the nearest parent
            var parent = this._getHighestParent(context, startNode);

            // If the nearest parent is the .accordion container div, we want to only add an accordionItem to the end
            if ( parent.className == RTEAccordion.CLASS ) {
                parent.firstChild.appendChild(accordionItem);
            } 
            // Otherwise we want to add a new .accordion container with a new list and item inside
            else {
                var accordionDom = dpr.createNode(context, tagObj.tag, tagObj.attributes);
                var accordionlist = context.createElement('ul');
                accordionlist.appendChild(accordionItem);
                accordionDom.appendChild(accordionlist);
                parent.parentNode.insertBefore(accordionDom, parent.nextSibling);
            }
        },
  
        queryState: function(selectionDef) {
            var common = CUI.rte.Common;
            var context = selectionDef.editContext;
  
            var selection = selectionDef.selection;
            var startNode = selection.startNode;
            var tagObj = this._getTagObject();
  
            return (common.getTagInPath(context, startNode, tagObj.tag, tagObj.attributes) !== null);
        }
    });
  
    CUI.rte.plugins.PluginRegistry.register(RTEAccordion.GROUP, RTEAccordion.TouchUIAccordionPlugin);
    CUI.rte.commands.CommandRegistry.register(RTEAccordion.FEATURE, RTEAccordion.AccordionCmd);
})($, window.CUI);
