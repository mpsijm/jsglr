package org.spoofax.jsglr2.testset;

import org.spoofax.jsglr2.parseforest.IParseForest;

public class ParseForestInput {

    public final String filename;
    public final String content;
    public final IParseForest parseForest;

    public ParseForestInput(String filename, String content, IParseForest parseForest) {
        this.filename = filename;
        this.content = content;
        this.parseForest = parseForest;
    }

}
