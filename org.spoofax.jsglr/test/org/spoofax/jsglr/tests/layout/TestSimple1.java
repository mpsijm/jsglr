/*
 * Created on 05.des.2005
 *
 * Copyright (c) 2005, Karl Trygve Kalleberg <karltk near strategoxt.org>
 *
 * Licensed under the GNU Lesser General Public License, v2.1
 */
package org.spoofax.jsglr.tests.layout;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.spoofax.jsglr.client.InvalidParseTableException;
import org.spoofax.jsglr.client.ParserException;

public class TestSimple1 extends AbstractParseTestCase {

    @Override
	public void gwtSetUp() throws FileNotFoundException, IOException,
            ParserException, InvalidParseTableException {
        super.gwtSetUp("Simple1", "test-offside/terms/", "txt");
    }

    public void testSimple1_1() throws FileNotFoundException, IOException {
        doParseTest("Simple1_1");
    }
    
    public void testSimple1_2() throws FileNotFoundException, IOException {
      doParseTest("Simple1_2");
    }
    
    public void testSimple1_3() throws FileNotFoundException, IOException {
      doParseTest("Simple1_3");
    }
    
    public void testSimple1_4() throws FileNotFoundException, IOException {
      doParseTest("Simple1_4");
    }
}