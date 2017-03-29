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
var ScriptEditor = {
    editorContainer : {},
    editor : {},
    workspace : {},
    script : {
    },
    initEditor : function() {
        ScriptEditor.editorContainer = document.getElementById('blocklyArea');
        ScriptEditor.editor = document.getElementById('blocklyDiv');
        if (!ScriptEditor.editor) {
            return;
        }
        ScriptEditor.handleResize();
        ScriptEditor.workspace = Blockly.inject(ScriptEditor.editor, {
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
        window.addEventListener('resize', ScriptEditor.handleResize, false);
        ScriptEditor.workspace.addChangeListener(ScriptEditor.save);
    },
    handleResize : function() {
        var element = ScriptEditor.editorContainer, x = 0, y = 0;
        // Compute the absolute coordinates and dimensions of area.
        do {
            x += element.offsetLeft;
            y += element.offsetTop;
            element = element.offsetParent;
        } while (element);
        // Move editor into position and adjust its size accordingly.
        ScriptEditor.editor.style.left = x + 'px';
        ScriptEditor.editor.style.top = y + 'px';
        ScriptEditor.editor.style.width = ScriptEditor.editorContainer.offsetWidth + 'px';
        ScriptEditor.editor.style.height = ScriptEditor.editorContainer.offsetHeight + 'px';
    },
    load : function(path) {
        jQuery.ajax({
            url: ScriptEditor.script.path + ".json",
            success: function(script) {
                var dom = Blockly.Xml.textToDom(script['script-content']);
                Blockly.Xml.domToWorkspace(dom, ScriptEditor.workspace);
                ScriptEditor.script = {
                    path: path,
                    name: script['jcr:title'] || script.name,
                    nodeName: script.name
                };
            },
            dataType: "json"
        });
    },
    save : function() {
        if (ScriptEditor.script.path) {
            //TODO: Add debounce/delay logic -- this fires too frequently!
            jQuery.post(ScriptEditor.script.path, {
                "jcr:primaryType": "nt:unstructured",
                "script-content": ScriptEditor.getScriptXML()
            });
        }
    },
    getScriptXML: function() {
        return Blockly.Xml.domToText(Blockly.Xml.workspaceToDom(ScriptEditor.workspace));
    }
    
};

jQuery('#blocklyArea').ready(function() {
    ScriptEditor.initEditor();
});
