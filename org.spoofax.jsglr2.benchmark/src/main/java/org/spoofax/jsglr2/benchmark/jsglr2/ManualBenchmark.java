package org.spoofax.jsglr2.benchmark.jsglr2;

import static org.spoofax.jsglr2.benchmark.jsglr2.JSGLR2BenchmarkIncremental.ParserType;
import static org.spoofax.terms.util.TermUtils.isAppl;

import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.metaborg.parsetable.productions.IProduction;
import org.metaborg.util.iterators.Iterables2;
import org.openjdk.jmh.infra.Blackhole;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr.client.imploder.IToken;
import org.spoofax.jsglr.client.imploder.ITokens;
import org.spoofax.jsglr.client.imploder.ImploderAttachment;
import org.spoofax.jsglr2.JSGLR2Failure;
import org.spoofax.jsglr2.JSGLR2Result;
import org.spoofax.jsglr2.incremental.EditorUpdate;
import org.spoofax.jsglr2.incremental.diff.IStringDiff;
import org.spoofax.jsglr2.incremental.diff.JGitHistogramDiff;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalDerivation;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseForest;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalParseNode;
import org.spoofax.jsglr2.incremental.parseforest.IncrementalSkippedNode;
import org.spoofax.jsglr2.inputstack.incremental.IIncrementalInputStack;
import org.spoofax.jsglr2.parseforest.ICharacterNode;
import org.spoofax.jsglr2.parseforest.IParseForest;
import org.spoofax.jsglr2.parseforest.IParseNode;
import org.spoofax.jsglr2.parser.AbstractParseState;
import org.spoofax.jsglr2.parser.ForShifterElement;
import org.spoofax.jsglr2.parser.ParseException;
import org.spoofax.jsglr2.parser.observing.IParserObserver;
import org.spoofax.jsglr2.parser.result.ParseFailure;
import org.spoofax.jsglr2.parser.result.ParseResult;
import org.spoofax.jsglr2.parser.result.ParseSuccess;
import org.spoofax.jsglr2.stack.hybrid.HybridStackNode;
import org.spoofax.jsglr2.testset.testinput.IncrementalStringInput;

public class ManualBenchmark {

    private static final IParserObserver<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode<IncrementalParseForest>, AbstractParseState<IIncrementalInputStack, HybridStackNode<IncrementalParseForest>>> COUNTING_OBSERVER =
        new IParserObserver<IncrementalParseForest, IncrementalDerivation, IncrementalParseNode, HybridStackNode<IncrementalParseForest>, AbstractParseState<IIncrementalInputStack, HybridStackNode<IncrementalParseForest>>>() {
            int createChar, createNode, shiftChar, shiftNode;
            Map<BreakdownReason, Integer> breakdown;

            @Override public void
                parseStart(AbstractParseState<IIncrementalInputStack, HybridStackNode<IncrementalParseForest>> parse) {
                breakdown = new HashMap<>();
                createChar = 0;
                createNode = 0;
                shiftChar = 0;
                shiftNode = 0;
            }


            @Override public void createParseNode(IncrementalParseNode parseNode, IProduction production) {
                createNode++;
            }

            @Override public void createCharacterNode(IncrementalParseForest characterNode, int character) {
                createChar++;
            }

            @Override public void success(ParseSuccess<IncrementalParseForest> success) {
                System.out.printf("Breakdown: %s    Create(N/C): %6d %6d    Shift: %6d %6d\n", breakdown, createNode,
                    createChar, shiftNode, shiftChar);
            }

            @Override public void shifter(IncrementalParseForest termNode,
                Queue<ForShifterElement<HybridStackNode<IncrementalParseForest>>> forShifter) {
                if(termNode instanceof IncrementalParseNode)
                    shiftNode++;
                else
                    shiftChar++;
            }

            @Override public void breakDown(IIncrementalInputStack inputStack, BreakdownReason reason) {
                breakdown.merge(reason, 1, Integer::sum);
            }
        };

