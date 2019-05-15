package org.spoofax.jsglr2.tokens;

import java.util.Objects;

import org.spoofax.interpreter.terms.ISimpleTerm;
import org.spoofax.jsglr.client.imploder.AbstractTokenizer;
import org.spoofax.jsglr.client.imploder.IToken;
import org.spoofax.jsglr.client.imploder.ITokens;
import org.spoofax.jsglr2.parser.Position;

public class TreeToken implements IToken, Cloneable {

    private static final long serialVersionUID = -7306530908136122951L;

    private transient TreeTokens tokens;

    private Position startPosition;

    private Position endPosition;

    private int index;

    private int kind;

    private String errorMessage;

    private ISimpleTerm astNode;

    // TODO IEL
    public TreeTokens.TokenTree tree;

    public TreeToken(TreeTokens tokens, Position startPosition, Position endPosition, int index, int kind,
        ISimpleTerm astNode, TreeTokens.TokenTree tree) {
        this.tokens = tokens;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.index = index;
        this.kind = kind;
        this.astNode = astNode;
        this.tree = tree;
    }

    @Override public ITokens getTokenizer() {
        return tokens;
    }

    @Override public int getKind() {
        return kind;
    }

    @Override public void setKind(int kind) {
        this.kind = kind;
    }

    @Override public int getIndex() {
        return index;
    }

    void setIndex(int index) {
        this.index = index;
    }

    @Override public final int getStartOffset() {
        return startPosition.offset;
    }

    @Override public final int getEndOffset() {
        return endPosition.offset - 1;
    }

    @Override public int getLine() {
        return startPosition.line;
    }

    @Override public int getEndLine() {
        return endPosition.line;
    }

    @Override public int getColumn() {
        return startPosition.column;
    }

    @Override public int getEndColumn() {
        return endPosition.column - 1;
    }

    void setStartPosition(Position startPosition) {
        this.startPosition = startPosition;
    }

    void setEndPosition(Position endPosition) {
        this.endPosition = endPosition;
    }

    @Override public int getLength() {
        return getEndOffset() - getStartOffset() + 1;
    }

    @Override public String getFilename() {
        return tokens.getFilename();
    }

    /**
     * Gets the error message associated with this token, if any.
     *
     * Note that this message is independent from the token kind, which may also indicate an error.
     */
    @Override public String getError() {
        return errorMessage;
    }

    /**
     * Sets a syntax error for this token. (Setting any other kind of error would break cacheability.)
     */
    public void setError(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override public void setAstNode(ISimpleTerm astNode) {
        this.astNode = astNode;
    }

    @Override public ISimpleTerm getAstNode() {
        if(astNode == null) {
            ITokens tokens = getTokenizer();

            // This is a hack. For jsglr1 the AST binding might not be done yet. For jsglr2 it is always done during
            // imploding.
            if(tokens instanceof AbstractTokenizer)
                ((AbstractTokenizer) getTokenizer()).initAstNodeBinding();
        }
        return astNode;
    }

    @Override public String toString() {
        return tokens.toString(this, this);
    }

    @Override public char charAt(int index) {
        return tokens.getInput().charAt(index + getStartOffset());
    }

    @Override public int hashCode() {
        return Objects.hash(startPosition, endPosition, index, kind, astNode);
    }

    @Override public boolean equals(Object o) {
        if(this == o)
            return true;
        if(o == null || getClass() != o.getClass())
            return false;
        TreeToken token = (TreeToken) o;
        return index == token.index && kind == token.kind && Objects.equals(startPosition, token.startPosition)
            && Objects.equals(endPosition, token.endPosition) && Objects.equals(astNode, token.astNode);
    }

    @Override public int compareTo(IToken other) {
        return getStartOffset() - other.getStartOffset();
    }

    @Override public TreeToken clone() {
        try {
            return (TreeToken) super.clone();
        } catch(CloneNotSupportedException e) {
            // Must be supported for IToken
            throw new RuntimeException(e);
        }
    }

}
