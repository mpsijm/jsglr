package org.spoofax.jsglr2.parser;

import org.spoofax.jsglr2.parseforest.IParseForest;
import org.spoofax.jsglr2.parser.observing.ParserObserving;
import org.spoofax.jsglr2.stack.AbstractStackNode;
import org.spoofax.jsglr2.stack.collections.IActiveStacks;
import org.spoofax.jsglr2.stack.collections.IForActorStacks;

public class Parse
//@formatter:off
   <ParseForest extends IParseForest,
    StackNode   extends AbstractStackNode<ParseForest>>
//@formatter:on
    extends AbstractParse<ParseForest, StackNode> {

    public static <ParseForest_ extends IParseForest, StackNode_ extends AbstractStackNode<ParseForest_>>
        ParseFactory<ParseForest_, StackNode_, Parse<ParseForest_, StackNode_>> factory() {
        return Parse::new;
    }

    public Parse(String inputString, String filename, IActiveStacks<StackNode> activeStacks,
        IForActorStacks<StackNode> forActorStacks, ParserObserving<ParseForest, StackNode> observing) {
        super(inputString, filename, activeStacks, forActorStacks, observing);
    }
}
