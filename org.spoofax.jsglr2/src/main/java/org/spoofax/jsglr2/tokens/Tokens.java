package org.spoofax.jsglr2.tokens;

import static org.spoofax.jsglr.client.imploder.IToken.Kind.TK_EOF;
import static org.spoofax.jsglr.client.imploder.IToken.Kind.TK_RESERVED;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import javax.annotation.Nonnull;

import org.metaborg.parsetable.productions.IProduction;
import org.spoofax.jsglr.client.imploder.IToken;
import org.spoofax.jsglr.client.imploder.Token;
import org.spoofax.jsglr.client.imploder.Tokenizer;
import org.spoofax.jsglr2.parser.Position;

public class Tokens implements IParseTokens {

    private static final long serialVersionUID = 2054391299757162697L;

    private final String fileName;
    private final String input;

    private IToken startToken, endToken;

    private final ArrayList<IToken> tokens;

    public Tokens(String input, String fileName) {
        this.input = input;
        this.fileName = fileName;

        this.tokens = new ArrayList<>();
    }

    public IToken startToken() {
        return startToken;
    }

    public IToken endToken() {
        return endToken;
    }

    public void makeStartToken() {
        startToken = new Token(this, fileName, 0, 1, 1, 0, -1, TK_RESERVED);

        addToken(startToken);
    }

    public void makeEndToken(Position endPosition) {
        endToken = new Token(this, fileName, tokens.size(), endPosition.line, endPosition.column,
            endPosition.offset, -1, TK_EOF);

        addToken(endToken);
    }

    public IToken makeToken(Position startPosition, Position endPosition, IProduction production) {
        IToken.Kind tokenKind;

        if(startPosition.equals(endPosition)) {
            tokenKind = IToken.Kind.TK_NO_TOKEN_KIND;
        } else if(production == null) {
            tokenKind = IToken.Kind.TK_STRING; // indicates a character/int terminal, e.g. 'x'
        } else if(production.isLayout()) {
            tokenKind = IToken.Kind.TK_LAYOUT;
        } else if(production.isLiteral()) {
            if(production.isOperator())
                tokenKind = IToken.Kind.TK_OPERATOR;
            else
                tokenKind = IToken.Kind.TK_KEYWORD;
        } else if(production.isLexical()) {
            if(production.isStringLiteral())
                tokenKind = IToken.Kind.TK_STRING;
            else if(production.isNumberLiteral())
                tokenKind = IToken.Kind.TK_NUMBER;
            else
                tokenKind = IToken.Kind.TK_IDENTIFIER;
        } else {
            throw new IllegalStateException("invalid production/token type");
        }

        IToken token = new Token(this, fileName, tokens.size(), startPosition.line, startPosition.column,
            startPosition.offset, endPosition.offset - 1, tokenKind);

        addToken(token);

        return token;
    }

    private void addToken(IToken token) {
        tokens.add(token);
    }

    @Override @Nonnull public Iterator<IToken> iterator() {
        return new Tokenizer.AmbiguousToNonAmbiguousIterator(ambiguousTokens());
    }

    @Override @Nonnull public Iterable<IToken> ambiguousTokens() {
        return Collections.unmodifiableList(tokens);
    }

    @Override public String getInput() {
        return input;
    }

    @Override public int getTokenCount() {
        return tokens.size();
    }

    @Override public IToken getTokenAtOffset(int offset) {
        for(IToken token : tokens) {
            if(token.getStartOffset() == offset)
                return token;
        }

        return null;
    }

    @Override public String getFilename() {
        return fileName;
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

    @Override public String toString() {
        return tokens.toString();
    }

}
