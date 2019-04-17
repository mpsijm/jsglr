package org.spoofax.jsglr2.benchmark;

import org.spoofax.jsglr2.JSGLR2;
import org.spoofax.jsglr2.parser.ParseException;
import org.spoofax.jsglr2.testset.ParseForestInput;
import org.spoofax.jsglr2.testset.TestSet;

public class BenchmarkParseForestInputTestSetReader extends BenchmarkTestSetReader<ParseForestInput> {
    private JSGLR2 jsglr2;

    public BenchmarkParseForestInputTestSetReader(TestSet testSet) {
        super(testSet);
    }

    @Override protected ParseForestInput getInput(String filename, String input) {
        if(jsglr2 == null)
            throw new IllegalStateException("JSGLR2 parser not set for TestSetReader");

        try {
            return new ParseForestInput(filename, input, jsglr2.parser.parseUnsafe(input, filename, null));
        } catch(ParseException e) {
            throw new IllegalStateException("Parsing should succeed for benchmark");
        }
    }

    public void setJsglr2(JSGLR2 jsglr2) {
        this.jsglr2 = jsglr2;
    }
}
