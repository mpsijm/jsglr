package org.spoofax.jsglr2.integrationtest;

import static java.util.Collections.sort;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import org.metaborg.parsetable.IParseTable;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr.client.imploder.IToken;
import org.spoofax.jsglr.client.imploder.ImploderAttachment;
import org.spoofax.jsglr2.JSGLR2;
import org.spoofax.jsglr2.JSGLR2Result;
import org.spoofax.jsglr2.JSGLR2Variants;
import org.spoofax.jsglr2.integration.IntegrationVariant;
import org.spoofax.jsglr2.integration.ParseTableVariant;
import org.spoofax.jsglr2.integration.WithParseTable;
import org.spoofax.jsglr2.parseforest.ParseForestRepresentation;
import org.spoofax.jsglr2.parser.IParser;
import org.spoofax.jsglr2.parser.ParseException;
import org.spoofax.jsglr2.parser.Position;
import org.spoofax.jsglr2.parser.result.ParseResult;
import org.spoofax.jsglr2.util.AstUtilities;
import org.spoofax.terms.TermFactory;
import org.spoofax.terms.io.binary.TermReader;

public abstract class BaseTest implements WithParseTable {

    private static TermReader termReader = new TermReader(new TermFactory());
    private static AstUtilities astUtilities = new AstUtilities();

    protected BaseTest() {
    }

    public TermReader getTermReader() {
        return termReader;
    }

    protected IParseTable getParseTableFailOnException(ParseTableVariant variant) {
        try {
            return getParseTable(variant);
        } catch(Exception e) {
            e.printStackTrace();

            fail("Exception during reading parse table: " + e.getMessage());

            return null;
        }
    }

    protected void testParseSuccess(String inputString) {
        for(IntegrationVariant variant : IntegrationVariant.testVariants()) {
            IParseTable parseTable = getParseTableFailOnException(variant.parseTable);
            IParser<?> parser = JSGLR2Variants.getParser(parseTable, variant.parser);

            ParseResult<?> parseResult = parser.parse(inputString);

            assertEquals("Variant '" + variant.name() + "' failed parsing: ", true, parseResult.isSuccess);
        }
    }

    protected void testParseFailure(String inputString) {
        for(IntegrationVariant variant : IntegrationVariant.testVariants()) {
            IParseTable parseTable = getParseTableFailOnException(variant.parseTable);
            IParser<?> parser = JSGLR2Variants.getParser(parseTable, variant.parser);

            ParseResult<?> parseResult = parser.parse(inputString);

            assertEquals("Variant '" + variant.name() + "' should fail: ", false, parseResult.isSuccess);
        }
    }

    protected void testSuccessByAstString(String inputString, String expectedOutputAstString) {
        testSuccess(inputString, expectedOutputAstString, null, false);
    }

    protected void testSuccessByExpansions(String inputString, String expectedOutputAstString) {
        testSuccess(inputString, expectedOutputAstString, null, true);
    }

    protected void testIncrementalSuccessByExpansions(String[] inputStrings, String[] expectedOutputAstStrings) {
        testIncrementalSuccess(inputStrings, expectedOutputAstStrings, null, true);
    }

    protected void testSuccessByAstString(String startSymbol, String inputString, String expectedOutputAstString) {
        testSuccess(inputString, expectedOutputAstString, startSymbol, false);
    }

    protected void testSuccessByExpansions(String startSymbol, String inputString, String expectedOutputAstString) {
        testSuccess(inputString, expectedOutputAstString, startSymbol, true);
    }

    private void testSuccess(String inputString, String expectedOutputAstString, String startSymbol,
        boolean equalityByExpansions) {
        IStrategoTerm previous = null;
        for(IntegrationVariant variant : IntegrationVariant.testVariants()) {
            IParseTable parseTable = getParseTableFailOnException(variant.parseTable);
            IStrategoTerm actualOutputAst = testSuccess(parseTable, variant.jsglr2, startSymbol, inputString);

            if(previous != null)
                assertEqualAST("Variant '" + variant.name() + "' does not have the same AST as the previous variant",
                    previous, actualOutputAst);
            previous = actualOutputAst;

            assertEqualAST("Variant '" + variant.name() + "' has incorrect AST", expectedOutputAstString,
                actualOutputAst, equalityByExpansions);
        }
    }

    protected IStrategoTerm testSuccess(IParseTable parseTable, JSGLR2Variants.Variant variant, String startSymbol,
        String inputString) {
        JSGLR2<IStrategoTerm> jsglr2 = JSGLR2Variants.getJSGLR2(parseTable, variant);

        return testSuccess("Variant '" + variant.name() + "' failed parsing: ",
            "Variant '" + variant.name() + "' failed imploding: ", jsglr2, "", startSymbol, inputString);
    }

