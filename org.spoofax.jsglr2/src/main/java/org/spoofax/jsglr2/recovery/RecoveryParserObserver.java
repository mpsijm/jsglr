package org.spoofax.jsglr2.recovery;

import java.util.Queue;

import org.metaborg.parsetable.actions.IAction;
import org.metaborg.parsetable.actions.IReduce;
import org.metaborg.parsetable.characterclasses.CharacterClassFactory;
import org.metaborg.parsetable.productions.IProduction;
import org.metaborg.parsetable.states.IState;
import org.spoofax.jsglr2.elkhound.AbstractElkhoundStackNode;
import org.spoofax.jsglr2.parseforest.IDerivation;
import org.spoofax.jsglr2.parseforest.IParseForest;
import org.spoofax.jsglr2.parseforest.IParseNode;
import org.spoofax.jsglr2.parser.AbstractParseState;
import org.spoofax.jsglr2.parser.ForShifterElement;
import org.spoofax.jsglr2.parser.observing.IParserObserver;
import org.spoofax.jsglr2.parser.observing.ParserObserving;
import org.spoofax.jsglr2.parser.result.ParseFailure;
import org.spoofax.jsglr2.parser.result.ParseSuccess;
import org.spoofax.jsglr2.stack.IStackNode;
import org.spoofax.jsglr2.stack.StackLink;
import org.spoofax.jsglr2.stack.collections.IForActorStacks;

public class RecoveryParserObserver
//@formatter:off
   <ParseForest extends IParseForest,
    Derivation  extends IDerivation<ParseForest>,
    ParseNode   extends IParseNode<ParseForest, Derivation>,
    StackNode   extends IStackNode,
    ParseState  extends AbstractParseState<?, StackNode> & IRecoveryParseState<?, StackNode, ?>>
//@formatter:on
    implements IParserObserver<ParseForest, Derivation, ParseNode, StackNode, ParseState> {

    @Override public void parseStart(ParseState parseState) {
    }

    @Override public void parseRound(ParseState parseState, Iterable<StackNode> activeStacks,
        ParserObserving<ParseForest, Derivation, ParseNode, StackNode, ParseState> observing) {
        // Record backtrack choice points per line. If in recovery mode, only record new choice points when parsing
        // after the point that initiated recovery.
        int currentOffset = parseState.inputStack.offset();
        if((currentOffset == 0 || CharacterClassFactory.isNewLine(parseState.inputStack.getChar(currentOffset - 1)))
            && (!parseState.isRecovering()
                || parseState.lastBacktrackChoicePoint().offset() < currentOffset)) {
            IBacktrackChoicePoint<StackNode> choicePoint = parseState.saveBacktrackChoicePoint();

            observing.notify(observer -> observer
                .recoveryBacktrackChoicePoint(parseState.backtrackChoicePoints().size() - 1, choicePoint));
        }

        if(parseState.successfulRecovery(currentOffset)) {
            parseState.endRecovery();

            observing.notify(observer -> observer.endRecovery(parseState));
        }
    }

    @Override public void addActiveStack(StackNode stack) {
    }

    @Override public void addForActorStack(StackNode stack) {
    }

    @Override public void findActiveStackWithState(IState state) {
    }

    @Override public void createStackNode(StackNode stack) {
    }

    @Override public void createStackLink(StackLink<ParseForest, StackNode> link) {
    }

    @Override public void resetDeterministicDepth(AbstractElkhoundStackNode<ParseForest> stack) {
    }

    @Override public void rejectStackLink(StackLink<ParseForest, StackNode> link) {
    }

    @Override public void forActorStacks(IForActorStacks<StackNode> forActorStacks) {
    }

    @Override public void handleForActorStack(StackNode stack, IForActorStacks<StackNode> forActorStacks) {
    }

    @Override public void actor(StackNode stack, ParseState parseState, Iterable<IAction> applicableActions) {
    }

    @Override public void skipRejectedStack(StackNode stack) {
    }

    @Override public void addForShifter(ForShifterElement<StackNode> forShifterElement) {
    }

    @Override public void doReductions(ParseState parseState, StackNode stack, IReduce reduce) {
    }

    @Override public void doLimitedReductions(ParseState parseState, StackNode stack, IReduce reduce,
        StackLink<ParseForest, StackNode> link) {
    }

    @Override public void reducer(StackNode stack, IReduce reduce, ParseForest[] parseNodes,
        StackNode activeStackWithGotoState) {
    }

    @Override public void reducerElkhound(StackNode stack, IReduce reduce, ParseForest[] parseNodes) {
    }

    @Override public void directLinkFound(ParseState parseState, StackLink<ParseForest, StackNode> directLink) {
    }

    @Override public void accept(StackNode acceptingStack) {
    }

    @Override public void createParseNode(ParseNode parseNode, IProduction production) {
    }

    @Override public void createDerivation(Derivation derivation, IProduction production, ParseForest[] parseNodes) {
    }

    @Override public void createCharacterNode(ParseForest characterNode, int character) {
    }

    @Override public void addDerivation(ParseNode parseNode, Derivation derivation) {
    }

    @Override public void shifter(ParseForest termNode, Queue<ForShifterElement<StackNode>> forShifter) {
    }

    @Override public void recoveryBacktrackChoicePoint(int index, IBacktrackChoicePoint<StackNode> choicePoint) {
    }

    @Override public void startRecovery(ParseState parseState) {
    }

    @Override public void recoveryIteration(ParseState parseState) {
    }

    @Override public void endRecovery(ParseState parseState) {
    }

    @Override public void remark(String remark) {
    }

    @Override public void success(ParseSuccess<ParseForest> success) {
    }

    @Override public void failure(ParseFailure<ParseForest> failure) {
    }

}
