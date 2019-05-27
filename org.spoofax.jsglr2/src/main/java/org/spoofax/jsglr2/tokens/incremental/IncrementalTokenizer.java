package org.spoofax.jsglr2.tokens.incremental;

import java.util.HashMap;
import java.util.Map;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr2.imploder.ImplodeResult;
import org.spoofax.jsglr2.imploder.TreeImploder;
import org.spoofax.jsglr2.parser.Position;
import org.spoofax.jsglr2.tokens.TreeTokens;

public class IncrementalTokenizer extends TreeTokens.Tokenizer {
    private Map<String, TreeImploder.SubTree<IStrategoTerm>> inputCache = new HashMap<>();
    private Map<String, TreeTokens> outputCache = new HashMap<>();
    private Map<String, TreeTokens.TokenTree> tokensCache = new HashMap<>();

    @Override public ImplodeResult<IStrategoTerm> tokenize(String input, String filename,
        TreeImploder.SubTree<IStrategoTerm> tree) {
        
        final TreeTokens tokens;
        if(!filename.equals("") && inputCache.containsKey(filename) && tokensCache.containsKey(filename)) {
            tokens = tokensCache.get(filename);
        } else
            tokens = new TreeTokens(input, filename);

        if(!filename.equals("") && inputCache.containsKey(filename) && outputCache.containsKey(filename)) {

        }
        
 = tokenizeInternal(tokens, inputCache.get(filename), new Position(0, 1, 1));
        res = (TreeTokens) super.tokenize(input, filename, tree).tokens;
        if(!filename.equals("")) {
            inputCache.put(filename, tree);
            outputCache.put(filename, res);
        }

        TreeTokens.TokenTree res = tokenizeInternal(tokens, tree, new Position(0, 1, 1));
        tokens.bind(res);

        return new ImplodeResult<>(tokens, res.tree.tree);
    }

    private TreeTokens tokenizeInternal(TreeTokens tokens, TreeImploder.SubTree<IStrategoTerm> tree,
        Position startPosition) {
    }

}
