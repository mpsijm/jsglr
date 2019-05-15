package org.spoofax.jsglr2.imploder;

import static org.spoofax.jsglr.client.imploder.ImploderAttachment.putImploderAttachment;

import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr.client.imploder.IToken;
import org.spoofax.jsglr.client.imploder.ImploderAttachment;

public class IterativeStrategoTermTokenizer extends IterativeTreeTokenizer<IStrategoTerm> {
    @Override protected void configure(IStrategoTerm term, String sort, IToken leftToken, IToken rightToken) {
        if(term != null && term.getAttachment(ImploderAttachment.TYPE) == null) {
            // rightToken can be null, e.g. for an empty string lexical
            putImploderAttachment(term, false, sort, leftToken, rightToken != null ? rightToken : leftToken, false,
                false, false, false);
            // When the node is ambiguous, also put an attachment on the list inside the `amb`
            if(term.getTermType() == IStrategoTerm.APPL
                && ((IStrategoAppl) term).getConstructor().getName().equals("amb"))
                putImploderAttachment(term.getSubterm(0), false, null, leftToken,
                    rightToken != null ? rightToken : leftToken, false, false, false, false);
        }
    }

    @Override protected void tokenTreeBinding(IToken token, IStrategoTerm term) {
        token.setAstNode(term);
    }
}
