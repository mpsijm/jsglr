package org.spoofax.jsglr2.integrationtest.incremental;

import org.junit.Test;
import org.spoofax.jsglr2.integrationtest.BaseTestWithSdf3ParseTables;

public class IncrementalPreferAvoidTest extends BaseTestWithSdf3ParseTables {

    public IncrementalPreferAvoidTest() {
        super("prefer-avoid.sdf3");
    }

    @Test public void testIncrementalPreferAvoid() {
        //@formatter:off
        testIncrementalSuccessByExpansions(
            new String[] { "p x",        "a x",                 "p x" },
            new String[] { "Prefer(X1)", "Avoid(amb([X2,X3]))", "Prefer(X1)" }
        );
        //@formatter:off
    }
}