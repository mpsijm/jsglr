package org.spoofax.jsglr2.inputstack;

import org.spoofax.jsglr2.inputstack.incremental.AbstractInputStack;

public class InputStack extends AbstractInputStack {
    int currentChar; // Current ASCII char in range [0, 256]

    public InputStack(String inputString, String fileName) {
        super(inputString, fileName);
    }

    @Override
    public void resetOffset(int offset) {
        currentOffset = offset;
        currentChar = getChar(offset);
    }

    @Override public boolean hasNext() {
        return currentOffset <= inputLength;
    }

    @Override public void next() {
        currentOffset++;
        currentChar = getChar(currentOffset);
    }

    @Override public int getChar() {
        return currentChar;
    }

}
