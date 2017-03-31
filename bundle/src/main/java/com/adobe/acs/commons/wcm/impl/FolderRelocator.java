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
package com.adobe.acs.commons.wcm.impl;

import com.adobe.acs.commons.fam.Failure;
import com.adobe.acs.commons.fam.ActionManager;
import com.adobe.acs.commons.fam.ActionManagerFactory;
import java.util.List;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * This utility takes an alternate approach to moving folders using a four-step process.
 * Step 1: Evaluate the requirements, check for possible authorization issues.
 * Step 2: Prepare destination folder structure
 * Step 3: Relocate the contents of the folders
 * Step 4: Remove the old folder structures
 */
public class FolderRelocator {
    private final String sourcePath;
    private final String destinationPath;
    private final String processName;
    private ActionManager step1;
    private ActionManager step2;
    private ActionManager step3;
    private ActionManager step4;
    private final SimpleFilteringItemVisitor folderVisitor;
    
    public FolderRelocator(String sourcePath, String destinationPath, String processName) {
        this.sourcePath = sourcePath;
        this.destinationPath = destinationPath;
        this.processName = processName;
        
        folderVisitor = new SimpleFilteringItemVisitor();
        folderVisitor.setBreadthFirst(true);
        folderVisitor.searchForMatchingNodes(this::isFolderOrChild);
    }
    
    public boolean isFolderOrChild(Node n) {
        //TODO: Return true if node is nt:folder, sling:folder, sling:orderedfolder
        return true;
    }
    
    public void startWork(ActionManagerFactory amf, ResourceResolver res) throws LoginException, RepositoryException {
        validateInputs();
        
        step1 = amf.createTaskManager(processName + "- Step 1", res, 1);
        step2 = amf.createTaskManager(processName + "- Step 2", res, 1);
        step3 = amf.createTaskManager(processName + "- Step 3", res, 1);
        step4 = amf.createTaskManager(processName + "- Step 4", res, 1);
                
        step1.onFailure(this::abortStep1);
        step1.onSuccess(this::startStep2);

        step2.onFailure(this::abortStep2);
        step2.onSuccess(this::startStep3);

        step3.onFailure(this::recordError);
        step3.onSuccess(this::startStep4);

        step4.onFailure(this::recordError);
        step4.onFinish(this::success);
        
        startStep1();
    }
    
    private void validateInputs() throws RepositoryException {
        
    }

    private void startStep1() {
        folderVisitor.onEnterNode((node, level) -> step1.deferredWithResolver(rr -> checkNodeAcls(rr, node)));
        step1.deferredWithResolver(rr -> {
            Node source = rr.getResource(sourcePath).adaptTo(Node.class);
            source.accept(folderVisitor);
        });
    }

    private void checkNodeAcls(ResourceResolver res, Node node) throws RepositoryException {
        
    }

    private void abortStep1(List<Failure> errors, ResourceResolver res) {
        
        recordError(errors, res);
    }
    
    private void startStep2(ResourceResolver res) {
        
    }
    
    private void abortStep2(List<Failure> errors, ResourceResolver res) {
        
        recordError(errors, res);
    }
    private void startStep3(ResourceResolver res) {
        
    }
    private void startStep4(ResourceResolver res) {
        
    }
    
    private void recordError(List<Failure> errors, ResourceResolver res) {
        
    }

    private void success() {
        
    }
    
}