    private IStrategoTerm testSuccess(String parseFailMessage, String implodeFailMessage, JSGLR2<IStrategoTerm> jsglr2,
        String filename, String startSymbol, String inputString) {
        try {

            IStrategoTerm result = jsglr2.parseUnsafe(inputString, filename, startSymbol);

            // Fail here if imploding or tokenization failed
            assertNotNull(implodeFailMessage, result);

            return result;

        } catch(ParseException e) {

            // Fail here if parsing failed
            fail(parseFailMessage + e.failureType);

        }
        return null;
    }

    private void testIncrementalSuccess(String[] inputStrings, String[] expectedOutputAstStrings, String startSymbol,
        boolean equalityByExpansions) {
        for(IntegrationVariant variant : IntegrationVariant.testVariants()) {
            if(variant.parser.parseForestRepresentation != ParseForestRepresentation.Incremental)
                continue;

            IParseTable parseTable = getParseTableFailOnException(variant.parseTable);
            JSGLR2<IStrategoTerm> jsglr2 = JSGLR2Variants.getJSGLR2(parseTable, variant.jsglr2);

            IStrategoTerm actualOutputAst;
            String filename = "" + System.nanoTime(); // To ensure the results will be cached
            for(int i = 0; i < expectedOutputAstStrings.length; i++) {
                String inputString = inputStrings[i];
                actualOutputAst = testSuccess("Variant '" + variant.name() + "' failed parsing at update " + i + ": ",
                    "Variant '" + variant.name() + "' failed imploding at update " + i + ": ", jsglr2, filename,
                    startSymbol, inputString);
                assertEqualAST("Variant '" + variant.name() + "' has incorrect AST at update " + i + ": ",
                    expectedOutputAstStrings[i], actualOutputAst, equalityByExpansions);
            }
        }
    }

    protected void assertEqualAST(String message, IStrategoTerm expected, IStrategoTerm actual) {
        assertEqualAST(message, expected, actual, expected, actual);
    }

    private void assertEqualAST(String message, IStrategoTerm expected, IStrategoTerm actual, IStrategoTerm e,
        IStrategoTerm a) {
        IStrategoTerm[] subTermsE = e.getAllSubterms();
        IStrategoTerm[] subTermsA = a.getAllSubterms();
        if(e.getTermType() == IStrategoTerm.APPL && ((IStrategoAppl) e).getConstructor().getName().equals("amb")
            && a.getTermType() == IStrategoTerm.APPL && ((IStrategoAppl) a).getConstructor().getName().equals("amb")) {
            // Check the list term that is the first argument of the amb()
            checkSubTerms(message, expected, actual, e, a, subTermsE, subTermsA);
            // Set the "parent" to this list term
            e = subTermsE[0];
            a = subTermsA[0];
            subTermsE = e.getAllSubterms();
            subTermsA = a.getAllSubterms();
            // Sort the sub terms of the list term
            Arrays.sort(subTermsE, Comparator.comparing(Object::toString));
            Arrays.sort(subTermsA, Comparator.comparing(Object::toString));
        }
        checkSubTerms(message, expected, actual, e, a, subTermsE, subTermsA);


        for(int i = 0; i < subTermsA.length; i++) {
            assertEqualAST(message, expected, actual, subTermsE[i], subTermsA[i]);
        }
    }

    private void checkSubTerms(String message, IStrategoTerm expected, IStrategoTerm actual, IStrategoTerm e,
        IStrategoTerm a, IStrategoTerm[] subTermsE, IStrategoTerm[] subTermsA) {
        if(subTermsA.length != subTermsE.length)
            fail(message + "\nExpected: " + expected + "\n  Actual: " + actual);
        if(!Objects.equals(e.getAnnotations(), a.getAnnotations()))
            fail(message + "\nExpected annotations: " + e.getAnnotations() + "\n  Actual annotations: "
                + a.getAnnotations() + "\n On tree: " + a);
        ImploderAttachment expectedAttachment = e.getAttachment(ImploderAttachment.TYPE);
        ImploderAttachment actualAttachment = a.getAttachment(ImploderAttachment.TYPE);
        if(!equalAttachment(expectedAttachment, actualAttachment)) {
            fail(message + "\nExpected attachment: " + expectedAttachment + " "
                + (expectedAttachment == null ? "null"
                    : printToken(expectedAttachment.getLeftToken()) + " - "
                        + printToken(expectedAttachment.getRightToken()))
                + "\n  Actual attachment: " + actualAttachment + " "
                + (actualAttachment == null ? "null" : printToken(actualAttachment.getLeftToken()) + " - "
                    + printToken(actualAttachment.getRightToken()))
                + "\nOn tree: " + a);
        }
    }

    private String printToken(IToken token) {
        if(token == null)
            return "null";
        return "<" + token.toString() + ";" + token.getKind() + ";o:" + token.getStartOffset() + " l:" + token.getLine()
            + " c:" + token.getColumn() + ";o:" + token.getEndOffset() + " l:" + token.getEndLine() + " c:"
            + token.getEndColumn() + ">";
    }

