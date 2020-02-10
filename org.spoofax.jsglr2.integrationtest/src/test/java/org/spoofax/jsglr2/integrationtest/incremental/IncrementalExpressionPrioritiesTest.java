package org.spoofax.jsglr2.integrationtest.incremental;

import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.spoofax.jsglr2.integrationtest.BaseTestWithSdf3ParseTables;
import org.spoofax.jsglr2.integrationtest.ParseNodeDescriptor;

public class IncrementalExpressionPrioritiesTest extends BaseTestWithSdf3ParseTables {

    public IncrementalExpressionPrioritiesTest() {
        super("exp-priorities.sdf3");
    }

    @TestFactory public Stream<DynamicTest> changingPriorities() {
        //@formatter:off
        return testIncrementalSuccessByExpansions(
            new String[] {
                "x+x+x",
                "x*x+x",
                "x*x*x",
                "x+x*x",
                "x+x+x"
            },
            new String[] {
                "Add(Add(Term(),Term()),Term())",
                "Add(Mult(Term(),Term()),Term())",
                "Mult(Mult(Term(),Term()),Term())",
                "Add(Term(),Mult(Term(),Term()))",
                "Add(Add(Term(),Term()),Term())"
            }
        );
        //@formatter:on
    }

    @TestFactory public Stream<DynamicTest> largerPrioritiesTest() {
        //@formatter:off
        return testIncrementalSuccessByExpansions(
            new String[] {
                "x*x+x*x+x*x",
                "x*x+x*x+x+x",
                "x*x*x*x+x+x"
            },
            new String[] {
                "Add(Add(Mult(Term,Term),Mult(Term,Term)),Mult(Term,Term))",
                "Add(Add(Add(Mult(Term,Term),Mult(Term,Term)),Term),Term)",
                "Add(Add(Mult(Mult(Mult(Term,Term),Term),Term),Term),Term)"
            }
        );
        //@formatter:on
    }

    @TestFactory public Stream<DynamicTest> nothingChangedTest() {
        //@formatter:off
        return testIncrementalSuccessByExpansions(
            new String[] {
                "x*x+x*x+x*x",
                "x*x+x*x+x*x"
            },
            new String[] {
                "Add(Add(Mult(Term,Term),Mult(Term,Term)),Mult(Term,Term))",
                "Add(Add(Mult(Term,Term),Mult(Term,Term)),Mult(Term,Term))"
            }
        );
        //@formatter:on
    }

    @TestFactory public Stream<DynamicTest> reusingSubtreesNoLayout() {
        String[] inputStrings = { "x+x+x", "x+x*x" };
        return Stream.concat(
            testIncrementalSuccessByExpansions(inputStrings,
                new String[] { "Add(Add(Term,Term),Term)", "Add(Term,Mult(Term,Term))" }),
            testSubtreeReuse(inputStrings[0], inputStrings[1], new ParseNodeDescriptor(0, 1, "Exp", "Term"),
                new ParseNodeDescriptor(1, 1, "\"+\"", null)
            // , new int[][] {
            // x+x cannot be reused, as it is broken down
            // { 0 }, // The empty layout before the first x is reused
            // { 2 }, // The empty layout after the last x is reused
            // }
            ));
    }

}
