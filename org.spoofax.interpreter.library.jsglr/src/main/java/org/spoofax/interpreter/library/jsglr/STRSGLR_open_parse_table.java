/*
 * Copyright (c) 2005-2011, Karl Trygve Kalleberg <karltk near strategoxt dot org>
 *
 * Licensed under the GNU Lesser General Public License, v2.1
 */
package org.spoofax.interpreter.library.jsglr;

import java.util.Map;

import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.stratego.Strategy;
import org.spoofax.interpreter.terms.IStrategoInt;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.jsglr.client.InvalidParseTableException;
import org.spoofax.jsglr.client.ParseTable;

public class STRSGLR_open_parse_table extends JSGLRPrimitive {

	public STRSGLR_open_parse_table() {
		super("STRSGLR_open_parse_table", 0, 1);
	}

	@Override
	public boolean call(IContext env, Strategy[] svars, IStrategoTerm[] tvars)
			throws InterpreterException {

		Map<IStrategoTerm, IStrategoInt> cache = getLibrary(env).getParseTableCache();

		IStrategoInt cached = cache.get(tvars[0]);
	    if (cached != null) {
	        env.setCurrent(cached);
	        if(!cache.containsValue(cached.intValue()))
	        	throw new IllegalStateException("Inconsistent context: wrong JSGLR library instance");
	        return true;
	    }

	    IStrategoTerm tableTerm = tvars[0];

		JSGLRLibrary lib = getLibrary(env);
		try {
			ParseTable pt = lib.getParseTableManager(env.getFactory()).loadFromTerm(tableTerm);
			IStrategoInt result = env.getFactory().makeInt(lib.addParseTable(pt));

			env.setCurrent(result);
			cache.put(tvars[0], result);
		} catch (InvalidParseTableException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

}
