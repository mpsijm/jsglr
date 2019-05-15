package org.spoofax.jsglr2.tokens;

import static org.spoofax.jsglr.client.imploder.IToken.TK_EOF;
import static org.spoofax.jsglr.client.imploder.IToken.TK_RESERVED;
import static org.spoofax.jsglr.client.imploder.ImploderAttachment.putImploderAttachment;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Stack;

import org.metaborg.parsetable.IProduction;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr.client.imploder.IToken;
import org.spoofax.jsglr.client.imploder.ITokens;
import org.spoofax.jsglr2.imploder.ITokenizer;
import org.spoofax.jsglr2.imploder.TokenizeResult;
import org.spoofax.jsglr2.imploder.TreeImploder;
import org.spoofax.jsglr2.parser.Position;

import com.google.common.collect.Lists;

public class TreeTokens implements ITokens {

    private static final long serialVersionUID = 2054391299757162697L;

    private final String filename;
    private final String input;

    private TreeToken startToken, endToken;
    private TokenTree tree;

    public class TokenTree {
        private final TreeImploder.SubTree<IStrategoTerm> tree;
        private final TreeToken token; // null for internal nodes
        private final TokenTree[] children;
        private final Position beginPosition;
        private final Position endPosition;
        private final int size;
        private final IToken leftToken;
        private final IToken rightToken;
        private IStrategoTerm enclosingAst;

        protected TokenTree(TreeImploder.SubTree<IStrategoTerm> tree, TreeToken token, Position beginPosition,
            Position endPosition) {
            this.tree = tree;
            this.leftToken = this.rightToken = this.token = token;
            this.children = new TokenTree[0];
            this.beginPosition = beginPosition;
            this.endPosition = endPosition;
            this.size = 1;
        }

        protected TokenTree(TreeImploder.SubTree<IStrategoTerm> tree, TokenTree[] children, Position beginPosition,
            Position endPosition) {
            this.tree = tree;
            this.token = null;
            this.children = children;
            this.beginPosition = beginPosition;
            this.endPosition = endPosition;

            int size = 0;
            for(TokenTree child : children) {
                size += child.size;
            }
            this.size = size;

            IToken leftToken = null;
            IToken rightToken = null;
            for(TokenTree child : children) {
                // The left-most token of this tree is the first non-null leftToken of a subTree
                if(leftToken == null)
                    leftToken = child.leftToken;

                // The right-most token of this tree is the last non-null rightToken of a subTree
                if(child.rightToken != null)
                    rightToken = child.rightToken;
            }
            this.leftToken = leftToken;
            this.rightToken = rightToken;
        }

        protected void setEnclosingAst(IStrategoTerm tree) {
            this.enclosingAst = tree;
        }
    }

    public TreeTokens(String input, String filename) {
        this.input = input;
        this.filename = filename;
        startToken = new TreeToken(this, new Position(0, 1, 0), new Position(0, 1, 0), 0, TK_RESERVED, null, null);
        // TODO set end position based on tree
        Position endInputPosition = Position.atEnd(input);
        Position endStartPosition =
            new Position(endInputPosition.offset, endInputPosition.line, endInputPosition.column - 1);
        Position endEndPosition = new Position(-1, endStartPosition.line, endStartPosition.column);
        // TODO token index?
        endToken = new TreeToken(this, endStartPosition, endEndPosition, -1, TK_EOF, null, null);
    }

    public static class Tokenizer implements ITokenizer<TreeImploder.SubTree<IStrategoTerm>, IStrategoTerm> {
        @Override public TokenizeResult<IStrategoTerm> tokenize(String input, String filename,
            TreeImploder.SubTree<IStrategoTerm> tree) {
            TreeTokens tokens = new TreeTokens(input, filename);

            TreeTokens.TokenTree res = tokenizeInternal(tokens, tree, new Position(0, 1, 1));
            tokens.bind(res);

            return new TokenizeResult<>(tokens, res.tree.tree);
        }

