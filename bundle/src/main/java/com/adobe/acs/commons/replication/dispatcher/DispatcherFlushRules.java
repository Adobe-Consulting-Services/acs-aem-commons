package com.adobe.acs.commons.replication.dispatcher;

import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.ReplicationOptions;

public interface DispatcherFlushRules {

    void preprocess(final ReplicationAction replicationAction,
                    final ReplicationOptions replicationOptions) throws ReplicationException;
}
