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
package com.adobe.acs.commons.fam.actions;

import com.adobe.acs.commons.fam.ActionManager;
import com.adobe.acs.commons.functions.CheckedConsumer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * Manage a queue of actions that are processed and committed in batches.
 * The number of actions processed in a batch is determined by the size of the queue.
 */
public class ActionBatch extends LinkedBlockingQueue<CheckedConsumer<ResourceResolver>> {

    private final ActionManager manager;
    private int retryCount = 5;
    private long retryDelay = 100;

    public ActionBatch(ActionManager manager, int capacity) {
        super(capacity);
        this.manager = manager;
    }

    public void setRetryCount(int count) {
        retryCount = count;
    }

    public void setRetryWait(long delay) {
        retryDelay = delay;
    }

    @Override
    public boolean add(CheckedConsumer<ResourceResolver> c) {
        if (remainingCapacity() == 0) {
            commitBatch();
        }
        super.add(c);
        if (remainingCapacity() == 0) {
            commitBatch();
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends CheckedConsumer<ResourceResolver>> c) {
        c.forEach(this::add);
        return true;
    }

    public void commitBatch() {
        final List<CheckedConsumer<ResourceResolver>> consumers = new ArrayList<>();
        int count = this.drainTo(consumers);
        if (count > 0) {
            manager.deferredWithResolver(
                    Actions.retry(retryCount, retryDelay, (ResourceResolver rr) -> {
                        Logger.getLogger(ActionBatch.class.getName()).log(Level.INFO, "Executing {0} actions", count);
                        for (CheckedConsumer<ResourceResolver> consumer : consumers) {
                            consumer.accept(rr);
                        }
                        Logger.getLogger(ActionBatch.class.getName()).log(Level.INFO, "Commiting {0} actions", count);
                        rr.commit();
                        Logger.getLogger(ActionBatch.class.getName()).log(Level.INFO, "Commit successful");
                    })
            );
        }
    }
}
