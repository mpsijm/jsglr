package org.spoofax.jsglr2.recovery;

import java.util.List;

import org.spoofax.jsglr2.stack.IStackNode;

public interface IBacktrackChoicePoint<StackNode extends IStackNode> {

    int offset();

    List<StackNode> activeStacks();

}
