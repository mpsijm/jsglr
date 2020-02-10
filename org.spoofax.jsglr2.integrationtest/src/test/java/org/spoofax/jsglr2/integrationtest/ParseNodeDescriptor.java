package org.spoofax.jsglr2.integrationtest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.annotation.Nullable;

import org.spoofax.jsglr2.parseforest.IParseNode;

public final class ParseNodeDescriptor {

    public final int offset, width;
    @Nullable public String sort, cons;

    public ParseNodeDescriptor(int offset, int width, @Nullable String sort, @Nullable String cons) {
        this.offset = offset;
        this.width = width;
        this.sort = sort;
        this.cons = cons;
    }

    public void assertSame(int offset, IParseNode<?, ?> node) {
        assertEquals(this.offset, offset, "Offsets do not match for " + this);
        assertEquals(this.width, width, "Width does not match for " + this);
        assertEquals(this.sort, node.production().lhs().descriptor(), "Sort does not match for " + this);
        assertEquals(this.cons, node.production().constructor(), "Cons does not match for " + this);
    }

    @Override public String toString() {
        return "ParseNodeDescriptor{" + "offset=" + offset + ", width=" + width + ", sort='" + sort + '\'' + ", cons='"
            + cons + '\'' + '}';
    }
}
