package org.spoofax.jsglr2.inputstack.incremental;

import static org.spoofax.jsglr2.incremental.parseforest.IncrementalCharacterNode.EOF_NODE;

import java.util.Stack;

import org.spoofax.jsglr2.incremental.parseforest.IncrementalCharacterNode;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalDerivation;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseForest;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseNode;

public class LazyIncrementalInputStack extends AbstractInputStack implements IIncrementalInputStack {
    /**
     * The stack contains the parent and child index of the node that has been returned last time. When the stack is
     * initialized, a mock root is created and pushed to the stack.
     */
    private final Stack<StackTuple> stack = new Stack<>();
    private IncrementalParseForest last;

    /**
     * @param inputString
     *            should be equal to the yield of the root.
     */
    public LazyIncrementalInputStack(IncrementalParseForest root, String inputString, String fileName) {
        super(inputString, fileName);
        IncrementalParseNode ultraRoot = new IncrementalParseNode(root, IncrementalCharacterNode.EOF_NODE);
        stack.push(new StackTuple(ultraRoot, 0));

        this.last = root;
    }

    LazyIncrementalInputStack(IncrementalParseForest root) {
        this(root, root.getYield(), "");
    }

    @Override public void resetOffset(int offset) {
        IncrementalParseForest[] children = new IncrementalParseForest[currentOffset - offset];
        for(int i = offset; i < currentOffset; i++) {
            children[i - offset] = new IncrementalCharacterNode(inputString.charAt(i));
        }
        stack.push(new StackTuple(new IncrementalParseNode(children), 0));
        currentOffset = offset;
        last = children[0];
    }

    @Override public IncrementalParseForest getNode() {
        return last;
    }

    @Override public void leftBreakdown() {
        if(stack.isEmpty())
            last = null;
        if(last == null || last.isTerminal())
            return;
        IncrementalParseForest[] children = ((IncrementalParseNode) last).getFirstDerivation().parseForests();
        if(children.length > 0) {
            stack.push(new StackTuple(((IncrementalParseNode) last), 0));
            last = children[0];
        } else
            next();
    }

    @Override public void next() {
        currentOffset += last.width();
        if(stack.isEmpty())
            last = null;
        StackTuple res = stack.pop();
        while(rightSibling(res) == null)
            if(stack.isEmpty()) {
                last = null;
                return;
            } else
                res = stack.pop();
        stack.push(new StackTuple(res.parseForest, res.childIndex + 1));
        last = rightSibling(res);
    }

    private IncrementalParseForest rightSibling(StackTuple res) {
        if(res == null)
            return IncrementalCharacterNode.EOF_NODE;
        else {
            IncrementalDerivation parent = res.parseForest.getFirstDerivation();
            if(res.childIndex + 1 == parent.parseForests().length)
                return null;
            else
                return parent.parseForests()[res.childIndex + 1];
        }
    }

    private static final class StackTuple {
        private final IncrementalParseNode parseForest;
        private final int childIndex;

        StackTuple(IncrementalParseNode parseForest, int childIndex) {
            this.parseForest = parseForest;
            this.childIndex = childIndex;
        }
    }
}
