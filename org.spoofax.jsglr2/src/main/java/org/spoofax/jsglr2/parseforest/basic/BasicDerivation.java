package org.spoofax.jsglr2.parseforest.basic;

import org.metaborg.parsetable.IProduction;
import org.metaborg.parsetable.ProductionType;
import org.spoofax.jsglr2.parseforest.IDerivation;
import org.spoofax.jsglr2.parser.Position;

public class BasicDerivation extends BasicParseForest implements IDerivation<BasicParseForest> {

    public final IProduction production;
    public final ProductionType productionType;
    public final BasicParseForest[] parseForests;

    public BasicDerivation(Position startPosition, Position endPosition, IProduction production,
        ProductionType productionType, BasicParseForest[] parseForests) {
        super(startPosition, endPosition);
        this.production = production;
        this.productionType = productionType;
        this.parseForests = parseForests;
    }

    @Override public String descriptor() {
        return production.descriptor();
    }

    @Override public IProduction production() {
        return production;
    }

    @Override public ProductionType productionType() {
        return productionType;
    }

    @Override public BasicParseForest[] parseForests() {
        return parseForests;
    }

}