package org.spoofax.jsglr2.inputstack.incremental;

import static org.metaborg.parsetable.characterclasses.ICharacterClass.EOF_INT;
import static org.metaborg.parsetable.characterclasses.ICharacterClass.MAX_CHAR;

import org.spoofax.jsglr2.inputstack.IInputStack;

public abstract class AbstractInputStack implements IInputStack {
    protected final String inputString;
    protected final int inputLength;
    protected int currentOffset = 0;

    public AbstractInputStack(String inputString) {
        this.inputString = inputString;
        this.inputLength = inputString.length();
    }

    @Override public abstract IInputStack clone();

    @Override public String inputString() {
        return inputString;
    }

    @Override public int offset() {
        return currentOffset;
    }

    @Override public int length() {
        return inputLength;
    }

    @Override public int actionQueryCharacter() {
        if(currentOffset < inputLength)
            return inputString.charAt(currentOffset);
        if(currentOffset == inputLength)
            return EOF_INT;
        else
            return -1;
    }

    @Override public String actionQueryLookahead(int length) {
        return inputString.substring(currentOffset + 1, Math.min(currentOffset + 1 + length, inputLength));
    }

    @Override public int getChar(int offset) {
        if(offset < inputLength) {
            char c = inputString.charAt(offset);

            if(c > MAX_CHAR)
                throw new IllegalStateException("Character " + c + " not supported");

            return c;
        } else
            return EOF_INT;
    }
}
