package org.spoofax.jsglr2.integrationtest.incremental;

import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.spoofax.jsglr2.integrationtest.BaseTestWithSdf3ParseTables;
import org.spoofax.jsglr2.integrationtest.ParseNodeDescriptor;

public class SubtreeChangeTest extends BaseTestWithSdf3ParseTables {

    public SubtreeChangeTest() {
        super("subtree-change.sdf3");
    }

    @TestFactory public Stream<DynamicTest> testSubtreeChange() {
        return testIncrementalSuccessByExpansions(new String[] { "<x+x+x>L", "<x+x+x>R", "<x+x+x>L" },
            new String[] { "Left(AddL(AddL(TermL,TermL),TermL))", "Right(AddR(TermR,AddR(TermR,TermR)))",
                "Left(AddL(AddL(TermL,TermL),TermL))" });
    }

    @TestFactory public Stream<DynamicTest> testSubtreeReuse() {
        return testSubtreeReuse("<x+x+x>L", "<x+x+x>R", new ParseNodeDescriptor(0, 1, "\"<\"", null),
            new ParseNodeDescriptor(1, 1, "\"x\"", null)
        // , new int[][] {
        // None of the subtrees of x+x+x are reused because the parser is in multiple states while parsing that part
        // { 0 }, // empty layout before "<"
        // { 1, 0 }, // "<"
        // { 1, 1 }, // empty layout between "<" and 'ExpL' or 'ExpR'
        // { 1, 4, 0 }, // ">"
        // }
        );
    }
}
