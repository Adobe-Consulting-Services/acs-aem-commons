/* 
 * Copyright 2017 Adobe.
 *
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
 */

/* global Blockly */
var MCP = {
    editorContainer : {},
    editor : {},
    workspace : {},
    script : {
        name : "Test script",
        nodeName : "test-script"
    },
    initEditor : function() {
        MCP.editorContainer = document.getElementById('blocklyArea');
        MCP.editor = document.getElementById('blocklyDiv');
        MCP.handleResize();
        MCP.workspace = Blockly.inject(MCP.editor, {
                path: '/apps/acs-commons/components/utilities/manage-controlled-processes/clientlibs/blockly/',
                toolbox: document.getElementById('toolbox'), 
                sounds: true,
                zoom: {
                    controls: true,
                    wheel: false
                },
                grid: {
                    spacing: 20,
                    length: 21,
                    colour: '#aaf',
                    snap: true
                }
            });
        window.addEventListener('resize', MCP.handleResize, false);
        window.setTimeout(MCP.handleResize, 100);
    },
    handleResize : function() {
        var element = MCP.editorContainer, x = 0, y = 0;
        // Compute the absolute coordinates and dimensions of area.
        do {
            x += element.offsetLeft;
            y += element.offsetTop;
            element = element.offsetParent;
        } while (element);
        // Move editor into position and adjust its size accordingly.
        MCP.editor.style.left = x + 'px';
        MCP.editor.style.top = y + 'px';
        MCP.editor.style.width = MCP.editorContainer.offsetWidth + 'px';
        MCP.editor.style.height = MCP.editorContainer.offsetHeight + 'px';
    },
    load : function(path) {
        if (path) {
            MCP.script.path = path;            
        }
        jQuery.ajax({
            url: MCP.script.path + ".json",
            success: function(script) {
                var dom = Blockly.Xml.textToDom(script['script-content']);
                Blockly.Xml.domToWorkspace(dom, MCP.workspace);
            },
            dataType: "json"
        });
    },
    save : function() {
        jQuery.post(MCP.script.path, {
            "jcr:primaryType": "nt:unstructured",
            "script-content": MCP.getScriptXML()
        });
    },
    getScriptXML: function() {
        return Blockly.Xml.domToText(Blockly.Xml.workspaceToDom(MCP.workspace));
    }
    
};

MCP.script.path="/etc/acs-commons/manage-controlled-processes/jcr:content/models/test-model";
jQuery(window).load(function() {
    MCP.initEditor();
    MCP.load();
    MCP.workspace.addChangeListener(MCP.save);
});
