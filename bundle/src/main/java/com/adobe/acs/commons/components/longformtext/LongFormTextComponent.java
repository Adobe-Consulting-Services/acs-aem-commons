package com.adobe.acs.commons.components.longformtext;

import org.apache.sling.api.resource.Resource;

import javax.jcr.RepositoryException;


public interface LongFormTextComponent {
    String LONG_FORM_TEXT_PAR = "long-form-text-par-";

    /**
     * Splits long form text into a series of "chunks" delimited &lt;/p>.
     *
     * Note: this method does not support intelligence for managing nested <p>'s in <p>'s
     *
     * @param text The HTML text to split
     * @return A string array of HTML paragraphs including the surrounding <p>...</p>
     */
    String[] getTextParagraphs(String text);

    /**
     * Housekeeping for the Long Form Text responsible for merging dangling par resource up when
     * the number of <p> decreases.
     *
     * @param resource the long form text resource
     * @param numParagraphCount the number of text paragraphs
     * @throws javax.jcr.RepositoryException
     */
    void mergeParagraphSystems(Resource resource, int numParagraphCount) throws RepositoryException;


    /**
     * Determines if the specified long form text parsys has content.
     *
     * @param resource the long form text resource
     * @param index the index of the long-form-text parsys to inspect
     * @return true to include the parsys
     */
    boolean hasContents(Resource resource, int index);
}
