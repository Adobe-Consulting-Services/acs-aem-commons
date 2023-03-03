/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
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
package com.adobe.acs.commons.fam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Used to optionally cancel future work.
 * Using Futures would also require keeping track of them somewhere else.
 * This alternative allows a single object which can cancel thousands of tasks
 * that are tied to it.
 */
@SuppressWarnings("CQRules:CWE-676") // use appropriate in this case
public class CancelHandler implements Serializable {
    private static final Logger log = LoggerFactory.getLogger(CancelHandler.class);
    private static final long serialVersionUID = 7526472295622776147L;
    
    private final transient Set<Thread> activeWork = ConcurrentHashMap.newKeySet();
    private boolean cancelled = false;
    private boolean force = false;
  
    public void cancel(boolean useForce) {
        cancelled = true;
        force = useForce;
        if (useForce) {
            activeWork.forEach(Thread::interrupt);
        }
        activeWork.clear();
    }
    
    public boolean isCancelled() {
        return cancelled;
    }
    
    public boolean isForcefullyCancelled() {
        return force;
    }
    
    public void trackActiveWork(Thread t) {
        if (cancelled) {
            // Halt UI button has been removed, but just to be safe...
            log.warn("Thread interruption is no longer supported as it can result in repository corruption.");
        } else {
            activeWork.add(t);
        }
    }
    
    public void untrackActiveWork(Thread t) {
        activeWork.remove(t);
    }
}
