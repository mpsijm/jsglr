package org.spoofax.jsglr.client.editregion.detection;

import java.util.ArrayList;
import java.util.Collections;

import org.spoofax.interpreter.terms.IStrategoTerm;

/**
 * Fragment in the source code that represents a (possibly broken) construct
 * that (presumably) can be discarded safely, e.g., without invalidating the parse input.
 * @author maartje
 */
public class DiscardableRegion{
	private final int startOffset;
	private final int endOffset;
	private final IStrategoTerm affectedTerm;
	

	/**
	 * Fragment in the source code that represents a (possibly broken) construct
	 * that can be discarded safely, e.g., without invalidating the parse input.
	 */
	public DiscardableRegion(int startOffset, int endOffset, IStrategoTerm affectedTerm){
		this.startOffset = startOffset;
		this.endOffset = endOffset;
		this.affectedTerm = affectedTerm;
		assert startOffset <= endOffset;
	}

	/**
	 * Returns the start offset of the discardable code fragment
	 * @return start offset
	 */
	public int getStartOffset() {
		return startOffset;
	}

	/**
	 * Returns the end offset of the discardable code fragment
	 * @return end offset
	 */
	public int getEndOffset() {
		return endOffset;
	}

	/**
	 * Returns the term associated to the discardable code fragment.
	 * Can be null in case the discarded region is a comment, or in case the abstract representation is not known.
	 * @return term
	 */
	public IStrategoTerm getAffectedTerm() {
		assert(!affectedTerm.isList()): "Individual list elements are prefered over full lists";
		return affectedTerm;
	}
	
	public static String replaceAllRegionsByWhitespace(ArrayList<DiscardableRegion> regions, String input) {
		String result = input;
		for (DiscardableRegion region : regions) {
			result = replaceRegionByWhitespace(region, result);
		}
		return result;
	}
	
	private static String replaceRegionByWhitespace(DiscardableRegion region, String input) {
		char[] chars = input.toCharArray();
		for (int offset = region.getStartOffset(); offset <= region.getEndOffset(); offset++) {
			char charAtOffset = chars[offset];
			if(!Character.isWhitespace(charAtOffset)){
				chars[offset] = ' ';
			}
		}
		return String.valueOf(chars);
	}
	
	public static ArrayList<DiscardableRegion> constructRegionsFromOffsets(ArrayList<Integer> offsets){
		ArrayList<DiscardableRegion> result = new ArrayList<DiscardableRegion>();
		int startOffset = -1;
		for (int i = 0; i < offsets.size(); i++) {
			int offset = offsets.get(i);
			if(i==0){
				startOffset = offsets.get(i);
			}
			else if (offset != offsets.get(i-1) + 1){
				DiscardableRegion region = new DiscardableRegion(startOffset, offsets.get(i-1), null);
				result.add(region);
				startOffset = offsets.get(i);
			}
		}
		if(startOffset != -1){
			DiscardableRegion region = new DiscardableRegion(startOffset, offsets.get(offsets.size()-1), null);
			result.add(region);
		}
		return result;		
	}
	
	public static ArrayList<Integer> getEditOffsets(ArrayList<DiscardableRegion> editRegions) {
		ArrayList<Integer> result = new ArrayList<Integer>();
		for (DiscardableRegion region : editRegions) {
			int startOffset = region.getStartOffset();
			int endOffset = region.getEndOffset();
			for (int offset = startOffset; offset <= endOffset; offset++) {
				// "regions can overlap, for example: term + separation and comment in separation";
				if(!result.contains(offset))
					result.add(offset);
			}
		}
		Collections.sort(result);
		return result;
	}

}
