package org.spoofax.jsglr2.inputstack.incremental;

import static org.spoofax.jsglr2.incremental.parseforest.IncrementalCharacterNode.EOF_NODE;

import java.util.Stack;

import org.spoofax.jsglr2.incremental.parseforest.IncrementalCharacterNode;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseForest;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseNode;

public class EagerIncrementalInputStack extends AbstractInputStack implements IIncrementalInputStack {
    /**
     * The stack contains all subtrees that are yet to be popped. The top of the stack also contains the subtree that
     * has been returned last time. The stack initially only contains EOF and the root.
     */
    protected final Stack<IncrementalParseForest> stack = new Stack<>();

    /**
     * @param inputString
     *            should be equal to the yield of the root.
     */
    public EagerIncrementalInputStack(IncrementalParseForest root, String inputString, String fileName) {
        super(inputString, fileName);
        stack.push(EOF_NODE);
        stack.push(root);
    }

    EagerIncrementalInputStack(IncrementalParseForest root) {
        this(root, root.getYield(), "");
    }

    @Override public void resetOffset(int offset) {
        for(int i = currentOffset - 1; i >= offset; i--) {
            stack.push(new IncrementalCharacterNode(inputString.charAt(i)));
        }
        currentOffset = offset;
    }

    @Override public void leftBreakdown() {
        if(stack.isEmpty())
            return;
        IncrementalParseForest current = stack.peek();
        if(current.isTerminal()) {
            return;
        }
        stack.pop(); // always pop last lookahead, whether it has children or not
        IncrementalParseForest[] children = ((IncrementalParseNode) current).getFirstDerivation().parseForests();
        // Push all children to stack in reverse order
        for(int i = children.length - 1; i >= 0; i--) {
            stack.push(children[i]);
        }
    }

    @Override public void next() {
        currentOffset += stack.pop().width();
    }

    @Override public IncrementalParseForest getNode() {
        return stack.empty() ? null : stack.peek();
    }
}
