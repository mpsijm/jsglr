package org.spoofax.jsglr2.imploder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.metaborg.parsetable.IProduction;
import org.spoofax.jsglr.client.imploder.IToken;
import org.spoofax.jsglr2.imploder.treefactory.ITokenizedTreeFactory;
import org.spoofax.jsglr2.layoutsensitive.LayoutSensitiveParseNode;
import org.spoofax.jsglr2.parseforest.IDerivation;
import org.spoofax.jsglr2.parseforest.IParseForest;
import org.spoofax.jsglr2.parseforest.IParseNode;
import org.spoofax.jsglr2.parser.Position;
import org.spoofax.jsglr2.tokens.Tokens;

public abstract class TokenizedTreeImploder
//@formatter:off
   <ParseForest extends IParseForest,
    ParseNode   extends IParseNode<ParseForest, Derivation>,
    Derivation  extends IDerivation<ParseForest>,
    Tree>
//@formatter:on
    implements IImploder<ParseForest, TokenizeResult<Tree>> {

    protected final ITokenizedTreeFactory<Tree> treeFactory;

    public TokenizedTreeImploder(ITokenizedTreeFactory<Tree> treeFactory) {
        this.treeFactory = treeFactory;
    }

    @Override public TokenizeResult<Tree> implode(String input, String filename, ParseForest parseForest) {
        @SuppressWarnings("unchecked") ParseNode topParseNode = (ParseNode) parseForest;

        Tokens tokens = new Tokens(input, filename);
        tokens.makeStartToken();

        Position position = new Position(0, 1, 1);

        SubTree<Tree> tree = implodeParseNode(topParseNode, tokens, position, tokens.startToken());

        tokens.makeEndToken(tree.endPosition);

        tokenTreeBinding(tokens.startToken(), tree.tree);
        tokenTreeBinding(tokens.endToken(), tree.tree);

        return new TokenizeResult<>(tokens, tree.tree);
    }

    static class SubTree<Tree> {

        Tree tree;
        Position endPosition;
        IToken leftToken, rightToken;

        SubTree(Tree tree, Position endPosition, IToken leftToken, IToken rightToken) {
            this.tree = tree;
            this.endPosition = endPosition;
            this.leftToken = leftToken;
            this.rightToken = rightToken;
        }

    }

    protected SubTree<Tree> implodeParseNode(ParseNode parseNode, Tokens tokens, Position startPosition,
        IToken parentLeftToken) {
        IProduction production = parseNode.production();

        if(production.isContextFree()) {
            List<Derivation> filteredDerivations = applyDisambiguationFilters(parseNode);

            if(filteredDerivations.size() > 1) {
                List<Tree> trees = new ArrayList<>(filteredDerivations.size());
                SubTree<Tree> result = null;

                for(Derivation derivation : filteredDerivations) {
                    if(result == null) {
                        result = implodeDerivation(tokens, derivation, startPosition, parentLeftToken);

                        trees.add(result.tree);
                    } else
                        trees.add(implodeDerivation(tokens, derivation, startPosition, parentLeftToken).tree);
                }

                String sort = production.sort();

                result.tree = treeFactory.createAmb(sort, trees, result.leftToken, result.rightToken);

                return result;
            } else
                return implodeDerivation(tokens, filteredDerivations.get(0), startPosition, parentLeftToken);
        } else {
            int width = parseNode.width();

            Position endPosition = startPosition.step(tokens.getInput(), width);

            IToken token = width > 0 ? tokens.makeToken(startPosition, endPosition, production) : null;

            Tree tree;

            if(production.isLayout() || production.isLiteral()) {
                tree = null;
            } else if(production.isLexical() || production.isLexicalRhs()) {
                tree = createLexicalTerm(production, tokens.toString(startPosition.offset, endPosition.offset), token);
            } else {
                throw new RuntimeException("invalid term type");
            }

            return new SubTree<>(tree, endPosition, token, token);
        }
    }

    protected List<Derivation> applyDisambiguationFilters(ParseNode parseNode) {
        if(!parseNode.isAmbiguous())
            return Collections.singletonList(parseNode.getFirstDerivation());

        List<Derivation> result;
        // TODO always filter longest-match?
        if(parseNode instanceof LayoutSensitiveParseNode) {
            ((LayoutSensitiveParseNode) parseNode).filterLongestMatchDerivations();
        }
        // TODO always filter prefer/avoid?
        result = parseNode.getPreferredAvoidedDerivations();

        return result;
    }

    protected SubTree<Tree> implodeDerivation(Tokens tokens, Derivation derivation, Position startPosition,
        IToken parentLeftToken) {
        IProduction production = derivation.production();

        if(!production.isContextFree())
            throw new RuntimeException("non context free imploding not supported");

        List<Tree> childASTs = new ArrayList<>();
        List<IToken> unboundTokens = new ArrayList<>();

        SubTree<Tree> subTree = implodeChildParseNodes(tokens, childASTs, derivation, derivation.production(),
            unboundTokens, startPosition, parentLeftToken);

        subTree.tree = createContextFreeTerm(derivation.production(), childASTs, subTree.leftToken, subTree.rightToken);

        for(IToken token : unboundTokens)
            tokenTreeBinding(token, subTree.tree);

        return subTree;
    }

    protected SubTree<Tree> implodeChildParseNodes(Tokens tokens, List<Tree> childASTs, Derivation derivation,
        IProduction production, List<IToken> unboundTokens, Position startPosition, IToken parentLeftToken) {
        SubTree<Tree> result = new SubTree<>(null, startPosition, parentLeftToken, null);

        Position pivotPosition = startPosition;
        IToken pivotToken = parentLeftToken;

        for(ParseForest childParseForest : derivation.parseForests()) {
            @SuppressWarnings("unchecked") ParseNode childParseNode = (ParseNode) childParseForest;

            if(childParseNode != null) { // Can be null in the case of a layout subtree parse node that is not created
                IProduction childProduction = childParseNode.production();

                SubTree<Tree> subTree;

                if(production.isList() && (childProduction.isList() && childProduction.constructor() == null
                    && childParseNode.getPreferredAvoidedDerivations().size() <= 1)) {
                    // Make sure lists are flattened
                    subTree = implodeChildParseNodes(tokens, childASTs, childParseNode.getFirstDerivation(),
                        childProduction, unboundTokens, pivotPosition, pivotToken);
                } else {
                    subTree = implodeParseNode(childParseNode, tokens, pivotPosition, pivotToken);

                    if(subTree.tree != null)
                        childASTs.add(subTree.tree);

                    // Collect tokens that are not bound to a tree such that they can later be bound to the resulting
                    // parent tree
                    if(subTree.tree == null) {
                        if(subTree.leftToken != null)
                            unboundTokens.add(subTree.leftToken);

                        if(subTree.rightToken != null)
                            unboundTokens.add(subTree.rightToken);
                    }
                }

                // Set the parent tree left and right token from the outermost non-layout left and right child tokens
                if(!childProduction.isLayout()) {
                    if(result.leftToken == parentLeftToken)
                        result.leftToken = subTree.leftToken;

                    if(subTree.rightToken != null) {
                        result.rightToken = subTree.rightToken;
                        pivotToken = subTree.rightToken;
                    }
                }

                pivotPosition = subTree.endPosition;
            }
        }

        result.endPosition = pivotPosition;

        return result;
    }

    protected Tree createContextFreeTerm(IProduction production, List<Tree> childASTs, IToken leftToken,
        IToken rightToken) {
        String constructor = production.constructor();

        if(production.isList())
            return treeFactory.createList(production.sort(), childASTs, leftToken, rightToken);
        else if(production.isOptional())
            return treeFactory.createOptional(production.sort(), childASTs, leftToken, rightToken);
        else if(constructor != null)
            return treeFactory.createNonTerminal(production.sort(), constructor, childASTs, leftToken, rightToken);
        else if(childASTs.size() == 1)
            return childASTs.get(0);
        else
            return treeFactory.createTuple(production.sort(), childASTs, leftToken, rightToken);
    }

    protected Tree createLexicalTerm(IProduction production, String lexicalString, IToken lexicalToken) {
        Tree lexicalTerm = treeFactory.createStringTerminal(production.sort(), lexicalString, lexicalToken);

        if(lexicalToken != null) // Can be null, e.g. for empty string lexicals
            tokenTreeBinding(lexicalToken, lexicalTerm);

        return lexicalTerm;
    }

    protected abstract void tokenTreeBinding(IToken token, Tree tree);

}
