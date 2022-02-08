/*
 * #%L
 * ACS AEM Commons Bundle
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
 */
package com.adobe.acs.commons.fam.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * Encapsulates details about a pooled resource resolver
 */
public class ReusableResolver {
    
    private final ResourceResolver resolver;
    private int changeCount;
    private final int saveInterval;
    private final List<String> pendingItems;
    private String currentItem;

    public ReusableResolver(ResourceResolver res, int save) {
        resolver = res;
        changeCount = 0;
        saveInterval = save;
        pendingItems = new ArrayList<>();
    }

    public void setCurrentItem(String current) {
        currentItem = current;
    }

    public String getCurrentItem() {
        return currentItem;
    }

    public void free() throws PersistenceException {
        if (getResolver().isLive()) {
            if (getResolver().hasChanges()) {
                setChangeCount(getChangeCount() + 1);
                pendingItems.add(getCurrentItem());
            }
            if (getChangeCount() >= getSaveInterval()) {
                commit();
            }
        }
    }

    public void commit() throws PersistenceException {
        setChangeCount(0);
        if (getResolver().isLive() && getResolver().hasChanges()) {
            try {
                getResolver().commit();
            } catch (PersistenceException e) {
                getResolver().revert();
                getResolver().refresh();
                throw e;
            } finally {
                pendingItems.clear();
            }
        }
    }

    public int getChangeCount() {
        return changeCount;
    }

    /**
     * @return the resolver
     */
    public ResourceResolver getResolver() {
        return resolver;
    }

    private void setChangeCount(int changeCount) {
        this.changeCount = changeCount;
    }

    public int getSaveInterval() {
        return saveInterval;
    }

    public List<String> getPendingItems() {
        return Collections.unmodifiableList(pendingItems);
    }
    
}
