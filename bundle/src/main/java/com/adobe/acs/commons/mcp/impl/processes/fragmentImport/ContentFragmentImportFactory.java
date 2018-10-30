package com.adobe.acs.commons.mcp.impl.processes.fragmentImport;

import com.adobe.acs.commons.mcp.ProcessDefinitionFactory;
import org.osgi.service.component.annotations.Component;

/**
 * OSGi factory for Content Import utility
 */
@Component(service = ProcessDefinitionFactory.class)
public class ContentFragmentImportFactory extends ProcessDefinitionFactory<ContentFragmentImport> {

    @Override
    public String getName() {
        return "Content Fragment Import";
    }

    @Override
    protected ContentFragmentImport createProcessDefinitionInstance() {
        return new ContentFragmentImport();
    }
}
