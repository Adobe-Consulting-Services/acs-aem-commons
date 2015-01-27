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
     * @param longFormText The HTML text to split
     * @return A string array of HTML paragraphs including the surrounding <p>...</p>
     */
    String[] getTextParagraphs(String longFormText);

    /**
     * Housekeeping for the Long Form Article text responsible for merging dangling par resource up when
     * the number of <p> decreases.
     *
     * @param articleTextResource
     * @param numParagraphCount
     * @throws javax.jcr.RepositoryException
     */
    void mergeArticleParagraphSystems(Resource articleTextResource, int numParagraphCount) throws RepositoryException;
}
