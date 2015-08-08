package com.adobe.acs.commons.version;

public interface EvolutionEntry {

    int getDepth();

    String getName();

    String getStatus();

    EvolutionEntryType getType();

    String getUniqueName();

    String getValueString();

    String getValueStringShort();

    boolean isAdded();

    boolean isChanged();

    boolean isCurrent();

    boolean isResource();

    boolean isWillBeRemoved();

    /**
     * The available entry types.
     */
    public enum EvolutionEntryType {
        RESOURCE, PROPERTY
    }

}
