package org.spoofax.jsglr2.inputstack.incremental;

import static org.spoofax.jsglr2.incremental.parseforest.IncrementalCharacterNode.EOF_NODE;

import org.spoofax.jsglr2.incremental.parseforest.IncrementalCharacterNode;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseForest;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseNode;

public class LinkedIncrementalInputStack extends AbstractInputStack implements IIncrementalInputStack {
    private StackTuple head;

    /**
     * @param inputString
     *            should be equal to the yield of the root.
     */
    public LinkedIncrementalInputStack(IncrementalParseForest root, String inputString, String fileName) {
        super(inputString, fileName);
        this.head = new StackTuple(root, new StackTuple(EOF_NODE));
    }

    LinkedIncrementalInputStack(IncrementalParseForest root) {
        this(root, root.getYield(), "");
    }

    @Override public IncrementalParseForest getNode() {
        return head == null ? null : head.node;
    }

    @Override public void resetOffset(int offset) {
        for(int i = currentOffset - 1; i >= offset; i--) {
            head = new StackTuple(new IncrementalCharacterNode(inputString.charAt(i)), head);
        }
        currentOffset = offset;
    }

    @Override public void leftBreakdown() {
        if(head == null)
            return;
        IncrementalParseForest current = head.node;
        if(current.isTerminal()) {
            return;
        }
        head = head.next; // always pop last lookahead, whether it has children or not
        IncrementalParseForest[] children = ((IncrementalParseNode) current).getFirstDerivation().parseForests();
        // Push all children to stack in reverse order
        for(int i = children.length - 1; i >= 0; i--) {
            head = new StackTuple(children[i], head);
        }
    }

    @Override public void next() {
        currentOffset += head.node.width();
        head = head.next;
    }

    private static class StackTuple {
        final IncrementalParseForest node;
        final StackTuple next;

        StackTuple(IncrementalParseForest node, StackTuple next) {
            this.node = node;
            this.next = next;
        }

        StackTuple(IncrementalParseForest node) {
            this(node, null);
        }
    }
}
