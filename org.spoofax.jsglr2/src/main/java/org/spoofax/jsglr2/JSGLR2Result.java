package org.spoofax.jsglr2;

import org.spoofax.jsglr.client.imploder.ITokens;
import org.spoofax.jsglr2.imploder.TokenizeResult;

public final class JSGLR2Result<AbstractSyntaxTree> {

    public final boolean isSuccess;
    public final ITokens tokens;
    public final AbstractSyntaxTree ast;

    /**
     * Constructs a result in the case that the parse failed. The fields `tokens` and `ast` are set to `null`.
     */
    JSGLR2Result() {
        this.isSuccess = false;
        this.tokens = null;
        this.ast = null;
    }

    /**
     * Constructs a result in the case that the parse succeeded.
     * 
     * @param tokenizeResult
     *            The result from the tokenizer.
     */
    JSGLR2Result(TokenizeResult<AbstractSyntaxTree> tokenizeResult) {
        this.isSuccess = true;
        this.tokens = tokenizeResult.tokens;
        this.ast = tokenizeResult.ast;
    }

}
