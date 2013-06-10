package com.adobe.acs.commons.util;

import com.day.cq.jcrclustersupport.ClusterAware;

/**
 * Abstact base class for scheduled job to be run only on the cluster master.
 */
public abstract class RunnableOnMaster implements ClusterAware, Runnable {
    
    private boolean isMaster;
    
    protected abstract void runOnMaster();
    
    @Override
    public void bindRepository(String repositoryId, String clusterId, boolean isMaster) {
        this.isMaster = isMaster;
    }
    
    @Override
    public void unbindRepository() {
        isMaster = false;
    }

    @Override
    public final void run() {
        if (isMaster) {
            runOnMaster();
        }
    }

}
