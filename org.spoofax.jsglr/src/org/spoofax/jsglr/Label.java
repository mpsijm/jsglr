/*
 * Created on 06.des.2005
 *
 * Copyright (c) 2005, Karl Trygve Kalleberg <karltk@ii.uib.no>
 * 
 * Licensed under the GNU Lesser General Public License, v2.1
 */
package org.spoofax.jsglr;

import aterm.ATermAppl;

import java.io.Serializable;

public class Label implements Serializable {

    static final long serialVersionUID = -4080621639747161438L;
    
    public final int labelNumber;
    public final ATermAppl prod;
    public final ProductionAttributes productionAttributes;
    
    public Label(int labelNumber, ATermAppl prod, ProductionAttributes productionAttributes) {
        this.labelNumber = labelNumber;
        this.prod = prod;
        this.productionAttributes = productionAttributes;
    }

    public boolean isLeftAssociative() {
        return productionAttributes.type == ProductionAttributes.LEFT_ASSOCIATIVE;
    }

    public boolean isRightAssociative() {
        return productionAttributes.type == ProductionAttributes.RIGHT_ASSOCIATIVE;
    }

}
