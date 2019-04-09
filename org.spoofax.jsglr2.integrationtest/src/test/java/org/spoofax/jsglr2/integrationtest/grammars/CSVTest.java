package org.spoofax.jsglr2.integrationtest.grammars;

import org.junit.Test;
import org.spoofax.jsglr2.integrationtest.BaseTestWithSdf3ParseTables;
import org.spoofax.terms.ParseError;

public class CSVTest extends BaseTestWithSdf3ParseTables {

    public CSVTest() {
        super("csv.sdf3");
    }

    @Test public void singleRowSingleColumn() throws ParseError {
        testSuccessByExpansions("42", "Document([Row([Int(\"42\")])])");
        testSuccessByExpansions("\"abc\"", "Document([Row([String(\"\\\"abc\\\"\")])])");
    }

    @Test public void singleRowMultipleColumns() throws ParseError {
        testSuccessByExpansions("1,2", "Document([Row([Int(\"1\"), Int(\"2\")])])");
        testSuccessByExpansions("1 ,	2", "Document([Row([Int(\"1\"), Int(\"2\")])])");
    }

    @Test public void multipleRows() throws ParseError {
        testSuccessByExpansions("1,2\n3", "Document([Row([Int(\"1\"), Int(\"2\")]), Row([Int(\"3\")])])");
    }

    @Test public void rowReuseMiddle() throws ParseError {
        testSubtreeReuse("1, 2\n3, 9, 4\n5, 6\n7, 8", "1, 2\n3, 0, 4\n5, 6\n7, 8", new int[][] {
            // Only the row with 3, 9, 4 is not fully reused
            { 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, // Row with 1, 2 is reused
            { 1, 0, 0, 0, 0, 0, 4, 0, 0, 0, 0 }, // Cell+ with 3 is reused
            { 1, 0, 0, 0, 0, 0, 4, 0, 0, 4 }, // Cell with 4 is reused
            { 1, 0, 0, 0, 0, 4, 0, 0 }, // Row with 5, 6 is reused
            { 1, 0, 0, 0, 4, 0, 0 }, // Row with 7, 8 is reused
        });
    }

    @Test public void rowReuseEnd() throws ParseError {
        testSubtreeReuse("1, 2\n3, 4\n5, 6\n7, 8, 9", "1, 2\n3, 4\n5, 6\n7, 8, 0", new int[][] {
            // Only the row with 7, 8, 9 is not fully reused
            { 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, // Row with 1, 2 is reused
            { 1, 0, 0, 0, 0, 0, 4, 0, 0 }, // Row with 3, 4 is reused
            { 1, 0, 0, 0, 0, 4, 0, 0 }, // Row with 5, 6 is reused
            { 1, 0, 0, 0, 4, 0, 0, 0 }, // Cell+ with 7, 8 is reused
        });
    }

}
