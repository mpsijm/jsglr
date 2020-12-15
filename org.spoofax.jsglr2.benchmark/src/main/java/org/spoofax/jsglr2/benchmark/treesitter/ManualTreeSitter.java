package org.spoofax.jsglr2.benchmark.treesitter;

import static com.github.mpsijm.javatreesitter.JavaTreeSitterLibrary.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.bridj.Pointer;
import org.metaborg.parsetable.ParseTableReadException;

import com.github.mpsijm.javatreesitter.TSInputEdit;
import com.github.mpsijm.javatreesitter.TSLanguage;
import com.github.mpsijm.javatreesitter.java.TreeSitterJavaLibrary;

public class ManualTreeSitter {
    public static void main(String[] args) throws IOException, ParseTableReadException {
        TreeSitterParser parser = new TreeSitterParser(TreeSitterJavaLibrary.tree_sitter_java());

        // @SuppressWarnings("rawtypes") IObservableParser parser =
        // ((JSGLR2Implementation) JSGLR2Variant.Preset.incremental
        // .getJSGLR2(ParseTableVariant.standard().parseTableReader().read(new TermReader(new TermFactory())
        // .parseFromFile("/home/maarten/git/thesis/java-front/lang.java/target/metaborg/sdf.tbl")))).parser;

        String input1 = new String(Files.readAllBytes(Paths.get(
            "/home/maarten/git/thesis/jsglr2evaluation/tmp/sources/java/incremental/apache-commons-lang-stringutils/0/src_main_java_org_apache_commons_lang3_StringUtils.java")));
        String input2 = "package derp;";

        long begin;
        begin = System.currentTimeMillis();

        Pointer<TSTree> previousTree = parser.parse(input1);
        // IParseForest previousTree = ((ParseSuccess) parser.parse(input1)).parseResult;

        System.out.println((System.currentTimeMillis() - begin) + " ms");
        // System.out.println(ts_tree_string(previousTree).getCString());
        System.out.println("----");

        for(int i = 1; i <= 50; i++) {
            StringBuilder sb = new StringBuilder();
            sb.append("package derp");
            for(int j = 0; j < i; j++) {
                sb.append(".derp");
            }
            sb.append(";");
            String input3 = sb.toString();

            begin = System.currentTimeMillis();

            Pointer<TSTree> newTree = parser.parse(input3, input1, previousTree);
            // ParseResult<IParseForest> newTree = parser.parse(input3, null, input1, previousTree);

            System.out.println("----");
            System.out.println(Runtime.getRuntime().freeMemory());
            System.out.println(Runtime.getRuntime().totalMemory());
            // System.out.println((System.currentTimeMillis() - begin) + " ms");
            // System.out.println(ts_tree_string(newTree).getCString());
        }
    }

    public static void mainFullManual(String[] args) throws IOException {
        Pointer<TSLanguage> javaLanguage = TreeSitterJavaLibrary.tree_sitter_java();

        Pointer<TSParser> parser = ts_parser_new();
        ts_parser_set_language(parser, javaLanguage);

        String input1 = new String(Files.readAllBytes(Paths.get(
            "/home/maarten/git/thesis/jsglr2evaluation/tmp/sources/java/incremental/apache-commons-lang-stringutils/0/src_main_java_org_apache_commons_lang3_StringUtils.java")));
        String input2 = "package derp;";

        long begin;
        begin = System.currentTimeMillis();

        Pointer<TSTree> previousTree =
            ts_parser_parse_string(parser, null, Pointer.pointerToCString(input2), input2.length());

        System.out.println((System.currentTimeMillis() - begin) + " ms");
        // System.out.println(ts_tree_string(previousTree).getCString());
        System.out.println("----");

        for(int i = 1; i <= 10; i++) {
            StringBuilder sb = new StringBuilder();
            sb.append("package derp");
            for(int j = 0; j < i; j++) {
                sb.append(".derp");
            }
            sb.append(";");
            String input3 = sb.toString();

            begin = System.currentTimeMillis();

            Pointer<TSInputEdit> edit = Pointer.allocate(TSInputEdit.class);
            // edit.get().start_byte(0).old_end_byte(input2.length()).new_end_byte(input3.length());
            edit.get().start_byte(8).old_end_byte(8).new_end_byte(8 + 5 * i);
            Pointer<TSTree> copyOfPrevious = ts_tree_copy(previousTree);
            ts_tree_edit(copyOfPrevious, edit);
            Pointer<TSTree> newTree =
                ts_parser_parse_string(parser, copyOfPrevious, Pointer.pointerToCString(input3), input3.length());

            System.out.println((System.currentTimeMillis() - begin) + " ms");
            // System.out.println(ts_tree_string(newTree).getCString());
        }
    }
}
