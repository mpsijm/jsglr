package org.spoofax.jsglr.client;

/**
 * @author Lennart Kats <lennart add lclnet.nl>
 */
public interface IToken {
	/** Unknown token kind. */
	public static final int TK_UNKNOWN = 0;
	
	/** Token kind for a generic identifier. */
	public static final int TK_IDENTIFIER = 1;
	
	/** Token kind for a generic numeric value. */
	public static final int TK_NUMBER = 2;
	
	/** Token kind for a generic string literal. */
	public static final int TK_STRING = 3;
	
	/** Token kind for a generic keyword token. */
	public static final int TK_KEYWORD = 4;
	
	/** Token kind for a generic keyword token. */
	public static final int TK_OPERATOR = 5;
	
	/** Token kind for a meta-variable. */
	public static final int TK_VAR = 6;
	
	/** Token kind for a layout (or comment) token. */
	public static final int TK_LAYOUT = 7;
	
	/** Token kind for an EOF token. */
	public static final int TK_EOF = 8;
	
	/** Token kind for an erroneous token. */
	public static final int TK_ERROR = 9;
	
	public static final int TK_RESERVED = 10;
	
	int getKind();
	
	int getIndex();

	int getStartOffset();

	int getEndOffset();

	int getLine();
	
	int getColumn();
}