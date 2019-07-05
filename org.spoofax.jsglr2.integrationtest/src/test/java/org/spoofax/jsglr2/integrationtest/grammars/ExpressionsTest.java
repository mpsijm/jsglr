package org.spoofax.jsglr2.integrationtest.grammars;

import java.util.Arrays;

import org.junit.Test;
import org.spoofax.jsglr.client.imploder.IToken;
import org.spoofax.jsglr2.integrationtest.BaseTestWithSdf3ParseTables;
import org.spoofax.jsglr2.integrationtest.TokenDescriptor;
import org.spoofax.terms.ParseError;

public class ExpressionsTest extends BaseTestWithSdf3ParseTables {

    public ExpressionsTest() {
        super("expressions.sdf3");
    }

    @Test public void number() throws ParseError {
        testTokens("1", Arrays.asList(
        //@formatter:off
            new TokenDescriptor("1", IToken.TK_NUMBER, 0, 1, 1)
        //@formatter:on
        ));
    }

    @Test public void identifier() throws ParseError {
        testTokens("x", Arrays.asList(
        //@formatter:off
            new TokenDescriptor("x", IToken.TK_IDENTIFIER, 0, 1, 1)
        //@formatter:on
        ));
    }

    @Test public void operator() throws ParseError {
        testTokens("x+x", Arrays.asList(
        //@formatter:off
            new TokenDescriptor("x", IToken.TK_IDENTIFIER, 0, 1, 1),
            new TokenDescriptor("+", IToken.TK_OPERATOR,   1, 1, 2),
            new TokenDescriptor("x", IToken.TK_IDENTIFIER, 2, 1, 3)
        //@formatter:on
        ));
    }

    @Test public void operatorWithLayout() throws ParseError {
        testTokens("x + x", Arrays.asList(
        //@formatter:off
            new TokenDescriptor("x", IToken.TK_IDENTIFIER, 0, 1, 1),
            new TokenDescriptor(" ", IToken.TK_LAYOUT,     1, 1, 2),
            new TokenDescriptor("+", IToken.TK_OPERATOR,   2, 1, 3),
            new TokenDescriptor(" ", IToken.TK_LAYOUT,     3, 1, 4),
            new TokenDescriptor("x", IToken.TK_IDENTIFIER, 4, 1, 5)
        //@formatter:on
        ));
    }

    @Test public void operatorWithLayout2() throws ParseError {
        testTokens(" x + x ", Arrays.asList(
        //@formatter:off
            new TokenDescriptor(" ", IToken.TK_LAYOUT,     0, 1, 1),
            new TokenDescriptor("x", IToken.TK_IDENTIFIER, 1, 1, 2),
            new TokenDescriptor(" ", IToken.TK_LAYOUT,     2, 1, 3),
            new TokenDescriptor("+", IToken.TK_OPERATOR,   3, 1, 4),
            new TokenDescriptor(" ", IToken.TK_LAYOUT,     4, 1, 5),
            new TokenDescriptor("x", IToken.TK_IDENTIFIER, 5, 1, 6),
            new TokenDescriptor(" ", IToken.TK_LAYOUT,     6, 1, 7)
        //@formatter:on
        ));
    }

    @Test public void multipleLines() throws ParseError {
        testTokens("x\nx", Arrays.asList(
        //@formatter:off
            new TokenDescriptor("x",  IToken.TK_IDENTIFIER, 0, 1, 1),
            new TokenDescriptor("\n", IToken.TK_LAYOUT,     1, 1, 2),
            new TokenDescriptor("x",  IToken.TK_IDENTIFIER, 2, 2, 1)
        //@formatter:on
        ));
    }

    @Test public void multipleLinesNewlineEnd() throws ParseError {
        testTokens("x\nx\n", Arrays.asList(
        //@formatter:off
            new TokenDescriptor("x",  IToken.TK_IDENTIFIER, 0, 1, 1),
            new TokenDescriptor("\n", IToken.TK_LAYOUT,     1, 1, 2),
            new TokenDescriptor("x",  IToken.TK_IDENTIFIER, 2, 2, 1),
            new TokenDescriptor("\n", IToken.TK_LAYOUT,     3, 2, 2)
        //@formatter:on
        ));
    }

    @Test public void changingIdentifier() throws ParseError {
        testIncrementalSuccessByExpansions(new String[] { "abc + def", "agc + def" },
            new String[] { "[Add(Var(\"abc\"),Var(\"def\"))]", "[Add(Var(\"agc\"),Var(\"def\"))]" });
    }

    @Test public void reusingSubtreesNoLayout() {
        String[] inputStrings = { "xx+x x+x", "xy+x x+x" };
        testIncrementalSuccessByExpansions(inputStrings,
            new String[] { "[Add(Var(\"xx\"),Var(\"x\")),Add(Var(\"x\"),Var(\"x\"))]",
                "[Add(Var(\"xy\"),Var(\"x\")),Add(Var(\"x\"),Var(\"x\"))]" });

        testSubtreeReuse(inputStrings[0], inputStrings[1], new int[][] {
            // x+x cannot be reused, as it was parsed in multiple states
            { 0 }, // The empty layout before the first x is reused
            { 1, 0, 0, 0, 3 }, // The layout before the second x is reused
            { 1, 0, 0, 0, 4 }, // The second x is reused
            { 1, 0, 2, 3 }, // The empty layout before the last x is reused
            { 1, 0, 2, 4 }, // The last x is reused
        });
    }

    @Test public void reusingSubtreesWithLayout() {
        testIncrementalSuccessByExpansions(new String[] { "xx + x x + x", "xy + x x + x" },
            new String[] { "[Add(Var(\"xx\"),Var(\"x\")),Add(Var(\"x\"),Var(\"x\"))]",
                "[Add(Var(\"xy\"),Var(\"x\")),Add(Var(\"x\"),Var(\"x\"))]" });
    }

    @Test public void incrementalAmbiguity() {
        testIncrementalSuccessByExpansions(new String[] { "x+x", "x+x+x", "x+x" },
            new String[] { "[Add(Var(\"x\"),Var(\"x\"))]",
                "[amb([Add(Var(\"x\"),Add(Var(\"x\"),Var(\"x\"))),Add(Add(Var(\"x\"),Var(\"x\")),Var(\"x\"))])]",
                "[Add(Var(\"x\"),Var(\"x\"))]" });
    }

}
