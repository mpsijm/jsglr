package org.spoofax.jsglr2.imploder;

import org.spoofax.jsglr.client.imploder.IToken;
import org.spoofax.jsglr2.parser.Position;
import org.spoofax.jsglr2.tokens.Tokens;

public abstract class TreeTokenizer<Tree> implements ITokenizer<TreeImploder.SubTree<Tree>, Tree> {
    class SubTree {
        final Tree tree;
        final IToken leftToken;
        final IToken rightToken;
        final Position endPosition;
        final boolean isLayout;

        SubTree(TreeImploder.SubTree<Tree> tree, IToken leftToken, IToken rightToken, Position endPosition) {
            this.tree = tree.tree;
            this.leftToken = leftToken;
            this.rightToken = rightToken;
            this.endPosition = endPosition;
            this.isLayout = tree.production != null && tree.production.isLayout();
            if(tree.tree != null && leftToken != null) {
                String sort = tree.production == null ? null : tree.production.sort();
                configure(tree.tree, sort, leftToken, rightToken);
            }
        }

    }

    @Override
    public TokenizeResult<Tree> tokenize(String input, String filename, TreeImploder.SubTree<Tree> tree) {
        Tokens tokens = new Tokens(input, filename);
        return new TokenizeResult<>(tokens, tokenize(tokens, tree));
    }

    protected Tree tokenize(Tokens tokens, TreeImploder.SubTree<Tree> tree) {
        tokens.makeStartToken();
        tokenTreeBinding(tokens.startToken(), tree.tree);

        SubTree res = tokenizeInternal(tokens, tree, new Position(0, 1, 1), tokens.startToken());

        tokens.makeEndToken(res.endPosition);
        tokenTreeBinding(tokens.endToken(), res.tree);

        return res.tree;
    }

    private SubTree tokenizeInternal(Tokens tokens, TreeImploder.SubTree<Tree> tree, Position startPosition,
        IToken lastToken) {
        if(tree.children.size() == 0) {
            if(tree.width > 0 || tree.production.isLexical() || tree.production.isLexicalRhs()) {
                Position endPosition = startPosition.step(tokens.getInput(), tree.width);
                IToken token = tokens.makeToken(startPosition, endPosition, tree.production);
                tokenTreeBinding(token, tree.tree);
                return new SubTree(tree, token, token, endPosition);
            }
            return new SubTree(tree, lastToken, null, startPosition);
        } else {
            IToken leftToken = lastToken;
            IToken rightToken = null;
            Position pivotPosition = startPosition;
            IToken pivotToken = lastToken;
            for(TreeImploder.SubTree<Tree> child : tree.children) {
                SubTree subTree = tokenizeInternal(tokens, child, pivotPosition, pivotToken);

                // If child tree had tokens that were not yet bound, bind them
                if(child.tree == null) {
                    if(subTree.leftToken != null)
                        tokenTreeBinding(subTree.leftToken, tree.tree);

                    if(subTree.rightToken != null)
                        tokenTreeBinding(subTree.rightToken, tree.tree);
                }

                // Set the parent tree left and right token from the outermost non-layout left and right child tokens
                if(!subTree.isLayout) {
                    if(leftToken == lastToken)
                        leftToken = subTree.leftToken;

                    if(subTree.rightToken != null) {
                        rightToken = subTree.rightToken;
                        pivotToken = subTree.rightToken;
                    }
                }

                // If tree production == null, that means it's an "amb" node; in that case, position is not advanced
                if(tree.production != null)
                    pivotPosition = subTree.endPosition;
            }
            return new SubTree(tree, leftToken, rightToken, pivotPosition);
        }
    }

    protected abstract void configure(Tree term, String sort, IToken leftToken, IToken rightToken);

    protected abstract void tokenTreeBinding(IToken token, Tree term);

}
