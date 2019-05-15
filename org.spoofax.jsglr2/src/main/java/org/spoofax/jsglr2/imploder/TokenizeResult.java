package org.spoofax.jsglr2.imploder;

import org.spoofax.jsglr.client.imploder.ITokens;

public class TokenizeResult<AbstractSyntaxTree> {

    public final ITokens tokens;
    public final AbstractSyntaxTree ast;

    public TokenizeResult(ITokens tokens, AbstractSyntaxTree ast) {
        this.tokens = tokens;
        this.ast = ast;
    }

}