    public static void main(String[] args) throws Exception {
        // JSGLR2Java8BenchmarkIncrementalParsing benchmark =
        // getIncrementalBenchmark(new JSGLR2Java8BenchmarkIncrementalParsing(), ParserType.Incremental);

        // JSGLR2Java8BenchmarkIncrementalParsingAndImploding benchmark =
        // getIncrementalBenchmark(new JSGLR2Java8BenchmarkIncrementalParsingAndImploding(), ParserType.Incremental);

        // JSGLR2OCamlGitBenchmarkIncrementalParsing benchmark = getIncrementalBenchmark(
        // new JSGLR2OCamlGitBenchmarkIncrementalParsing(), ParserType.Incremental);

        // JSGLR2Java8GitBenchmarkIncrementalParsing benchmark =
        // getIncrementalBenchmark(new JSGLR2Java8GitBenchmarkIncrementalParsing(), ParserType.Incremental);

        // JSGLR2SumNonAmbiguousBenchmarkIncrementalParsing benchmark = getIncrementalBenchmark(
        // new JSGLR2SumNonAmbiguousBenchmarkIncrementalParsing(), ParserType.Incremental, 4200);

        // JSGLR2SumNonAmbiguousBenchmarkIncrementalParsingAndImploding benchmark = getIncrementalBenchmark(
        // new JSGLR2SumNonAmbiguousBenchmarkIncrementalParsingAndImploding(), ParserType.Incremental, 420);

        // JSGLR2WebDSLBenchmarkIncrementalParsing benchmark =
        // getIncrementalBenchmark(new JSGLR2WebDSLBenchmarkIncrementalParsing(), ParserType.Incremental);

        // JSGLR2WebDSLGitBenchmarkIncrementalParsing benchmark =
        // getIncrementalBenchmark(new JSGLR2WebDSLGitBenchmarkIncrementalParsing(), ParserType.Incremental);

        // JSGLR2WebDSLBenchmarkIncrementalParsingAndImploding benchmark =
        // getIncrementalBenchmark(new JSGLR2WebDSLBenchmarkIncrementalParsingAndImploding(), ParserType.Incremental);

        Map<String, String> a = new HashMap<>();

        a.put("language", "java");
        a.put("extension", "java");
        a.put("parseTablePath",
            // "/home/maarten/git/thesis/jsglr2evaluation/tmp/.old/20201022-first-with-sdf3-sources/languages/java/lang.java/target/metaborg/sdf.tbl");
            // "/home/maarten/git/thesis/jsglr2evaluation/tmp/.old/20201027-first-with-logs/languages/java/lang.java/target/metaborg/sdf.tbl");
            // "/home/maarten/git/thesis/jsglr2evaluation/tmp/languages/java/lang.java/target/metaborg/sdf.tbl");
            "/home/maarten/git/thesis/java-front/lang.java/target/metaborg/sdf.tbl");
        a.put("sourcePath",
            // "/home/maarten/git/thesis/jsglr2evaluation/tmp/.old/20201022-first-with-sdf3-sources/sources/java/incremental/apache-commons-lang-stringutils");
            // "/home/maarten/git/thesis/jsglr2evaluation/tmp/.old/20201027-first-with-logs/sources/java/incremental/apache-commons-lang-stringutils");
             "/home/maarten/git/thesis/jsglr2evaluation/tmp/.old/20201117-first-with-memory-benchmark/sources/java/incremental/apache-commons-lang-stringutils");
//            "/home/maarten/git/thesis/jsglr2evaluation/tmp/sources/java/incremental/apache-commons-lang-stringutils");

//         a.put("language", "sdf3");
//         a.put("extension", "sdf3");
//         a.put("parseTablePath",
//         "/home/maarten/git/thesis/jsglr2evaluation/tmp/.old/20201022-first-with-sdf3-sources/languages/sdf3/org.metaborg.meta.lang.template/target/metaborg/sdf.tbl");
//         // a.put("sourcePath", "/home/maarten/git/thesis/jsglr2evaluation/tmp/sources/sdf3/incremental/dynsem");
//         a.put("sourcePath",
//         "/home/maarten/git/thesis/jsglr2evaluation/tmp/.old/20201022-first-with-sdf3-sources/sources/sdf3/incremental/nabl");

        a.put("iteration", "-1");
        JSGLR2BenchmarkIncrementalExternal benchmark =
            getIncrementalBenchmark(new JSGLR2BenchmarkIncrementalExternal(a), ParserType.Incremental);
         benchmark.implode = true;
        // previewBenchmarkExternal(benchmark, Integer.parseInt(a.get("iteration")));
        a.put("iteration", "0");
        previewBenchmarkExternal(benchmark, Integer.parseInt(a.get("iteration")));
        a.put("iteration", "1");
        previewBenchmarkExternal(benchmark, Integer.parseInt(a.get("iteration")));

        // noinspection unchecked,rawtypes
        // ((org.spoofax.jsglr2.parser.IObservableParser) benchmark.jsglr2.parser).observing()
        // .attachObserver(COUNTING_OBSERVER);

        // printSizePerVersion(benchmark.getInputs());

        // doManualBenchmark(benchmark, true);

        // previewBenchmark(benchmark);
    }

