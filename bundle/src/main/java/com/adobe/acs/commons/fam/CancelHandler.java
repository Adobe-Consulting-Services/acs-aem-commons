/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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
package com.adobe.acs.commons.fam;

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
            t.interrupt();
        } else {
            activeWork.add(t);
        }
    }
    
    public void untrackActiveWork(Thread t) {
        activeWork.remove(t);
    }
}
