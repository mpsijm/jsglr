package org.spoofax.jsglr2.imploder;

import javax.annotation.Nullable;

import org.apache.commons.vfs2.FileObject;

public interface ITokenizer<ImplodeResult> {

    TokenizeResult tokenize(String input, @Nullable FileObject resource, ImplodeResult tree);

}
