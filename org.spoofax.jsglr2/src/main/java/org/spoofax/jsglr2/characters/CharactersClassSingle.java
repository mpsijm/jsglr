package org.spoofax.jsglr2.characters;

public final class CharactersClassSingle implements ICharacters {

    private final int containsCharacter;

    public CharactersClassSingle(int containsCharacter) {
        this.containsCharacter = containsCharacter;
    }

    public final boolean containsCharacter(int character) {
        return containsCharacter == character;
    }

    public final <C extends Number & Comparable<C>> CharacterClassRangeSet<C>
        rangeSetUnion(CharacterClassRangeSet<C> rangeSet) {
        return rangeSet.addSingle(containsCharacter);
    }

    @Override public final String toString() {
        return "{" + ICharacters.intToString(containsCharacter) + "}";
    }

}
