package org.spoofax.jsglr2.inputstack;

import org.metaborg.parsetable.query.IActionQuery;

public interface IInputStack extends IActionQuery {
    String inputString();

    String fileName();

    boolean hasNext();

    void next();

    int getChar();

    int getChar(int offset);

    int offset();

    // This is an optimization over cloning the entire input stack
    // because the parse nodes in the the part of the input being recovered need to be broken down anyway.
    void resetOffset(int offset);
}