    private static void doManualBenchmark(JSGLR2BenchmarkIncrementalParsingAndImploding benchmark, boolean printReuse) {
        for(IncrementalStringInput testInput : benchmark.getInputs()) {
            System.out.println("File " + testInput.fileName);
            String[] inputs = testInput.content;
            IStrategoTerm current = null;
            IStrategoTerm previous;
            for(int i = 0; i < inputs.length; i++) {
                String input = inputs[i];
                previous = current;
                JSGLR2Result<?> jsglr2Result = benchmark.jsglr2.parseResult(input, testInput.fileName, null);
                if(jsglr2Result.isSuccess()) {
                    current = (IStrategoTerm) benchmark.jsglr2.parse(input, testInput.fileName, null);
                } else {
                    System.out.println("FAILED!");
                    System.out.println(((JSGLR2Failure<?>) jsglr2Result).parseFailure.parseState.inputStack.offset());
                    break;
                }
                if(printReuse) {
                    System.out.printf("Iteration %3d: \n", i);
                    printReuse(previous, current);
                }
            }
            System.out.println();
        }
    }

    private static void doManualBenchmark(JSGLR2BenchmarkIncrementalParsing benchmark, boolean printReuse) {
        for(IncrementalStringInput testInput : benchmark.getInputs()) {
            System.out.println("File " + testInput.fileName);
            String[] inputs = testInput.content;
            IParseForest current = null;
            IParseForest previous = null;
            long begin = System.currentTimeMillis();
            for(int i = 0; i < inputs.length; i++) {
                String input = inputs[i];
                if(printReuse)
                    System.out.printf("Iteration %3d: \n", i);
                ParseResult<?> parseResult =
                    benchmark.jsglr2.parser.parse(input, i == 0 ? null : inputs[i - 1], previous);
                if(parseResult.isSuccess()) {
                    current = ((ParseSuccess<?>) parseResult).parseResult;
                } else {
                    System.out.println("FAILED!");
                    AbstractParseState<?, ?> parseState = ((ParseFailure<?>) parseResult).parseState;
                    int offset = parseState.inputStack.offset();
                    String inputString = parseState.inputStack.inputString();
                    System.out.println("Offset " + offset);
                    System.out.println(inputString.substring(offset - 40, Math.min(inputString.length(), offset + 40))
                        .replaceAll("\n", "⏎"));
                    System.out.println("                                        ^");
                    break;
                }
                if(printReuse) {
                    System.out.println("Time: " + (System.currentTimeMillis() - begin) + " ms");
                    begin = System.currentTimeMillis();
                    printReuse(previous, current);
                }
                previous = current;
            }
            System.out.println();
        }
    }

    private static void previewBenchmark(JSGLR2BenchmarkIncremental benchmark) {
        for(int j = 0; j < 2; j++) {
            for(IncrementalStringInput input : benchmark.getInputs()) {
                System.out.println(input.fileName);
                for(int i = -1; i < input.content.length; i++) {
                    long l = previewBenchmarkIteration(benchmark, input, i);
                    System.out.println(i + ": " + l + " ms");
                }
            }
        }
    }

