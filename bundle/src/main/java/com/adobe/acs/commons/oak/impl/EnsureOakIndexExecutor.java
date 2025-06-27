package com.adobe.acs.commons.oak.impl;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.openmbean.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Component(service = EnsureOakIndexExecutor.class)
public class EnsureOakIndexExecutor {

    private static final Logger log = LoggerFactory.getLogger(EnsureOakIndexExecutor.class);

    @Reference(
            cardinality = ReferenceCardinality.MULTIPLE,
            policyOption = ReferencePolicyOption.GREEDY,
            policy = ReferencePolicy.DYNAMIC,
            fieldOption = FieldOption.UPDATE
    )
    // Thread-safe ArrayList to track EnsureIndex service registrations
    private CopyOnWriteArrayList<AppliableEnsureOakIndex> ensureIndexes = new CopyOnWriteArrayList<>();

    public final int ensureAll(boolean force) {
        log.info("Applying all un-applied ensure index definitions");

        int count = 0;
        for (AppliableEnsureOakIndex index : this.ensureIndexes) {
            if (!index.isApplied() || force) {
                index.apply(force);
                count++;
                log.debug("Started applying index definition on [ {} ]", index);
            } else {
                log.debug("Skipping... [ {} ] is already applied.", index);
            }
        }

        return count;
    }

    public final int ensure(final boolean force,
                            final String ensureDefinitionPath) {
        int count = 0;
        for (AppliableEnsureOakIndex index : this.ensureIndexes) {
            if ((!index.isApplied() || force)
                    && StringUtils.equals(ensureDefinitionPath, index.getEnsureDefinitionsPath())) {
                index.apply(force);
                count++;
                log.debug("Started async job applying index definition for {}", index);
            } else {
                log.debug("Skipping... [ {} ] is already applied.", index);
            }
        }
        return count;
    }


    protected final void bindAppliableEnsureOakIndex(AppliableEnsureOakIndex index) {
        if (index != null && !this.ensureIndexes.contains(index)) {
            this.ensureIndexes.add(index);
        }
    }

    protected final void unbindAppliableEnsureOakIndex(AppliableEnsureOakIndex index) {
        if (index != null && this.ensureIndexes.contains(index)) {
            this.ensureIndexes.remove(index);
        }
    }


    /**
     * Method for displaying Ensure Oak Index state in in the MBean
     *
     * @return the Ensure Oak Index data in a Tabular Format for the MBean
     * @throws OpenDataException
     */
    @SuppressWarnings("squid:S1192")
    public final TabularData getEnsureOakIndexes() throws OpenDataException {

        final CompositeType configType = new CompositeType(
                "Ensure Oak Index Configurations",
                "Ensure Oak Index Configurations",
                new String[]{"Ensure Definitions Path", "Oak Indexes Path", "Applied", "Immediate"},
                new String[]{"Ensure Definitions Path", "Oak Indexes Path", "Applied", "Immediate"},
                new OpenType[]{SimpleType.STRING, SimpleType.STRING, SimpleType.BOOLEAN, SimpleType.BOOLEAN});

        final TabularDataSupport tabularData = new TabularDataSupport(new TabularType(
                "Ensure Oak Index Configuration",
                "Ensure Oak Index Configuration",
                configType,
                new String[]{"Ensure Definitions Path", "Oak Indexes Path"}));


        for (final AppliableEnsureOakIndex index : this.ensureIndexes) {
            final Map<String, Object> data = new HashMap<String, Object>();

            data.put("Ensure Definitions Path", index.getEnsureDefinitionsPath());
            data.put("Oak Indexes Path", index.getOakIndexesPath());
            data.put("Applied", index.isApplied());
            data.put("Immediate", index.isImmediate());

            tabularData.put(new CompositeDataSupport(configType, data));
        }

        return tabularData;
    }

}
