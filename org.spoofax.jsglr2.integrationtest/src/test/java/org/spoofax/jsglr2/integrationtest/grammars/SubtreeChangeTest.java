package org.spoofax.jsglr2.integrationtest.grammars;

import org.junit.Test;
import org.spoofax.jsglr2.integrationtest.BaseTestWithSdf3ParseTables;

public class SubtreeChangeTest extends BaseTestWithSdf3ParseTables {

    public SubtreeChangeTest() {
        super("subtree-change.sdf3");
    }

    @Test public void testSubtreeChange() {
        testIncrementalSuccessByExpansions(new String[] { "<x+x+x>L", "<x+x+x>R", "<x+x+x>L" },
            new String[] { "Left(AddL(AddL(TermL,TermL),TermL))", "Right(AddR(TermR,AddR(TermR,TermR)))",
                "Left(AddL(AddL(TermL,TermL),TermL))" });
    }

    @Test public void testSubtreeReuse() {
        testSubtreeReuse("<x+x+x>L", "<x+x+x>R", new int[][] {
            // None of the subtrees of x+x+x are reused because the parser is in multiple states while parsing that part
            { 0 }, // empty layout before "<"
            { 1, 0 }, // "<"
            { 1, 1 }, // empty layout between "<" and 'ExpL' or 'ExpR'
            { 1, 4, 0 }, // ">"
        });
    }
}
