package org.spoofax.jsglr.shared.terms;

import java.util.Iterator;

public class ATermList extends ATerm implements Iterable<ATerm> {

	private final ATerm[] elements;

	ATermList(ATermFactory factory) {
		super(factory);
		elements = new ATerm[0];
	}

	ATermList(ATermFactory factory, ATerm... terms) {
		super(factory);
		elements = terms;
	}

	public ATermList prepend(ATerm t) {
		ATerm[] newList = new ATerm[elements.length+1];
		System.arraycopy(elements, 0, newList, 1, elements.length);
		newList[0] = t;
		return new ATermList(factory, newList);
	}

	public ATermList append(ATerm t) {
		ATerm[] newList = new ATerm[elements.length+1];
		System.arraycopy(elements, 0, newList, 0, elements.length);
		newList[elements.length] = t;
		return new ATermList(factory, newList);
	}

	public boolean isEmpty() {
		return elements.length == 0;
	}

	public ATerm getFirst() {
		return elements[0];
	}

	public ATermList getNext() {
		ATerm[] newList = new ATerm[elements.length-1];
		System.arraycopy(elements, 1, newList, 0, elements.length-1);
		return new ATermList(factory, newList);
	}

	@Override
	public ATerm getChildAt(int i) {
		return elements[i];
	}

	@Override
	public int getChildCount() {
		return elements.length;
	}

	@Override
	public int getType() {
		return ATerm.LIST;
	}

	private static class ATermListIterator implements Iterator<ATerm> {
		private final ATermList underlying;
		private int position;

		ATermListIterator(ATermList underlying) {
			this.underlying = underlying;
			position = 0;
		}

		@Override
		public boolean hasNext() {
			return position < underlying.elements.length;
		}

		@Override
		public ATerm next() {
			return underlying.elements[position++];
		}

		@Override
		public void remove() {
			throw new RuntimeException("Not supported");
		}
	}

	@Override
	public Iterator iterator() {
		return new ATermListIterator(this);
	}

}