    private boolean equalAttachment(ImploderAttachment expectedAttachment, ImploderAttachment actualAttachment) {
        if(expectedAttachment == null)
            return actualAttachment == null;
        if(actualAttachment == null)
            return false;
        IToken expectedLeft = expectedAttachment.getLeftToken();
        IToken expectedRight = expectedAttachment.getRightToken();
        IToken actualLeft = actualAttachment.getLeftToken();
        IToken actualRight = actualAttachment.getRightToken();
        return Objects.equals(expectedAttachment.getSort(), actualAttachment.getSort())
            && expectedLeft.getKind() == actualLeft.getKind() && expectedRight.getKind() == actualRight.getKind()
            && expectedLeft.getStartOffset() == actualLeft.getStartOffset()
            && expectedRight.getStartOffset() == actualRight.getStartOffset()
            && expectedLeft.getEndOffset() == actualLeft.getEndOffset()
            && expectedRight.getEndOffset() == actualRight.getEndOffset()
            && expectedLeft.getLine() == actualLeft.getLine() && expectedRight.getLine() == actualRight.getLine()
            && expectedLeft.getEndLine() == actualLeft.getEndLine()
            && expectedRight.getEndLine() == actualRight.getEndLine()
            && expectedLeft.getColumn() == actualLeft.getColumn()
            && expectedRight.getColumn() == actualRight.getColumn()
            && expectedLeft.getEndColumn() == actualLeft.getEndColumn()
            && expectedRight.getEndColumn() == actualRight.getEndColumn();
    }

    private void assertEqualAST(String message, String expectedOutputAstString, IStrategoTerm actualOutputAst,
        boolean equalityByExpansions) {
        if(equalityByExpansions) {
            IStrategoTerm expectedOutputAst = termReader.parseFromString(expectedOutputAstString);

            assertEqualTermExpansions(message, expectedOutputAst, actualOutputAst);
        } else {
            assertEquals(message, expectedOutputAstString, actualOutputAst.toString());
        }
    }

    protected static void assertEqualTermExpansions(IStrategoTerm expected, IStrategoTerm actual) {
        assertEqualTermExpansions(null, expected, actual);
    }

    protected static void assertEqualTermExpansions(String message, IStrategoTerm expected, IStrategoTerm actual) {
        List<String> expectedExpansion = toSortedStringList(astUtilities.expand(expected));
        List<String> actualExpansion = toSortedStringList(astUtilities.expand(actual));

        assertEquals(message, expectedExpansion, actualExpansion);

    }

    private static List<String> toSortedStringList(List<IStrategoTerm> astExpansion) {
        List<String> result = new ArrayList<>(astExpansion.size());

        for(IStrategoTerm ast : astExpansion) {
            result.add(ast.toString());
        }

        sort(result);

        return result;
    }

    protected void testTokens(String inputString, List<TokenDescriptor> expectedTokens) {
        for(IntegrationVariant variant : IntegrationVariant.testVariants()) {
            IParseTable parseTable = getParseTableFailOnException(variant.parseTable);
            JSGLR2<IStrategoTerm> jsglr2 = JSGLR2Variants.getJSGLR2(parseTable, variant.jsglr2);

            JSGLR2Result<?> jsglr2Result = jsglr2.parseResult(inputString, "", null);

            assertTrue("Variant '" + variant.name() + "' failed: ", jsglr2Result.isSuccess);

            List<TokenDescriptor> actualTokens = new ArrayList<>();

            for(IToken token : jsglr2Result.tokens) {
                actualTokens.add(TokenDescriptor.from(inputString, token));
            }

            TokenDescriptor expectedBeginToken = new TokenDescriptor("", IToken.TK_RESERVED, 0, 1, 1);
            TokenDescriptor actualBeginToken = actualTokens.get(0);

            assertEquals("Start token incorrect:", expectedBeginToken, actualBeginToken);

            Position endPosition = Position.atEnd(inputString);

            int endLine = endPosition.line;
            int endColumn = endPosition.column;

            TokenDescriptor expectedEndToken =
                new TokenDescriptor("", IToken.TK_EOF, inputString.length(), endLine, endColumn - 1);
            TokenDescriptor actualEndToken = actualTokens.get(actualTokens.size() - 1);

            List<TokenDescriptor> actualTokenWithoutStartAndEnd = actualTokens.subList(1, actualTokens.size() - 1);

            assertThat(actualTokenWithoutStartAndEnd, is(expectedTokens));

            assertEquals("End token incorrect:", expectedEndToken, actualEndToken);
        }
    }

    protected String getFileAsString(String filename) throws IOException {
        byte[] encoded =
            Files.readAllBytes(Paths.get(getClass().getClassLoader().getResource("samples/" + filename).getPath()));

        return new String(encoded, StandardCharsets.UTF_8);
    }

    protected IStrategoTerm getFileAsAST(String filename) throws IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("samples/" + filename);

        return termReader.parseFromStream(inputStream);
    }

}
