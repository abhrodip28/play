package play.template2;


import play.libs.IO;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GTCompiler {

    private int nextMethodIndex = 0;

    private GTInternalTagsCompiler gtInternalTagsCompiler = new GTInternalTagsCompiler();

    public static class SourceContext {
        public final File file;
        // generated java code
        // generated groovy code
        public StringBuilder out = new StringBuilder();
        public StringBuilder gout = new StringBuilder();
        public String[] lines;
        public int currentLine;
        public int lineOffset;

        public SourceContext(File file) {
            this.file = file;
        }
    }

    public static class Output {
        public final String javaCode;
        public final String groovyCode;

        public Output(String javaCode, String groovyCode) {
            this.javaCode = javaCode;
            this.groovyCode = groovyCode;
        }

        @Override
        public String toString() {
            return "Output[->\n" +
                    "javaCode=\n" + javaCode + '\n' +
                    "--------------\n" +
                    "groovyCode=\n" + groovyCode + '\n' +
                    "<-]";
        }
    }

    public Output compile(File file) {

        String src = IO.readContentAsString( file );
        String[] lines = src.split("\\n");

        SourceContext sc = new SourceContext(file);
        sc.lines = lines;

        GTFragment fragment = null;

        List<GTFragment> rootFragments = new ArrayList<GTFragment>();

        String templateClassName = generateTemplateClassname( file );
        String templateClassNameGroovy = templateClassName + "G";

        StringBuilder gout = sc.gout;

        // generate groovy class
        gout.append("class " + templateClassNameGroovy + " extends play.template2.GTGroovyBase {\n");

        StringBuilder out = sc.out;

        // generate java class
        out.append("package play.template2.generated_templates\n");

        out.append("import java.util.*;\n");

        out.append("public class " + templateClassName + " extends play.template2.GTJavaBase {\n");

        // add constructor which initializes the templateClassNameGroovy-instance
        out.append(" public "+templateClassName+"(Map<String,Object> args) {\n");
        out.append("  super("+templateClassNameGroovy+".class, args);\n");
        out.append(" }\n");

        while ( (fragment = processNextFragment(sc)) != null ) {
            rootFragments.add( fragment );
        }

        GTFragmentMethodCall mainContentMethod = generateTagCode( "main", null, sc, rootFragments);

        // end of java class
        out.append( "public void main() {\n");
        out.append( "  "+mainContentMethod.methodName+"();\n" );
        out.append("}\n");
        out.append("}\n");

        // end of groovy class
        gout.append("}\n");

        return new Output(out.toString(), gout.toString());
    }

    private String generateTemplateClassname(File file) {
        return "Template2_"+file.getAbsolutePath().replaceAll(":", "D_").replaceAll("/", "_").replaceAll("\\\\","_").replaceAll("\\.", "_").replaceAll("-", "_");
    }

    public static class GTFragment {

    }

    public static class GTFragmentMethodCall extends GTFragment{
        public final String methodName;

        public GTFragmentMethodCall(String methodName) {
            this.methodName = methodName;
        }
    }

    public static class GTFragmentCode extends GTFragment {
        public final String code;

        public GTFragmentCode(String code) {
            this.code = code;
        }
    }

    public static class GTFragmentEndOfMultiLineTag extends GTFragment {
        public final String tagName;

        public GTFragmentEndOfMultiLineTag(String tagName) {
            this.tagName = tagName;
        }
    }

    protected GTFragment processNextFragment( SourceContext sc) {
        // find next something..

        final Pattern tagP = Pattern.compile("#\\{(/)?([\\d\\w_\\.-]+)(?:\\s+(.+)|\\s*)(/)?\\}");

        int startLine = sc.currentLine;
        int startOffset = sc.lineOffset;

        while ( sc.currentLine < sc.lines.length) {
            Matcher m = tagP.matcher( sc.lines[sc.currentLine]);

            if (m.find(sc.lineOffset)) {

                boolean endedTag = m.group(1)!=null;
                String tagName = m.group(2);
                String tagArgString = m.group(3);
                boolean tagWithoutBody = m.group(4)!=null;

                GTFragment plainText = checkForPlainText(sc, startLine, startOffset, m.start());
                if ( plainText != null) {
                    return plainText;
                }

                sc.lineOffset = m.end();

                if ( endedTag ) {
                    return new GTFragmentEndOfMultiLineTag(tagName);
                }

                return processTag(sc, tagName, tagArgString, tagWithoutBody);

            } else {
                sc.currentLine++;
                sc.lineOffset = 0;
            }

        }

        GTFragment plainText = checkForPlainText(sc, startLine, startOffset, -1);
        return plainText;
    }

    private GTFragment checkForPlainText(SourceContext sc, int startLine, int startOffset, int endOfLastLine) {
        if (sc.currentLine == startLine && sc.lineOffset == startOffset) {
            return null;
        }

        StringBuilder sb = new StringBuilder();

        int line = startLine;
        while ( line <= sc.currentLine && line < sc.lines.length) {
            if (line == startLine) {
                if ( startLine ==  sc.currentLine) {
                    sb.append(sc.lines[line].substring(startOffset, endOfLastLine));
                    // done
                    break;
                } else {
                    sb.append(sc.lines[line].substring(startOffset));
                }
            } else if ( line < sc.currentLine) {
                sb.append("\n");
                sb.append(sc.lines[line]);
            } else {
                sb.append("\n");
                if ( endOfLastLine == -1) {
                    sb.append(sc.lines[line]);
                } else {
                    sb.append(sc.lines[line].substring(0, endOfLastLine));
                }
            }
            line++;
        }

        String oneLiner = sb.toString().replace("\\", "\\\\").replaceAll("\"", "\\\\\"").replaceAll("\r\n", "\n").replaceAll("\n", "\\\\n");

        if ( oneLiner.length() > 0) {
            return new GTFragmentCode("out.append(\""+oneLiner+"\");");
        } else {
            return new GTFragmentCode("");
        }
    }

    protected GTFragment processTag( SourceContext sc, String tagName, String tagArgString, boolean tagWithoutBody) {

        final List<GTFragment> body = new ArrayList<GTFragment>();

        if ( tagWithoutBody) {
            return generateTagCode(tagName, tagArgString, sc, body);
        }

        int startLine = sc.currentLine;

        GTFragment nextFragment = null;
        while ( (nextFragment = processNextFragment( sc )) != null ) {
            if ( nextFragment instanceof GTFragmentEndOfMultiLineTag) {
                GTFragmentEndOfMultiLineTag f = (GTFragmentEndOfMultiLineTag)nextFragment;
                if (f.tagName.equals(tagName)) {
                    return generateTagCode(tagName, tagArgString, sc, body);
                } else {
                    throw new GTCompilerException("Found unclosed tag '"+tagName+"' used at line "+(startLine+1), sc.file, startLine+1);
                }
            } else {
                body.add(nextFragment);
            }
        }

        throw new GTCompilerException("Found unclosed tag '"+tagName+"' used at line "+(startLine+1), sc.file, startLine+1);
    }

    // Generates a method in the templates groovy-class which, when called, returns the args-map.
    // returns the java code needed to execute and return the data
    private String generateGroovyCodeForTagArgs(SourceContext sc, String tagName, String tagArgString) {

        if ( tagArgString == null || tagArgString.trim().length() == 0) {
            // just generate code that creates empty map
            return " Map tagArgs = new HashMap();\n";
        }

        if (!tagArgString.matches("^[_a-zA-Z0-9]+\\s*:.*$")) {
            tagArgString = "arg:" + tagArgString;
        }
        StringBuilder gout = sc.gout;
        String methodName = "args_"+tagName;
        gout.append("Map<String, Object> "+methodName+"() {\n");
        gout.append(" return ["+tagArgString+"];\n");
        gout.append( "}\n");

        // must return the javacode needed to get the data
        return " Map tagArgs = (Map)invokeGroovy(\""+methodName+"\");\n";
    }

    private String generateMethodName(String hint) {
        return "m_" + hint + "_" + (nextMethodIndex++);
    }



    private GTFragmentMethodCall generateTagCode(String tagName, String tagArgString, SourceContext sc, List<GTFragment> body) {

        // generate groovy code for tag-args
        String javaCodeToGetRefToArgs = generateGroovyCodeForTagArgs( sc, tagName, tagArgString);


        String methodName = generateMethodName(tagName);
        String contentMethodName = methodName+"_content";

        // generate method that runs the content..
        generateCodeForGTFragments( sc, body, contentMethodName);


        StringBuilder out = sc.out;
        out.append("public void "+methodName+"() {\n");

        // add tag args code
        out.append(javaCodeToGetRefToArgs);

        if ( !gtInternalTagsCompiler.generateCodeForGTFragments(tagName, contentMethodName, sc)) {
            // Tag was not an internal tag - must resolve it diferently
            //throw new GTCompilerException("Cannot find tag-implementation for '"+tagName+"' used on line "+(sc.currentLine+1), sc.file, sc.currentLine+1);
            out.append("//TODO: Missing tag impl.\n");
        }

        out.append("}\n");

        return new GTFragmentMethodCall(methodName);
    }

    private void generateCodeForGTFragments(SourceContext sc, List<GTFragment> body, String methodName) {

        StringBuilder out = sc.out;

        out.append("public void "+methodName+"() {\n");
        for ( GTFragment f : body) {
            if (f instanceof GTFragmentMethodCall) {
                GTFragmentMethodCall m = (GTFragmentMethodCall)f;
                out.append("  " + m.methodName + "();\n");
            } else if (f instanceof GTFragmentCode) {
                GTFragmentCode c = (GTFragmentCode)f;
                if ( c.code.length() > 0) {
                    out.append("  " + c.code + "\n");
                }
            } else {
                throw new GTCompilerException("Unknown GTFragment-type", sc.file, sc.currentLine);
            }
        }
        out.append("}\n");

    }


}
