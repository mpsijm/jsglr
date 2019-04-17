package org.spoofax.jsglr2.benchmark.jsglr2;

import java.io.IOException;

import org.metaborg.sdf2table.parsetable.query.ActionsForCharacterRepresentation;
import org.metaborg.sdf2table.parsetable.query.ProductionToGotoRepresentation;
import org.openjdk.jmh.annotations.Param;
import org.spoofax.jsglr2.JSGLR2Variants.ParserVariant;
import org.spoofax.jsglr2.benchmark.BenchmarkParseForestInputTestSetReader;
import org.spoofax.jsglr2.imploder.IImploder;
import org.spoofax.jsglr2.imploder.ImploderVariant;
import org.spoofax.jsglr2.integration.IntegrationVariant;
import org.spoofax.jsglr2.integration.ParseTableVariant;
import org.spoofax.jsglr2.parseforest.IParseForest;
import org.spoofax.jsglr2.parseforest.ParseForestConstruction;
import org.spoofax.jsglr2.parseforest.ParseForestRepresentation;
import org.spoofax.jsglr2.reducing.Reducing;
import org.spoofax.jsglr2.stack.StackRepresentation;
import org.spoofax.jsglr2.stack.collections.ActiveStacksRepresentation;
import org.spoofax.jsglr2.stack.collections.ForActorStacksRepresentation;
import org.spoofax.jsglr2.testset.ParseForestInput;
import org.spoofax.jsglr2.testset.TestSet;

public abstract class JSGLR2BenchmarkImploding extends JSGLR2Benchmark<ParseForestInput> {

    protected JSGLR2BenchmarkImploding(TestSet testSet) {
        super(new BenchmarkParseForestInputTestSetReader(testSet));
    }

    @Param({ "true" }) public boolean implode;

    @Param({ "DisjointSorted" }) ActionsForCharacterRepresentation actionsForCharacterRepresentation;

    @Param({ "JavaHashMap" }) ProductionToGotoRepresentation productionToGotoRepresentation;

    @Param({ "ArrayList" }) public ActiveStacksRepresentation activeStacksRepresentation;

    @Param({ "ArrayDeque" }) public ForActorStacksRepresentation forActorStacksRepresentation;

    @Param({ "Hybrid" }) public ParseForestRepresentation parseForestRepresentation;

    @Param({ "Full" }) public ParseForestConstruction parseForestConstruction;

    @Param({ "HybridElkhound" }) public StackRepresentation stackRepresentation;

    @Param({ "Elkhound" }) public Reducing reducing;

    @Param({ "CombinedRecursive", "SeparateRecursive", "SeparateIterative" }) public ImploderVariant imploderVariant;

    @Override public void setupInputs() {
        // Setting up inputs will only happen after the parser has been set up
    }

    @Override protected void postParserSetup() {
        ((BenchmarkParseForestInputTestSetReader) testSetReader).setJsglr2(jsglr2);
        try {
            super.setupInputs();
        } catch(IOException e) {
            throw new IllegalStateException("Setting up inputs should succeed!");
        }
    }

    @Override protected IntegrationVariant variant() {
        return new IntegrationVariant(
            new ParseTableVariant(actionsForCharacterRepresentation, productionToGotoRepresentation),
            new ParserVariant(activeStacksRepresentation, forActorStacksRepresentation, parseForestRepresentation,
                parseForestConstruction, stackRepresentation, reducing),
            imploderVariant);
    }

    @Override protected boolean implode() {
        return implode;
    }

    @SuppressWarnings("unchecked") @Override protected Object action(ParseForestInput input) {
        return ((IImploder<IParseForest, ?>) jsglr2.imploder).implode(input.content, input.filename, input.parseForest);
    }

}