    private static void previewBenchmarkExternal(JSGLR2BenchmarkIncremental benchmark, int i) {
        for(int j = 0; j < 5; j++) {
            for(IncrementalStringInput input : benchmark.getInputs()) {
                System.out.println(input.fileName);
                long l = previewBenchmarkIteration(benchmark, input, i);
                System.out.println(i + ": " + l + " ms");
            }
        }
    }

    private static long previewBenchmarkIteration(JSGLR2BenchmarkIncremental benchmark, IncrementalStringInput input,
        int i) {
        long begin;
        try {
            benchmark.i = i;
            benchmark.setupCache();
            // printHeapSize();
            // beginHeapMeasure();
            begin = System.currentTimeMillis();
            // for(int j = 0; j < 100; j++) {
            Object o = benchmark.action(
                new Blackhole(
                    "Today's password is swordfish. I understand instantiating Blackholes directly is dangerous."),
                input);
            // }
            long time = System.currentTimeMillis() - begin;
            // endHeapMeasure(benchmark);
            // printHeapSize();
            if(o instanceof IncrementalParseForest)
                printStats((IncrementalParseForest) o);
            return time;
        } catch(ParseException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private static long freeMemory, totalMemory;
    private static final Runtime runtime = Runtime.getRuntime();

    private static void beginHeapMeasure() {
        System.gc();
        freeMemory = runtime.freeMemory();
        totalMemory = runtime.totalMemory();
    }

    private static void endHeapMeasure(JSGLR2BenchmarkIncremental benchmark) {
        if(runtime.totalMemory() != totalMemory) {
            System.out.println("Memory measurement failed because GC kicked in");
            return;
        }

        long usedNew = totalMemory - runtime.freeMemory();
        long usedOld = totalMemory - freeMemory;
        System.gc();
        long usedAfterGc = runtime.totalMemory() - runtime.freeMemory();

        benchmark.prevParse.clear();
        benchmark.prevCacheImpl.clear();
        System.gc();
        long usedAfterClear = runtime.totalMemory() - runtime.freeMemory();

        System.out.println(new DecimalFormat("###,###").format(usedNew - usedOld));
        System.out.println(new DecimalFormat("###,###").format(usedAfterGc - usedOld));
        System.out.println(new DecimalFormat("###,###").format(usedAfterGc - usedAfterClear));
    }

    private static void printHeapSize() {
        System.out.println("Free/current/max heap size: " + runtime.freeMemory() + "/" + runtime.totalMemory() + "/"
            + runtime.maxMemory());
        System.gc();
        System.out.println("Free/current/max heap size: " + runtime.freeMemory() + "/" + runtime.totalMemory() + "/"
            + runtime.maxMemory());
    }

    private static void printSizePerVersion(Iterable<IncrementalStringInput> inputs) {
        int n = inputs.iterator().next().content.length;
        IStringDiff diff = new JGitHistogramDiff();
        System.out.println("Version\tSize (chars)\tRemoved\tAdded\tUpdates");
        for(int i = 0; i < n; i++) {
            int finalI = i;
            System.out.println(i + "\t"
                + Iterables2.stream(inputs).mapToInt(input -> input.content[finalI].length()).sum() + "\t"
                + Iterables2.stream(inputs)
                    .mapToInt(input -> diff.diff(finalI == 0 ? "" : input.content[finalI - 1], input.content[finalI])
                        .stream().mapToInt(EditorUpdate::deletedLength).sum())
                    .sum()
                + "\t"
                + Iterables2.stream(inputs)
                    .mapToInt(input -> diff.diff(finalI == 0 ? "" : input.content[finalI - 1], input.content[finalI])
                        .stream().mapToInt(EditorUpdate::insertedLength).sum())
                    .sum()
                + "\t"
                + Iterables2.stream(inputs)
                    .mapToInt(
                        input -> diff.diff(finalI == 0 ? "" : input.content[finalI - 1], input.content[finalI]).size())
                    .sum());
        }
    }

    private static <T extends JSGLR2BenchmarkIncrementalParsingAndImploding> T getIncrementalBenchmark(T benchmark,
        ParserType parserType) throws Exception {
        return getIncrementalBenchmark(benchmark, parserType, -1);
    }

    private static <T extends JSGLR2BenchmarkIncrementalParsingAndImploding> T getIncrementalBenchmark(T benchmark,
        ParserType parserType, int n) throws Exception {
        benchmark.n = n;
        benchmark.parserType = parserType;

        benchmark.setupInputs();
        benchmark.parserSetup();
        benchmark.setupCache();
        return benchmark;
    }

    private static <T extends JSGLR2BenchmarkIncrementalParsing> T getIncrementalBenchmark(T benchmark,
        ParserType parserType) throws Exception {
        return getIncrementalBenchmark(benchmark, parserType, -1);
    }

    private static <T extends JSGLR2BenchmarkIncrementalParsing> T getIncrementalBenchmark(T benchmark,
        ParserType parserType, int n) throws Exception {
        benchmark.n = n;
        benchmark.parserType = parserType;

        benchmark.setupInputs();
        benchmark.parserSetup();
        benchmark.setupCache();
        return benchmark;
    }

    private static <T extends JSGLR2BenchmarkIncrementalExternal> T getIncrementalBenchmark(T benchmark,
        ParserType parserType) throws Exception {
        benchmark.n = -1;
        benchmark.parserType = parserType;

        benchmark.setupInputs();
        benchmark.parserSetup();
        benchmark.setupCache();
        return benchmark;
    }

    private static boolean treeEqual(IParseForest one, IParseForest two) {
        if(one instanceof ICharacterNode && two instanceof ICharacterNode)
            return ((ICharacterNode) one).character() == ((ICharacterNode) two).character();
        if(!(one instanceof IParseNode && two instanceof IParseNode))
            return false;
        IncrementalParseNode oneN = (IncrementalParseNode) one;
        IncrementalParseNode twoN = (IncrementalParseNode) two;
        IParseForest[] ones = ((IParseNode<?, ?>) one).getFirstDerivation().parseForests();
        IParseForest[] twos = ((IParseNode<?, ?>) two).getFirstDerivation().parseForests();
        if(oneN.state().id() != twoN.state().id() || oneN.production().id() != twoN.production().id()
            || ones.length != twos.length) {
            System.err.println(one);
            System.err.println(two);
            return false;
        }
        for(int i = 0; i < ones.length; i++) {
            if(!treeEqual(ones[i], twos[i])) {
                System.err.println(ones[i]);
                System.err.println(twos[i]);
                return false;
            }
        }
        return true;
    }

    private static void printReuseString(IParseForest parse1, IParseForest parse2) {
        Set<IParseForest> nodeSet = new HashSet<>();

        Stack<IParseForest> todo1 = new Stack<>();
        todo1.add(parse1);
        while(!todo1.isEmpty()) {
            IParseForest t1 = todo1.pop();
            nodeSet.add(t1);
            if(t1 instanceof IParseNode) {
                IParseForest[] sub1 = ((IParseNode<?, ?>) t1).getFirstDerivation().parseForests();
                for(int i = sub1.length - 1; i >= 0; i--) {
                    todo1.add(sub1[i]);
                }
            }
        }

        Stack<IParseForest> todo2 = new Stack<>();
        todo2.add(parse2);
        List<IParseForest> reused = new ArrayList<>();
        while(!todo2.isEmpty()) {
            IParseForest t2 = todo2.pop();
            if(nodeSet.contains(t2))
                reused.add(t2);
            else {
                if(t2 instanceof IParseNode) {
                    IParseForest[] sub2 = ((IParseNode<?, ?>) t2).getFirstDerivation().parseForests();
                    for(int i = sub2.length - 1; i >= 0; i--) {
                        todo2.add(sub2[i]);
                    }
                }
            }
        }

        System.out
            .println(reused.stream().map(t -> ((IncrementalParseForest) t).getYield()).collect(Collectors.joining("")));
    }

    private static IdentityHashMap<IStrategoTerm, Set<IToken>> tokensCache = new IdentityHashMap<>();

    private static void printReuse(IStrategoTerm parse1, IStrategoTerm parse2) {
        int nodes = 0, leaves = 0, ambs = 0, reusedNodes = 0, reusedLeaves = 0;
        Set<IStrategoTerm> nodeSet = new IdentityHashSet<>(), leafSet = new IdentityHashSet<>();

        Stack<IStrategoTerm> todo1 = new Stack<>();
        todo1.add(parse1);
        while(!todo1.isEmpty()) {
            IStrategoTerm t1 = todo1.pop();
            if(t1 != null && t1.getSubtermCount() > 0) {
                nodeSet.add(t1);
                IStrategoTerm[] sub1 = t1.getAllSubterms();
                for(int i = sub1.length - 1; i >= 0; i--) {
                    todo1.add(sub1[i]);
                }
            } else {
                leafSet.add(t1);
            }
        }

        Stack<IStrategoTerm> todo2 = new Stack<>();
        todo2.add(parse2);
        while(!todo2.isEmpty()) {
            IStrategoTerm t2 = todo2.pop();
            if(t2.getSubtermCount() > 0) {
                nodes++;
                if(nodeSet.contains(t2))
                    reusedNodes++;
                if(isAppl(t2) && ((IStrategoAppl) t2).getConstructor().getName().equals("amb"))
                    ambs++;
                IStrategoTerm[] sub2 = t2.getAllSubterms();
                for(int i = sub2.length - 1; i >= 0; i--) {
                    todo2.add(sub2[i]);
                }
            } else {
                leaves++;
                if(leafSet.contains(t2))
                    reusedLeaves++;
            }
        }

        Set<IToken> tokenSet = new IdentityHashSet<>();
        ITokens tokenizer = ImploderAttachment.get(parse2).getLeftToken().getTokenizer();
        for(IToken token : tokenizer) {
            tokenSet.add(token);
        }
        tokensCache.put(parse2, tokenSet);
        int tokens = tokenSet.size(), reusedTokens = 0;
        Set<IToken> previousTokenizer = tokensCache.get(parse1);
        if(previousTokenizer != null) {
            for(IToken token : previousTokenizer) {
                if(tokenSet.contains(token))
                    reusedTokens++;
            }
        }

        System.out.println("Ambigous: " + ambs);
        System.out.println("Internal: " + nodes);
        System.out.println("  Reused: " + reusedNodes);
        System.out.println("Leaves:   " + leaves);
        System.out.println("  Reused: " + reusedLeaves);
        System.out.println("Tokens:   " + tokens);
        System.out.println("  Reused: " + reusedTokens);
        System.out.println();
    }

    private static void printReuse(IParseForest parse1, IParseForest parse2) {
        int nodes = 0, leaves = 0, ambs = 0, nondets = 0, reusedNodes = 0, reusedLeaves = 0, rebuilt = 0;
        Set<IParseForest> nodeSet = new HashSet<>(), leafSet = new HashSet<>();

        Stack<IParseForest> todo1 = new Stack<>();
        todo1.add(parse1);
        while(!todo1.isEmpty()) {
            IParseForest t1 = todo1.pop();
            if(t1 instanceof IParseNode) {
                nodeSet.add(t1);
                if(!(t1 instanceof IncrementalSkippedNode)) {
                    IParseForest[] sub1 = ((IParseNode<?, ?>) t1).getFirstDerivation().parseForests();
                    for(int i = sub1.length - 1; i >= 0; i--) {
                        todo1.add(sub1[i]);
                    }
                }
            } else {
                leafSet.add(t1);
            }
        }

        Map<IParseForest, IParseNode<?, ?>> parents = new HashMap<>();
        Stack<IParseForest> todo2 = new Stack<>();
        todo2.add(parse2);
        while(!todo2.isEmpty()) {
            IParseForest t2 = todo2.pop();
            if(t2 instanceof IParseNode) {
                nodes++;
                if(nodeSet.contains(t2))
                    reusedNodes++;
                IParseNode<?, ?> t2Node = (IParseNode<?, ?>) t2;
                if(t2Node.isAmbiguous())
                    ambs++;
                if(!((IncrementalParseForest) t2Node).isReusable())
                    nondets++;
                if(!(t2 instanceof IncrementalSkippedNode)) {
                    IParseForest[] sub2 = t2Node.getFirstDerivation().parseForests();
                    for(int i = sub2.length - 1; i >= 0; i--) {
                        parents.put(sub2[i], t2Node);
                        todo2.add(sub2[i]);
                    }
                    IParseNode<?, ?> parent = t2Node;
                    while(!nodeSet.contains(parent) && parent.getFirstDerivation().parseForests().length > 0
                        && Arrays.stream(parent.getFirstDerivation().parseForests()).allMatch(nodeSet::contains)) {
                        rebuilt++;
                        nodeSet.add(parent);
                        parent = parents.get(parent);
                    }
                }
            } else {
                leaves++;
                if(leafSet.contains(t2))
                    reusedLeaves++;
            }
        }

        System.out.println("Ambigous: " + ambs);
        System.out.println("Non-det.: " + nondets);
        System.out.println("Internal: " + nodes);
        System.out.println("  Reused: " + reusedNodes);
        System.out.println("  Rebult: " + rebuilt);
        System.out.println("Leaves:   " + leaves);
        System.out.println("  Reused: " + reusedLeaves);
        System.out.println();
    }

    private static void printStats(IncrementalParseForest parseForest) {
        int nodes = 0, leaves = 0, ambs = 0, nondets = 0;

        Stack<IParseForest> todo2 = new Stack<>();
        todo2.add(parseForest);
        while(!todo2.isEmpty()) {
            IParseForest t2 = todo2.pop();
            if(t2 instanceof IParseNode) {
                nodes++;
                IParseNode<?, ?> t2Node = (IParseNode<?, ?>) t2;
                if(t2Node.isAmbiguous())
                    ambs++;
                if(!((IncrementalParseForest) t2Node).isReusable())
                    nondets++;
                if(!(t2 instanceof IncrementalSkippedNode)) {
                    IParseForest[] sub2 = t2Node.getFirstDerivation().parseForests();
                    for(int i = sub2.length - 1; i >= 0; i--) {
                        todo2.add(sub2[i]);
                    }
                }
            } else {
                leaves++;
            }
        }

        System.out.println("Ambigous: " + ambs);
        System.out.println("Non-det.: " + nondets);
        System.out.println("Internal: " + nodes);
        System.out.println("Leaves:   " + leaves);
        System.out.println();
    }

    public static class IdentityHashSet<E> implements Set<E> {
        IdentityHashMap<E, E> map = new IdentityHashMap<>();

        @Override public int size() {
            return map.size();
        }

        @Override public boolean isEmpty() {
            return map.isEmpty();
        }

        @Override public boolean contains(Object o) {
            return map.containsKey(o);
        }

        @NotNull @Override public Iterator<E> iterator() {
            return map.keySet().iterator();
        }

        @NotNull @Override public Object[] toArray() {
            return map.keySet().toArray();
        }

        @NotNull @Override public <T> T[] toArray(@NotNull T[] ts) {
            return map.keySet().toArray(ts);
        }

        @Override public boolean add(E e) {
            return map.put(e, e) == null;
        }

        @Override public boolean remove(Object o) {
            return map.remove(o, o);
        }

        @Override public boolean containsAll(@NotNull Collection<?> collection) {
            return collection.stream().allMatch(map::containsKey);
        }

        @Override public boolean addAll(@NotNull Collection<? extends E> collection) {
            return collection.stream().map(this::add).anyMatch(b -> b);
        }

        @Override public boolean retainAll(@NotNull Collection<?> collection) {
            throw new UnsupportedOperationException();
        }

        @Override public boolean removeAll(@NotNull Collection<?> collection) {
            return collection.stream().map(this::remove).anyMatch(b -> b);
        }

        @Override public void clear() {
            map.clear();
        }
    }
}
