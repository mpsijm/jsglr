package org.spoofax.jsglr.client.editregion.detection;

import java.util.ArrayList;

import org.spoofax.jsglr.client.editregion.detection.LCS;
import org.spoofax.jsglr.client.imploder.IToken;
import org.spoofax.jsglr.client.imploder.ITokenizer;
import org.spoofax.jsglr.client.imploder.Token;
import org.spoofax.jsglr.client.editregion.detection.DiscardableRegion;

/**
 * Determines the comment regions that are edited and therefore (possible) damaged.
 * Filters the offsets of the deleted characters, removing comment and whitespace offsets 
 * that are either irrelevant (not affecting the parse result) or allready processed (block comment damage)
 * Example damaged comment: '/* comment * /'
 * Example harmful layout deletion: 'int  i' => 'inti' 
 * @author Maartje de Jonge
 *
 */
public class LayoutEditsAnalyzer {
		
	private final DamagedTokenAnalyzer tokenEdits;

	//filled in the analysis
	private final ArrayList<DiscardableRegion> damagedCommentRegions;
	private final ArrayList<Integer> offsetsDeletedLayoutChars;
	
	/**
	 * Returns the comment regions that are edited and therefore (possible) damaged.
	 */
	public ArrayList<DiscardableRegion> getDamagedCommentRegions() {
		return damagedCommentRegions;
	}

	/**
	 * Filters the list of deletion-offsets,
	 * removing all layout-deletion-offsets that are in fact harmless, e.g. do not affect the parse result.
	 */
	public void filterNonLayoutOffsets(ArrayList<Integer> deletedOffsets) {
		deletedOffsets.removeAll(offsetsDeletedLayoutChars);
	}
	
	/**
	 * Determines the comment regions that are edited and therefore (possible) damaged.
	 * Filters the offsets of the deleted characters, removing comment and whitespace offsets 
	 * that are either irrelevant (not affecting the parse result) or allready processed (block comment damage)
	 */
	public LayoutEditsAnalyzer(ITokenizer tokens, LCS<Character> lcs){
		this.offsetsDeletedLayoutChars = new ArrayList<Integer>();
		this.damagedCommentRegions = new ArrayList<DiscardableRegion>();
		tokenEdits = new DamagedTokenAnalyzer(tokens, lcs);
		analyze();
	}
	
	private void analyze() {
		for (IToken tokenWithDeletions : tokenEdits.getTokensDamagedByDeletion()) {
			if(tokenWithDeletions.getKind() == Token.TK_LAYOUT){
				if(!tokenEdits.isDamagingLayoutDeletion(tokenWithDeletions)){
					this.offsetsDeletedLayoutChars.addAll(tokenEdits.getOffsetsDeletions(tokenWithDeletions));						
				}
				if(!Token.isWhiteSpace(tokenWithDeletions)){
					DiscardableRegion commentRegion = new DiscardableRegion(tokenWithDeletions.getStartOffset(), tokenWithDeletions.getEndOffset(), null);
					damagedCommentRegions.add(commentRegion);
					assert !HelperFunctions.contains(damagedCommentRegions, commentRegion);
				}
			}
		}
		for (IToken tokenWithInsertions : tokenEdits.getTokensDamagedByInsertion()) {
			if(tokenWithInsertions.getKind() == Token.TK_LAYOUT && !Token.isWhiteSpace(tokenWithInsertions)){
				DiscardableRegion commentRegion = new DiscardableRegion(tokenWithInsertions.getStartOffset(), tokenWithInsertions.getEndOffset(), null);
				if(!HelperFunctions.contains(damagedCommentRegions, commentRegion))
					damagedCommentRegions.add(commentRegion);
			}
		}
	}
}