        private TreeTokens.TokenTree tokenizeInternal(TreeTokens tokens, TreeImploder.SubTree<IStrategoTerm> tree,
            Position startPosition) {
            if(tree.children.size() == 0) {
                if(tree.width > 0 || tree.production.isLexical() || tree.production.isLexicalRhs()) {
                    Position endPosition = startPosition.step(tokens.getInput(), tree.width);
                    TreeToken token = new TreeToken(tokens, startPosition, endPosition, -1,
                        IToken.getTokenKind(tree.production), tree.tree, null);
                    TreeTokens.TokenTree tokenTree = tokens.new TokenTree(tree, token, startPosition, endPosition);
                    token.tree = tokenTree;
                    return tokenTree;
                }
                return tokens.new TokenTree(tree, (TreeToken) null, startPosition, startPosition);
            } else {
                TreeTokens.TokenTree[] children = new TreeTokens.TokenTree[tree.children.size()];
                Position pivotPosition = startPosition;
                List<TreeImploder.SubTree<IStrategoTerm>> subTrees = tree.children;
                for(int i = 0; i < subTrees.size(); i++) {
                    TreeTokens.TokenTree subTree = tokenizeInternal(tokens, subTrees.get(i), pivotPosition);
                    children[i] = subTree;

                    // If tree ast == null, that means it's layout or literal lexical;
                    // that means it needs to be bound to the current tree
                    if(subTree.tree.tree == null)
                        subTree.setEnclosingAst(tree.tree);

                    // If tree production == null, that means it's an "amb" node; in that case, position is not advanced
                    if(tree.production != null)
                        pivotPosition = subTree.endPosition;
                }
                return tokens.new TokenTree(tree, children, startPosition, pivotPosition);
            }
        }
    }

    protected void bind(TokenTree res) {
        tree = res;
        startToken.setAstNode(res.tree.tree);
        endToken.setAstNode(res.tree.tree);

        IToken lastToken = this.startToken;
        Stack<TokenTree> stack = new Stack<>();
        stack.push(tree);
        while(!stack.isEmpty()) {
            TokenTree tokenTree = stack.pop();
            IStrategoTerm term = tokenTree.tree.tree;
            if(term != null) {
                IProduction production = tokenTree.tree.production;
                String sort = production == null ? null : production.sort();
                IToken leftToken = tokenTree.leftToken;
                IToken rightToken = tokenTree.rightToken;
                if(leftToken == null)
                    leftToken = lastToken;
                if(rightToken == null)
                    rightToken = lastToken;
                putImploderAttachment(term, false, sort, leftToken, rightToken, false, false, false, false);
                // When the node is ambiguous (production == null), also put an attachment on the list inside the
                // `amb`
                if(production == null)
                    putImploderAttachment(term.getSubterm(0), false, null, leftToken, rightToken, false, false, false,
                        false);
                lastToken = leftToken;
            }


            for(int i = tokenTree.children.length - 1; i >= 0; i--) {
                stack.push(tokenTree.children[i]);
            }
        }
    }

    class TokenIterator implements Iterator<IToken> {
        Stack<TokenTree> stack = new Stack<>();

        TokenIterator(boolean includeStartEnd) {
            if(includeStartEnd)
                stack.push(new TokenTree(null, endToken, null, null));
            stack.push(tree);
            if(includeStartEnd)
                stack.push(new TokenTree(null, startToken, null, null));
        }

        @Override public boolean hasNext() {
            while(!stack.isEmpty()) {
                boolean updated = false;
                while(!stack.isEmpty() && stack.peek().children.length > 0) {
                    TokenTree pop = stack.pop();
                    for(int i = pop.children.length - 1; i >= 0; i--) {
                        stack.push(pop.children[i]);
                    }
                    updated = true;
                }
                while(!stack.isEmpty() && stack.peek().children.length == 0 && stack.peek().token == null) {
                    stack.pop();
                    updated = true;
                }
                if(!updated)
                    return true;
            }
            return false;
        }

        @Override public IToken next() {
            if(!hasNext())
                throw new NoSuchElementException();
            return stack.pop().token;
        }
    }

    @Override public Iterator<IToken> iterator() {
        return new TokenIterator(true);
    }

    @Override public String getInput() {
        return input;
    }

    @Override public int getTokenCount() {
        return tree.size;
    }

    @Override public IToken getTokenAt(int index) {
        // TODO Doing O(h) traversal would be better
        return Lists.newArrayList(this).get(index);
    }

    @Override public IToken getTokenAtOffset(int offset) {
        // TODO Doing O(h) traversal would be better
        for(IToken token : this) {
            if(token.getStartOffset() == offset)
                return token;
        }

        return null;
    }

    @Override public String getFilename() {
        return filename;
    }

    @Override public String toString(IToken left, IToken right) {
        int startOffset = left.getStartOffset();
        int endOffset = right.getEndOffset();

        if(startOffset >= 0 && endOffset >= 0)
            return toString(startOffset, endOffset + 1);
        else
            return "";
    }

    @Override public String toString(int startOffset, int endOffset) {
        return input.substring(startOffset, endOffset);
    }

    @Override public boolean isAmbiguous() {
        return false; // TODO: implement
    }

    @Override public String toString() {
        return Lists.newArrayList(this).toString();
    }

}
