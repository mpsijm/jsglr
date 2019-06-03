package org.spoofax.jsglr2.integrationtest;

import static org.junit.Assert.fail;

import org.metaborg.parsetable.IParseTable;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr.client.InvalidParseTableException;
import org.spoofax.jsglr.shared.SGLRException;
import org.spoofax.jsglr2.integration.IntegrationVariant;
import org.spoofax.jsglr2.integration.WithJSGLR1;

public abstract class BaseTestWithParseTableFromTermWithJSGLR1 extends BaseTestWithParseTableFromTerm
    implements WithJSGLR1 {

    protected void testSuccessByJSGLR1(String inputString) {
        try {
            IStrategoTerm expectedOutputAst = (IStrategoTerm) getJSGLR1().parse(inputString, null, null).output;

            for(IntegrationVariant variant : IntegrationVariant.testVariants()) {
                IParseTable parseTable = getParseTableFailOnException(variant.parseTable);
                IStrategoTerm actualOutputAst = testSuccess(parseTable, variant.jsglr2, null, inputString);

                assertEqualAST("Variant '" + variant.name() + "' does not have the same AST as JSGLR1",
                    expectedOutputAst, actualOutputAst);
            }
        } catch(SGLRException | InterruptedException | InvalidParseTableException e) {
            e.printStackTrace();

            fail();
        }
    }

}
