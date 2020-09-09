package org.spoofax.jsglr2.testset;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.spoofax.jsglr2.testset.testinput.IncrementalStringInput;

public class TestSetIncrementalInput extends TestSetInput<String[], IncrementalStringInput> {

    // Directory (if internal) in the org.spoofax.jsglr2.integration/src/main/resources/samples directory or (if not
    // internal) an absolute path, containing files %d.in
    public final String directory;
    private final int startVersion;

    public TestSetIncrementalInput(String directory, boolean internal, int startVersion) {
        super(Type.INCREMENTAL, internal);

        this.directory = directory;
        this.startVersion = startVersion;
    }

    public TestSetIncrementalInput(String directory, boolean internal) {
        this(directory, internal, 0);
    }

    @Override protected IncrementalStringInput getInput(String filename, String[] input) {
        return new IncrementalStringInput(filename, input);
    }

    @Override public List<IncrementalStringInput> getInputs() throws IOException {
        List<String> inputs = new ArrayList<>();
        for(int i = startVersion;; i++) {
            try {
                inputs.add(getFileAsString(
                    directory + (directory.endsWith(File.separator) ? "" : File.separator) + i + ".in"));
            } catch(NullPointerException | IOException ignored) {
                break;
            }
        }
        return Collections.singletonList(getInput(directory, inputs.toArray(new String[0])));
    }
}
