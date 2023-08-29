package com.adobe.acs.commons.mcp.impl.processes;

import com.adobe.acs.commons.mcp.ProcessDefinitionFactory;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;

@Component
@Service(ProcessDefinitionFactory.class)
public class BulkPageTaggerFactory extends ProcessDefinitionFactory<BulkPageTagger> {

    @Override
    public String getName() {
        return BulkPageTagger.NAME;
    }

    @Override
    protected BulkPageTagger createProcessDefinitionInstance() {
        return new BulkPageTagger();
    }
}
