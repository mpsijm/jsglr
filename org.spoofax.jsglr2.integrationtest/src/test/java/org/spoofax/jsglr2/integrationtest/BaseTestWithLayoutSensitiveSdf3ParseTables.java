package org.spoofax.jsglr2.integrationtest;

import java.util.Arrays;

import org.junit.BeforeClass;
import org.metaborg.core.MetaborgException;
import org.metaborg.parsetable.IParseTable;
import org.metaborg.sdf2table.io.ParseTableIO;
import org.metaborg.sdf2table.parsetable.ParseTable;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr2.integration.ParseTableVariant;
import org.spoofax.jsglr2.integration.Sdf3ToParseTable;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

public abstract class BaseTestWithLayoutSensitiveSdf3ParseTables extends BaseTest {

    private String sdf3Resource;
    private static Table<String, ParseTableVariant, IParseTable> parseTableTable = HashBasedTable.create();

    protected BaseTestWithLayoutSensitiveSdf3ParseTables(String sdf3Resource) {
        this.sdf3Resource = sdf3Resource;
    }

    private static Sdf3ToParseTable sdf3ToParseTable;

    @BeforeClass public static void setup() throws MetaborgException {
        sdf3ToParseTable = new Sdf3ToParseTable(resource -> BaseTestWithLayoutSensitiveSdf3ParseTables.class
            .getClassLoader().getResource(resource).getPath());
    }

    public ParseTableWithOrigin getParseTable(ParseTableVariant variant) throws Exception {
        if(!parseTableTable.contains(sdf3Resource, variant)) {
            parseTableTable.put(sdf3Resource, variant,
                sdf3ToParseTable.getLayoutSensitiveParseTable(variant, sdf3Resource));
        }
        return new ParseTableWithOrigin(parseTableTable.get(sdf3Resource, variant), ParseTableOrigin.Sdf3Generation);
    }

    @Override public Iterable<ParseTableWithOrigin> getParseTables(ParseTableVariant variant) throws Exception {
        ParseTableWithOrigin parseTableWithOrigin = getParseTable(variant);

        IStrategoTerm parseTableTerm = ParseTableIO.generateATerm((ParseTable) parseTableWithOrigin.parseTable);

        ParseTableWithOrigin parseTableWithOriginSerializedDeserialized =
            new ParseTableWithOrigin(variant.parseTableReader().read(parseTableTerm), ParseTableOrigin.ATerm);

        // Ensure that the parse table that directly comes from the generation behaves the same after
        // serialization/deserialization to/from term format
        return Arrays.asList(parseTableWithOrigin, parseTableWithOriginSerializedDeserialized);
    }

}