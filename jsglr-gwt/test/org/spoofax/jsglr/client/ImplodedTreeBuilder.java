package org.spoofax.jsglr.client;

import static java.lang.Math.max;

import java.util.ArrayList;
import java.util.List;

import org.spoofax.jsglr.shared.terms.ATermAppl;
import org.spoofax.jsglr.shared.terms.ATermFactory;

/**
 * @author Lennart Kats <lennart add lclnet.nl>
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ImplodedTreeBuilder extends TopdownTreeBuilder {
	
	public static final char SKIPPED_CHAR = (char) -1; // TODO: sync with ParseErorHandler
	
	public static final char UNEXPECTED_EOF_CHAR = (char) -2; // TODO: sync with ParseErorHandler
	
	private static final int NONE = -1;

	private static final int EXPECTED_NODE_CHILDREN = 5;
	
	private static final String LIST_CONSTRUCTOR = new String("[]");
	
	private static final String TUPLE_CONSTRUCTOR = new String("");
	
	private final ITokenizer tokenizer;
	
	private IImplodedTreeFactory factory;
	
	private boolean useDefaultFactory;
	
	private ProductionAttributeReader prodReader;

	private ATermFactory termFactory;
	
	private LabelInfo[] labels;
	
	private int labelStart;
	
	/** Character offset for the current implosion. */ 
	private int offset;
	
	private int nonMatchingOffset = NONE;
	
	private char nonMatchingChar, nonMatchingCharExpected, prevChar;
	
	private boolean inLexicalContext;
	
	public ImplodedTreeBuilder() {
		this(null, new Tokenizer());
		this.useDefaultFactory = true;
	}
	
	public ImplodedTreeBuilder(IImplodedTreeFactory treeFactory, ITokenizer tokenizer) {
		this.factory = treeFactory;
		this.tokenizer = tokenizer;
	}

	public void initialize(ParseTable table, int productionCount, int labelStart, int labelCount) {
		this.termFactory = table.getFactory();
		if (useDefaultFactory)
			factory = new ATermImplodedTreeFactory(termFactory);
		this.prodReader = new ProductionAttributeReader(termFactory);
		this.labels = new LabelInfo[labelCount - labelStart];
		this.labelStart = labelStart;
	}

	public void initializeLabel(int labelNumber, ATermAppl parseTreeProduction) {
		labels[labelNumber - labelStart] = new LabelInfo(prodReader, parseTreeProduction);
	}
	
	public ITokenizer getTokenizer() {
		return tokenizer;
	}
	
	/*
	public void visitLabel(int labelNumber) {
		LabelInfo label = labels[labelNumber - labelStart];
		if (label.isLexical() || label.isVar())
			lexicalContextDepth++;
	}
	
	public void endVisitLabel(int labelNumber) {
		LabelInfo label = labels[labelNumber - labelStart];
		if (label.isLexical() || label.isVar())
			lexicalContextDepth--;
	}
	*/
	
	@Override
	protected Object buildTreeNode(ParseNode node) {
		LabelInfo label = labels[node.label - labelStart];
		IToken prevToken = tokenizer.currentToken();
		int lastOffset = offset;
		AbstractParseNode[] subnodes = node.getChildren();
		
		boolean lexicalStart = !inLexicalContext && label.isLexicalLiteralOrLayout();
		if (lexicalStart) inLexicalContext = true;
		
		if (!inLexicalContext
				&& subnodes.length > 0 && subnodes[0] instanceof ParseProductionNode
				&& label.isSortProduction()
				&& label.getLHS().getChildCount() == 1) {
			return createIntTerminal(label, subnodes);
		}
		
		boolean isVar  = !inLexicalContext && label.isVar();
		if (isVar) inLexicalContext = true;
		
		// TODO: Optimize - one particularly gnarly optimization would be to reuse the subnodes array here
		//                  and in buildTreeAmb
		ArrayList<Object> children = null;
		if (!inLexicalContext)
			children = new ArrayList<Object>(max(EXPECTED_NODE_CHILDREN, subnodes.length));

		// Recurse
		for (AbstractParseNode subnode : subnodes) {
			Object child = subnode.toTreeTopdown(this);
			if (child != null) children.add(child);
		}
		
		if (lexicalStart || isVar) {
			return createStringTerminal(label);
		} else if (inLexicalContext) {
			tokenizer.createLayoutToken(offset, lastOffset, label);
			return null; // don't create tokens inside lexical context; just create one big token at the top
		} else {
			return createNodeOrInjection(label, prevToken, children);
		}
	}

	public Object buildTreeAmb(Amb a) {
		final int oldOffset = offset;
		final int oldBeginOffset = tokenizer.getStartOffset();
		final boolean oldLexicalContext = inLexicalContext;
		final AbstractParseNode[] subnodes = a.getAlternatives();
		final ArrayList<Object> children =
			new ArrayList<Object>(max(EXPECTED_NODE_CHILDREN, subnodes.length));

		// Recurse
		for (AbstractParseNode subnode : subnodes) {
			// Restore lexical state for each branch
			offset = oldOffset;
			tokenizer.setStartOffset(oldBeginOffset);
			inLexicalContext = oldLexicalContext;
			
			Object child = subnode.toTreeTopdown(this);
			if (child != null) children.add(child);
		}
		return factory.createAmb(children);
	}

	public Object buildTreeProduction(ParseProductionNode node) {
		int character = node.prod;
		consumeLexicalChar(character);
		return null;
	}


	private Object createStringTerminal(LabelInfo label) {
		inLexicalContext = false;
		String sort = label.getSort();
		IToken token = tokenizer.makeToken(offset, label, sort != null);
		
		if (sort == null) return null;
		
		// Debug.log("Creating node ", sort, " from ", SGLRTokenizer.dumpToString(token));
		
		Object result = factory.createStringTerminal(sort, getPaddedLexicalValue(label, token), token);
		String constructor = label.getMetaVarConstructor();
		if (constructor != null) {
			ArrayList<Object> children = new ArrayList<Object>(1);
			children.add(result);
			result = factory.createNonTerminal(sort, constructor, token, token, children);
		}
		return result;
	}
	
	private Object createIntTerminal(LabelInfo label, AbstractParseNode[] contents) {
		IToken token = tokenizer.makeToken(offset, label, true);
		int value = contents.length == 1 && contents[0] instanceof ParseProductionNode
			? ((ParseProductionNode) contents[0]).prod : -1;
		assert value != -1;
		return factory.createIntTerminal(label.getSort(), token, value);
	}

	private Object createNodeOrInjection(LabelInfo label, IToken prevToken, List<Object> children) {
		
		String constructor = label.getConstructor();
		
		if (label.isList()) {
			return createNode(label, LIST_CONSTRUCTOR, prevToken, children);
		} else if (constructor != null) {
			tokenizer.makeToken(offset, label); // TODO: why makeToken here??
			return createNode(label, constructor, prevToken, children);
		} else if (label.getAstAttribute() != null) {
			return createAstNonTerminal(label, prevToken, children);
		} else if (label.isOptional()) {
			// TODO: Spoofax/295: JSGLR does not output correct AST for optional literals
			if (children.size() == 0) {
				return createNode(label, "None", prevToken, children);
			} else {
				assert children.size() == 1;
				return createNode(label, "Some", prevToken, children);
			}
		} else if (children.size() == 1) {
			// Injection
			// TODO: efficiently store injection sort for use by content completion?
			//       would be needed to distinguish FoldingRules and Sorts in "folding" sections...
			//       maybe only if the content proposer demands it?
			// TODO: also, avoid semantics for deprecated?
			return factory.createInjection(label.getSort(), children);
		} else {
			// Constructor-less application (tuple)
			return createNode(label, TUPLE_CONSTRUCTOR, prevToken, children);
		}
	}

	/**
	 * Create a context-free tree node.
	 * 
	 * @param constructor
	 *          The constructor to use, or {@link #LIST_CONSTRUCTOR} to construct a list,
	 *          or {@link #TUPLE_CONSTRUCTOR} to construct a tuple.
	 */
	private Object createNode(LabelInfo label, String constructor, IToken prevToken,
			List<Object> children) {
		
		IToken left = getStartToken(prevToken);
		IToken right = getEndToken(left, tokenizer.currentToken());
		
		if (constructor == LIST_CONSTRUCTOR) {
			return factory.createList(label.getSort(), left, right, children);
		} else if (constructor == TUPLE_CONSTRUCTOR) {
			return factory.createTuple(label.getSort(), left, right, children);
		} else if (constructor == null && children.size() == 1 && factory.isStringTerminal(children.get(0))) {
			// Child node was a <string> node (rare case); unpack it and create a new terminal
			assert left == right;
			return factory.createStringTerminal(label.getSort(), getPaddedLexicalValue(label, left), left);
		} else {
			return factory.createNonTerminal(label.getSort(), constructor, left, right, children);
		}
	}
	
	/**
	 * Gets the padded lexical value for {indentpadding} lexicals, or returns null.
	 */
	private String getPaddedLexicalValue(LabelInfo label, IToken startToken) {
		if (label.isIndentPaddingLexical()) {
			char[] inputChars = tokenizer.getInputChars();
			int lineStart = startToken.getStartOffset() - 1;
			if (lineStart < 0) return null;
			while (lineStart >= 0) {
				char c = inputChars[lineStart--];
				if (c == '\n' || c == '\r') {
					lineStart++;
					break;
				}
			}
			StringBuilder result = new StringBuilder();
			result.append(inputChars, lineStart, startToken.getStartOffset() - lineStart - 1);
			for (int i = 0; i < result.length(); i++) {
				char c = result.charAt(i);
				if (c != ' ' && c != '\t') result.setCharAt(i, ' ');
			}
			result.append(startToken.toString());
			return result.toString();
		} else {
			return null; // lazily load token string value
		}
	}

	/** Implode a context-free node with an {ast} annotation. */
	private Object createAstNonTerminal(LabelInfo label, IToken prevToken, List<Object> children) {
		IToken left = getStartToken(prevToken);
		IToken right = getEndToken(left, tokenizer.currentToken());
		AstAnnoImploder imploder = new AstAnnoImploder<Object>(factory, termFactory, children, left, right);
		return imploder.implode(label.getAstAttribute(), label.getSort());
	}
	
	/** Get the token after the previous node's ending token, or null if N/A. */
	private IToken getStartToken(IToken prevToken) {
		if (prevToken == null) {
			return tokenizer.getTokenCount() == 0
				? null
			    : tokenizer.getTokenAt(0);
		} else {
			int index = prevToken.getIndex();
			
			if (tokenizer.getTokenCount() - index <= 1) {
				// Create new empty token
				// HACK: Assume TK_LAYOUT kind for empty tokens in AST nodes
				return tokenizer.makeToken(offset, IToken.TK_LAYOUT, true);
			} else {
				return tokenizer.getTokenAt(index + 1); 
			}
		}
	}
	
	/** Get the last no-layout token for an AST node. */
	private IToken getEndToken(IToken startToken, IToken lastToken) {
		int begin = startToken.getIndex();
		
		for (int i = lastToken.getIndex(); i > begin; i--) {
			lastToken = tokenizer.getTokenAt(i);
			if (lastToken.getKind() != IToken.TK_LAYOUT
					|| lastToken.getStartOffset() == lastToken.getEndOffset()-1)
				break;
		}
		
		return lastToken;
	}
	
	/** Consume a character of a lexical terminal. */
	protected final void consumeLexicalChar(int character) {
		char[] inputChars = tokenizer.getInputChars();
		if (offset >= inputChars.length) {
			if (nonMatchingOffset != NONE) {
				assert false : "Character in parse tree after end of input stream: "
						+ (char) character
						+ " - may be caused by unexcepted character in parse tree at position "
						+ nonMatchingChar 	+ ": " + nonMatchingChar + " instead of "
						+ nonMatchingCharExpected;
			}
		    // UNDONE: Strict lexical stream checking
			// throw new ImploderException("Character in parse tree after end of input stream: " + (char) character.getInt());
			// a forced reduction may have added some extra characters to the tree;
			inputChars[inputChars.length - 1] = UNEXPECTED_EOF_CHAR;
			return;
		}
		
		char parsedChar = (char) character;
		char inputChar = inputChars[offset];
		
		if (parsedChar != inputChar) {
			if (RecoveryConnector.isLayoutCharacter(parsedChar)) {
				// Remember that the parser skipped the current character
				// for later error reporting. (Cannot modify the immutable
				// parse tree here; changing the original stream instead.)
				inputChars[offset] = SKIPPED_CHAR;
				tokenizer.createSkippedToken(offset, inputChar, prevChar);
				offset++;
			} else {
				// UNDONE: Strict lexical stream checking
				// throw new IllegalStateException("Character from asfix stream (" + parsedChar
				//	 	+ ") must be in lex stream (" + inputChar + ")");
			    // instead, we allow the non-matching character for now, and hope
			    // we can pick up the right track later
				// TODO: better way to report skipped fragments in the parser
				//       this isn't 100% reliable
				if (nonMatchingOffset == NONE) {
					nonMatchingOffset = offset;
					nonMatchingChar = parsedChar;
					nonMatchingCharExpected = inputChar;
				}
				inputChars[offset] = SKIPPED_CHAR;
			}
		} else {
			offset++;
		}
		prevChar = inputChar;
	}

}