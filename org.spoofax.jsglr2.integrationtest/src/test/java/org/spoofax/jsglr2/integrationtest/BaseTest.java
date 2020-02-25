package org.spoofax.jsglr2.integrationtest;

import static com.google.common.collect.Iterables.isEmpty;
import static java.util.Collections.sort;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.function.Executable;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr.client.imploder.IToken;
import org.spoofax.jsglr2.JSGLR2;
import org.spoofax.jsglr2.JSGLR2Result;
import org.spoofax.jsglr2.JSGLR2Success;
import org.spoofax.jsglr2.integration.IntegrationVariant;
import org.spoofax.jsglr2.integration.ParseTableVariant;
import org.spoofax.jsglr2.integration.WithParseTable;
import org.spoofax.jsglr2.messages.Message;
import org.spoofax.jsglr2.parseforest.IParseForest;
import org.spoofax.jsglr2.parseforest.IParseNode;
import org.spoofax.jsglr2.parseforest.ParseForestRepresentation;
import org.spoofax.jsglr2.parser.IParser;
import org.spoofax.jsglr2.parser.ParseException;
import org.spoofax.jsglr2.parser.Position;
import org.spoofax.jsglr2.parser.result.ParseResult;
import org.spoofax.jsglr2.parser.result.ParseSuccess;
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

    protected Iterable<ParseTableWithOrigin> getParseTablesOrFailOnException(ParseTableVariant variant) {
        try {
            return getParseTables(variant);
        } catch(Exception e) {
            e.printStackTrace();

            fail("Exception during reading parse table: " + e.getMessage());

            return null;
        }
    }

    static class TestVariant {

        IntegrationVariant variant;
        ParseTableWithOrigin parseTableWithOrigin;

        TestVariant(IntegrationVariant variant, ParseTableWithOrigin parseTableWithOrigin) {
            this.variant = variant;
            this.parseTableWithOrigin = parseTableWithOrigin;
        }

        String name() {
            return variant.name() + "(parseTableOrigin:" + parseTableWithOrigin.origin + ")";
        }

        IParser<? extends IParseForest> parser() {
            return variant.parser.getParser(parseTableWithOrigin.parseTable);
        }

        JSGLR2<IStrategoTerm> jsglr2() {
            return variant.jsglr2.getJSGLR2(parseTableWithOrigin.parseTable);
        }

    }

    protected Stream<TestVariant> getTestVariants(Predicate<TestVariant> filter) {
        List<TestVariant> testVariants = new ArrayList<>();

        for(IntegrationVariant variant : IntegrationVariant.testVariants()) {
            for(ParseTableWithOrigin parseTableWithOrigin : getParseTablesOrFailOnException(variant.parseTable)) {
                TestVariant testVariant = new TestVariant(variant, parseTableWithOrigin);

                // data-dependent, layout-sensitive, and composite parsers are incompatible with Aterm parse table
                if((variant.parser.parseForestRepresentation.equals(ParseForestRepresentation.DataDependent)
                    || variant.parser.parseForestRepresentation.equals(ParseForestRepresentation.LayoutSensitive)
                    || variant.parser.parseForestRepresentation.equals(ParseForestRepresentation.Composite))
                    && parseTableWithOrigin.origin.equals(ParseTableOrigin.ATerm)) {
                    continue;
                }

                if(filter.test(testVariant))
                    testVariants.add(testVariant);
            }
        }

        return testVariants.stream();
    }

    protected Stream<TestVariant> getTestVariants() {
        return getTestVariants(testVariant -> true);
    }

    protected Stream<DynamicTest> testPerVariant(Stream<TestVariant> variants, Function<TestVariant, Executable> body) {
        return variants.map(variant -> DynamicTest.dynamicTest(variant.name(), body.apply(variant)));
    }

    protected Stream<DynamicTest> testParseSuccess(String inputString) {
        return testParseSuccess(inputString, getTestVariants());
    }

    protected Stream<DynamicTest> testParseSuccess(String inputString, Stream<TestVariant> variants) {
        return testPerVariant(variants, variant -> () -> {
            ParseResult<?> parseResult = variant.parser().parse(inputString);

            assertEquals(true, parseResult.isSuccess(), "Parsing failed");
        });
    }

    protected Stream<DynamicTest> testParseFailure(String inputString) {
        return testParseFailure(inputString, getTestVariants());
    }

    protected Stream<DynamicTest> testParseFailure(String inputString, Stream<TestVariant> variants) {
        return testPerVariant(variants, variant -> () -> {
            ParseResult<?> parseResult = variant.parser().parse(inputString);

            assertEquals(false, parseResult.isSuccess(), "Parsing should fail");
        });
    }

    protected Stream<DynamicTest> testSuccessByAstString(String inputString, String expectedOutputAstString) {
        return testSuccess(inputString, expectedOutputAstString, null, false);
    }

    protected Stream<DynamicTest> testSuccessByExpansions(String inputString, String expectedOutputAstString) {
        return testSuccess(inputString, expectedOutputAstString, null, true);
    }

    protected Stream<DynamicTest> testIncrementalSuccessByExpansions(String[] inputStrings,
        String[] expectedOutputAstStrings) {
        return testIncrementalSuccess(inputStrings, expectedOutputAstStrings, null, true);
    }

    protected Stream<DynamicTest> testSuccessByAstString(String startSymbol, String inputString,
        String expectedOutputAstString) {
        return testSuccess(inputString, expectedOutputAstString, startSymbol, false);
    }

    protected Stream<DynamicTest> testSuccessByExpansions(String startSymbol, String inputString,
        String expectedOutputAstString) {
        return testSuccess(inputString, expectedOutputAstString, startSymbol, true);
    }

    private Stream<DynamicTest> testSuccess(String inputString, String expectedOutputAstString, String startSymbol,
        boolean equalityByExpansions) {
        return testPerVariant(getTestVariants(), variant -> () -> {
            IStrategoTerm actualOutputAst = testSuccess(variant, startSymbol, inputString);

            assertEqualAST("Incorrect AST", expectedOutputAstString, actualOutputAst, equalityByExpansions);
        });
    }

    protected IStrategoTerm testSuccess(TestVariant variant, String startSymbol, String inputString) {
        return testSuccess("Parsing failed", "Imploding failed", variant.jsglr2(), "", startSymbol, inputString);
    }

    private IStrategoTerm testSuccess(String parseFailMessage, String implodeFailMessage, JSGLR2<IStrategoTerm> jsglr2,
        String fileName, String startSymbol, String inputString) {
        try {
            IStrategoTerm result = jsglr2.parseUnsafe(inputString, fileName, startSymbol);

            // Fail here if imploding or tokenization failed
            assertNotNull(result, implodeFailMessage);

            return result;
        } catch(ParseException e) {
            // Fail here if parsing failed
            fail(parseFailMessage + e.failureType);
        }
        return null;
    }

    protected Predicate<TestVariant> isIncrementalVariant =
        testVariant -> testVariant.variant.parser.parseForestRepresentation == ParseForestRepresentation.Incremental;

    private Stream<DynamicTest> testIncrementalSuccess(String[] inputStrings, String[] expectedOutputAstStrings,
        String startSymbol, boolean equalityByExpansions) {
        return testIncrementalSuccess(inputStrings, expectedOutputAstStrings, startSymbol, equalityByExpansions,
            getTestVariants(isIncrementalVariant));
    }

    private Stream<DynamicTest> testIncrementalSuccess(String[] inputStrings, String[] expectedOutputAstStrings,
        String startSymbol, boolean equalityByExpansions, Stream<TestVariant> variants) {
        return testPerVariant(variants, variant -> () -> {
            JSGLR2<IStrategoTerm> jsglr2 = variant.jsglr2();
            IStrategoTerm actualOutputAst;
            String fileName = "" + System.nanoTime(); // To ensure the results will be cached
            for(int i = 0; i < expectedOutputAstStrings.length; i++) {
                String inputString = inputStrings[i];
                actualOutputAst = testSuccess("Parsing failed at update " + i + ": ",
                    "Imploding failed at update " + i + ": ", jsglr2, fileName, startSymbol, inputString);
                assertEqualAST("Incorrect AST at update " + i + ": ", expectedOutputAstStrings[i], actualOutputAst,
                    equalityByExpansions);
            }
        });
    }

    protected Stream<DynamicTest> testSubtreeReuse(String inputString1, String inputString2,
        ParseNodeDescriptor... parseNodeDescriptors) {
        return testPerVariant(getTestVariants(isIncrementalVariant), variant -> () -> {
            @SuppressWarnings("unchecked") IParser<IParseForest> parser = (IParser<IParseForest>) variant.parser();
            // ((IObservableParser<?, ?>) parser).observing()
            // .attachObserver(new org.spoofax.jsglr2.parser.observing.ParserLogObserver<>());
            ParseResult<IParseForest> parse1 = parser.parse(inputString1);
            assertTrue(parse1.isSuccess(), "Parse 1 of " + inputString1 + " failed!");
            IParseForest parseForest1 = ((ParseSuccess<?>) parse1).parseResult;
            ParseResult<IParseForest> parse2 = parser.parse(inputString2, inputString1, parseForest1);
            assertTrue(parse2.isSuccess(), "Parse 2 of " + inputString2 + " failed!");
            IParseForest parseForest2 = ((ParseSuccess<?>) parse2).parseResult;

            Map<IParseForest, IParseNode<?, ?>> cache = populateCache(parseForest1);
            Map<IParseForest, Integer> offsets = calculateOffsets(parseForest1);
            List<IParseNode<?, ?>> reused = checkReuse(cache, parseForest2);
            assertEquals(parseNodeDescriptors.length, reused.size(),
                "Length of reused nodes not equal! Reused: " + reused);
            for(int i = 0; i < parseNodeDescriptors.length; i++) {
                parseNodeDescriptors[i].assertSame(offsets.get(reused.get(i)), reused.get(i));
            }
        });
    }

    private Map<IParseForest, IParseNode<?, ?>> populateCache(IParseForest parseForest) {
        Map<IParseForest, IParseNode<?, ?>> cache = new HashMap<>();
        Queue<IParseForest> queue = new LinkedList<>();
        queue.add(parseForest);
        while(!queue.isEmpty()) {
            IParseForest current = queue.poll();
            if(current instanceof IParseNode) {
                @SuppressWarnings("rawtypes") IParseNode parseNode = (IParseNode) current;
                cache.put(current, parseNode);
                if(!isEmpty(parseNode.getDerivations())) {
                    queue.addAll(Arrays.asList(parseNode.getFirstDerivation().parseForests()));
                }
            }
        }
        return cache;
    }

    private Map<IParseForest, Integer> calculateOffsets(IParseForest parseForest) {
        Map<IParseForest, Integer> offsets = new HashMap<>();
        offsets.put(parseForest, 0);
        Queue<IParseForest> queue = new LinkedList<>();
        queue.add(parseForest);
        while(!queue.isEmpty()) {
            IParseForest current = queue.poll();
            int offset = offsets.get(current);
            if(current instanceof IParseNode) {
                @SuppressWarnings("rawtypes") IParseNode parseNode = (IParseNode) current;
                if(!isEmpty(parseNode.getDerivations())) {
                    for(IParseForest child : parseNode.getFirstDerivation().parseForests()) {
                        offsets.put(child, offset);
                        offset += child.width();
                        queue.add(child);
                    }
                }
            }
        }
        return offsets;
    }

    private List<IParseNode<?, ?>> checkReuse(Map<IParseForest, IParseNode<?, ?>> cache, IParseForest parseForest) {
        List<IParseNode<?, ?>> reused = new ArrayList<>();
        Queue<IParseForest> queue = new LinkedList<>();
        queue.add(parseForest);
        while(!queue.isEmpty()) {
            IParseForest current = queue.poll();
            if(current instanceof IParseNode) {
                @SuppressWarnings("rawtypes") IParseNode parseNode = (IParseNode) current;
                if(cache.containsKey(current)) {
                    reused.add(parseNode);
                    continue;
                }
                if(!isEmpty(parseNode.getDerivations())) {
                    queue.addAll(Arrays.asList(parseNode.getFirstDerivation().parseForests()));
                }
            }
        }
        return reused;
    }

    protected void assertEqualAST(String message, String expectedOutputAstString, IStrategoTerm actualOutputAst,
        boolean equalityByExpansions) {
        if(equalityByExpansions) {
            IStrategoTerm expectedOutputAst = termReader.parseFromString(expectedOutputAstString);

            assertEqualTermExpansions(message, expectedOutputAst, actualOutputAst);
        } else {
            assertEquals(expectedOutputAstString, actualOutputAst.toString(), message);
        }
    }

    protected static void assertEqualTermExpansions(IStrategoTerm expected, IStrategoTerm actual) {
        assertEqualTermExpansions(null, expected, actual);
    }

    protected static void assertEqualTermExpansions(String message, IStrategoTerm expected, IStrategoTerm actual) {
        List<String> expectedExpansion = toSortedStringList(astUtilities.expand(expected));
        List<String> actualExpansion = toSortedStringList(astUtilities.expand(actual));

        assertEquals(expectedExpansion, actualExpansion, message);
    }

    private static List<String> toSortedStringList(List<IStrategoTerm> astExpansion) {
        List<String> result = new ArrayList<>(astExpansion.size());

        for(IStrategoTerm ast : astExpansion) {
            result.add(ast.toString());
        }

        sort(result);

        return result;
    }

    protected Stream<DynamicTest> testTokens(String inputString, List<TokenDescriptor> expectedTokens) {
        return testTokens(inputString, expectedTokens, getTestVariants());
    }

    protected Stream<DynamicTest> testTokens(String inputString, List<TokenDescriptor> expectedTokens,
        Stream<TestVariant> variants) {
        return testPerVariant(variants, variant -> () -> {
            JSGLR2Result<?> jsglr2Result = variant.jsglr2().parseResult(inputString, "", null);

            assertTrue(jsglr2Result.isSuccess(), "Parsing failed");

            JSGLR2Success<?> jsglr2Success = (JSGLR2Success<?>) jsglr2Result;

            List<TokenDescriptor> actualTokens = new ArrayList<>();

            for(IToken token : jsglr2Success.tokens) {
                actualTokens.add(TokenDescriptor.from(inputString, token));
            }

            TokenDescriptor expectedStartToken = new TokenDescriptor("", IToken.TK_RESERVED, 0, 1, 1, null, null);
            TokenDescriptor actualStartToken = actualTokens.get(0);

            assertEquals(expectedStartToken, actualStartToken, "Start token incorrect");

            Position endPosition = Position.atEnd(inputString);

            int endLine = endPosition.line;
            int endColumn = endPosition.column;

            TokenDescriptor expectedEndToken =
                new TokenDescriptor("", IToken.TK_EOF, inputString.length(), endLine, endColumn - 1, null, null);
            TokenDescriptor actualEndToken = actualTokens.get(actualTokens.size() - 1);

            List<TokenDescriptor> actualTokensWithoutStartAndEnd = actualTokens.subList(1, actualTokens.size() - 1);

            assertIterableEquals(expectedTokens, actualTokensWithoutStartAndEnd, "Token lists don't match");

            assertEquals(expectedEndToken, actualEndToken, "End token incorrect");
        });
    }

    protected Stream<DynamicTest> testOrigins(String inputString, List<OriginDescriptor> expectedOrigins) {
        return testOrigins(inputString, expectedOrigins, getTestVariants());
    }

    protected Stream<DynamicTest> testOrigins(String inputString, List<OriginDescriptor> expectedOrigins,
        Stream<TestVariant> variants) {
        return testPerVariant(variants, variant -> () -> {
            JSGLR2Result<IStrategoTerm> jsglr2Result = variant.jsglr2().parseResult(inputString, "", null);

            assertTrue(jsglr2Result.isSuccess(), "Parsing failed");

            IStrategoTerm ast = ((JSGLR2Success<IStrategoTerm>) jsglr2Result).ast;

            List<OriginDescriptor> actualOrigins = new ArrayList<>();

            Stack<IStrategoTerm> terms = new Stack<>();

            terms.push(ast);

            while(!terms.isEmpty()) {
                IStrategoTerm term = terms.pop();

                for(int i = term.getSubtermCount() - 1; i >= 0; i--)
                    terms.push(term.getSubterm(i));

                if(term instanceof IStrategoAppl) {
                    actualOrigins.add(OriginDescriptor.from(term));
                }
            }

            assertIterableEquals(expectedOrigins, actualOrigins, "Origin lists don't match");
        });
    }

    protected Stream<DynamicTest> testMessages(String inputString, List<MessageDescriptor> expectedMessages) {
        return testMessages(inputString, expectedMessages, getTestVariants(), null);
    }

    protected Stream<DynamicTest> testMessages(String inputString, List<MessageDescriptor> expectedMessages,
        Stream<TestVariant> variants) {
        return testMessages(inputString, expectedMessages, variants, null);
    }

    protected Stream<DynamicTest> testMessages(String inputString, List<MessageDescriptor> expectedMessages,
        String startSymbol) {
        return testMessages(inputString, expectedMessages, getTestVariants(), startSymbol);
    }

    protected Stream<DynamicTest> testMessages(String inputString, List<MessageDescriptor> expectedMessages,
        Stream<TestVariant> variants, String startSymbol) {
        return testPerVariant(variants, variant -> () -> {
            JSGLR2Result<?> jsglr2Result = variant.jsglr2().parseResult(inputString, "", startSymbol);

            List<MessageDescriptor> actualMessages = new ArrayList<>();

            for(Message message : jsglr2Result.messages) {
                actualMessages.add(MessageDescriptor.from(message));
            }

            assertIterableEquals(expectedMessages, actualMessages, "Message lists don't match");
        });
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
