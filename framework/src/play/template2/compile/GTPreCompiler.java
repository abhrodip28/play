package play.template2.compile;

import play.template2.GTFastTagResolver;
import play.template2.legacy.GTLegacyFastTagResolver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GTPreCompiler {

    public static String generatedPackageName = "play.template2.generated_templates";

    private GTInternalTagsCompiler gtInternalTagsCompiler = new GTInternalTagsCompiler();

    private Map<String, String> expression2GroovyMethodLookup = new HashMap<String, String>();
    private Map<String, String> tagArgs2GroovyMethodLookup = new HashMap<String, String>();

    public GTFastTagResolver customFastTagResolver = null;

    public static GTLegacyFastTagResolver legacyFastTagResolver = null;

    private final String varName = "ev";

    public static class SourceContext {
        public final File file;
        // generated java code
        // generated groovy code
        public StringBuilder out = new StringBuilder();
        public StringBuilder gout = new StringBuilder();
        public String[] lines;
        public int currentLine;
        public int lineOffset;
        public int nextMethodIndex = 0;

        public SourceContext(File file) {
            this.file = file;
        }
    }

    public static class Output {
        public final String javaClassName;
        public final String javaCode;
        public final String groovyClassName;
        public final String groovyCode;

        public Output(String javaClassName, String javaCode, String groovyClassName, String groovyCode) {
            this.javaClassName = javaClassName;
            this.javaCode = javaCode;
            this.groovyClassName = groovyClassName;
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

    /**
     * Read file content to a String (always use utf-8)
     * @param file The file to read
     * @return The String content
     */
    private String readContentAsString(File file) {
        return readContentAsString(file, "utf-8");
    }

    /**
     * Read file content to a String
     * @param file The file to read
     * @return The String content
     */
    private String readContentAsString(File file, String encoding) {
        InputStream is = null;
        try {
            is = new FileInputStream(file);
            StringWriter result = new StringWriter();
            PrintWriter out = new PrintWriter(result);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, encoding));
            String line = null;
            while ((line = reader.readLine()) != null) {
                out.println(line);
            }
            return result.toString();
        } catch(IOException e) {
            throw new RuntimeException(e);
        } finally {
            if(is != null) {
                try {
                    is.close();
                } catch(Exception e) {
                    //
                }
            }
        }
    }


    public Output compile(File file) {
        String src = readContentAsString(file);
        return compile(src, file);
    }

    public Output compile(String src, File file) {


        String[] lines = src.split("\\n");

        SourceContext sc = new SourceContext(file);
        sc.lines = lines;

        GTFragment fragment = null;

        List<GTFragment> rootFragments = new ArrayList<GTFragment>();

        String templateClassName = generateTemplateClassname( file );
        String templateClassNameGroovy = templateClassName + "G";

        StringBuilder gout = sc.gout;

        // generate groovy class
        gout.append("package "+generatedPackageName+";\n");
        gout.append("class " + templateClassNameGroovy + " extends play.template2.GTGroovyBase {\n");

        StringBuilder out = sc.out;

        // generate java class
        out.append("package "+generatedPackageName+";\n");

        out.append("import java.util.*;\n");
        out.append("import java.io.*;\n");

        out.append("public class " + templateClassName + " extends play.template2.GTJavaBase {\n");

        out.append(" private "+templateClassNameGroovy+" g;\n");

        // add constructor which initializes the templateClassNameGroovy-instance
        out.append(" public "+templateClassName+"() {\n");
        out.append("  super("+templateClassNameGroovy+".class, \""+sc.file.getAbsolutePath()+"\");\n");
        out.append(" }\n");

        rootFragments.add( new GTFragmentCode("  this.g = ("+templateClassNameGroovy+")groovyScript;\n"));

        while ( (fragment = processNextFragment(sc)) != null ) {
            rootFragments.add( fragment );
        }

        generateCodeForGTFragments(sc, rootFragments, "_renderTemplate", "_renderTemplate");

        // end of java class
        out.append("}\n");

        // end of groovy class
        gout.append("}\n");

        return new Output( generatedPackageName+"."+templateClassName, out.toString(), generatedPackageName+"."+templateClassNameGroovy, gout.toString());
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

        // pattern that find any of the '#/$/& etc we're intercepting. it find the next one - so we know what to look for
        final Pattern partsP = Pattern.compile("([#\\$])\\{[^\\}]+\\}");

        // pattern that finds all kinds of tags
        final Pattern tagP = Pattern.compile("#\\{([^\\}]+)\\}");

        // pattern that finds a $ (value) with content/expression
        final Pattern valueP = Pattern.compile("\\$\\{([^\\}]+)\\}");

        int startLine = sc.currentLine;
        int startOffset = sc.lineOffset;

        while ( sc.currentLine < sc.lines.length) {

            String currentLine = sc.lines[sc.currentLine];

            Matcher m = partsP.matcher(currentLine);

            // do we have anything on this line?
            if ( m.find(sc.lineOffset)) {

                // yes we did find something

                // must check for plain text first..
                GTFragment plainText = checkForPlainText(sc, startLine, startOffset, m.start());
                if ( plainText != null) {
                    return plainText;
                }

                sc.lineOffset = m.end();

                // what did we find?
                int correctOffset = m.start();

                String type = m.group(1);

                if ("#".equals(type)) {
                    // we found a tag - go' get it

                    m = tagP.matcher( currentLine );

                    if (!m.find(correctOffset)) {
                        throw new RuntimeException("Where supposed to find the #tag here..");
                    }

                    String tagBody = m.group(1);
                    boolean endedTag = tagBody.startsWith("/");
                    if ( endedTag) {
                        tagBody = tagBody.substring(1);
                    }
                    boolean tagWithoutBody = tagBody.endsWith("/");
                    if ( tagWithoutBody) {
                        tagBody = tagBody.substring(0,tagBody.length()-1);
                    }
                    // split tag name and optional params

                    final Pattern tagBodyP = Pattern.compile("([^\\s]+)(?:$|\\s+(.+))");

                    m = tagBodyP.matcher(tagBody);
                    if (!m.find()) {
                        throw new RuntimeException("Not supposed to happen");
                    }
                    String tagName = m.group(1);
                    String tagArgString = m.group(2);
                    if (tagArgString == null) {
                        tagArgString = "";
                    }

                    if ( endedTag ) {
                        return new GTFragmentEndOfMultiLineTag(tagName);
                    }

                    return processTag(sc, tagName, tagArgString, tagWithoutBody);

                } else if ("$".equals(type)) {
                    m = valueP.matcher(currentLine);
                    if (!m.find(correctOffset)) {
                        throw new RuntimeException("Where supposed to find the $value here..");
                    }

                    String expression = m.group(1).trim();

                    return generateExpressionPrinter(expression, sc);

                }
                else {
                    throw new RuntimeException("Don't know how to handle type '"+type+"'");
                }
            } else {
                // skip to next line
                sc.currentLine++;
                sc.lineOffset = 0;
            }

        }

        GTFragment plainText = checkForPlainText(sc, startLine, startOffset, -1);
        return plainText;
    }

    private GTFragmentCode generateExpressionPrinter(String expression, SourceContext sc) {

        // check if we already have generated method for this expression
        String methodName = expression2GroovyMethodLookup.get(expression);

        if ( methodName == null ) {

            // generate the groovy method for retrieving the actual value

            StringBuilder gout = sc.gout;
            methodName = "expression_"+(sc.nextMethodIndex++);
            gout.append("Object "+methodName+"() {\n");
            gout.append(" return "+expression+";\n");
            gout.append( "}\n");
            
            expression2GroovyMethodLookup.put(expression, methodName);
        }

        // return the java-code for retrieving and pringing the expression

        String javaCode = varName+" = g."+methodName+"();\n" +
                "if ("+varName+"!=null) out.append( objectToString("+varName+"));\n";
        return new GTFragmentCode(javaCode);
    }

    private GTFragment checkForPlainText(SourceContext sc, int startLine, int startOffset, int endOfLastLine) {
        if (sc.currentLine == startLine && sc.lineOffset == startOffset && sc.lineOffset == endOfLastLine) {
            return null;
        }

        StringBuilder sb = new StringBuilder();

        int line = startLine;
        while ( line <= sc.currentLine && line < sc.lines.length) {
            if (line == startLine) {
                if ( startLine ==  sc.currentLine) {
                    sb.append(sc.lines[line].substring(startOffset, endOfLastLine));

                    // must advance sc-offset
                    sc.lineOffset = endOfLastLine;
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
            return null;
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

        tagArgString = tagArgString.trim();

        // have we generated method for these args before?
        String methodName = tagArgs2GroovyMethodLookup.get(tagArgString);

        if (methodName==null) {

            // first time - must generate it

            if (!tagArgString.matches("^[_a-zA-Z0-9]+\\s*:.*$")) {
                tagArgString = "arg:" + tagArgString;
            }
            StringBuilder gout = sc.gout;
            methodName = "args_"+tagName + "_"+(sc.nextMethodIndex++);
            gout.append("Map<String, Object> "+methodName+"() {\n");
            gout.append(" return ["+tagArgString+"];\n");
            gout.append( "}\n");

            tagArgs2GroovyMethodLookup.put(tagArgString, methodName);
        }

        // must return the javacode needed to get the data
        return " Map tagArgs = (Map)g."+methodName+"();\n";
    }

    private String generateMethodName(String hint, SourceContext sc) {
        return "m_" + hint + "_" + (sc.nextMethodIndex++);
    }



    private GTFragmentMethodCall generateTagCode(String tagName, String tagArgString, SourceContext sc, List<GTFragment> body) {

        // generate groovy code for tag-args
        String javaCodeToGetRefToArgs = generateGroovyCodeForTagArgs( sc, tagName, tagArgString);


        String methodName = generateMethodName(tagName, sc);
        String contentMethodName = methodName+"_content";

        // generate method that runs the content..
        generateCodeForGTFragments( sc, body, contentMethodName, tagName);


        StringBuilder out = sc.out;
        out.append("public void "+methodName+"() {\n");

        // add tag args code
        out.append(javaCodeToGetRefToArgs);

        if ( !gtInternalTagsCompiler.generateCodeForGTFragments(tagName, contentMethodName, sc)) {
            // Tag was not an internal tag - must resolve it diferently

            // check internal fastTags
            String fullnameToFastTagMethod = new GTInternalFastTags().resolveFastTag(tagName);
            if ( fullnameToFastTagMethod != null) {
                generateFastTagInvocation(sc, fullnameToFastTagMethod, contentMethodName);
            } else {

                // Check for custom fastTags
                if (customFastTagResolver !=null && (fullnameToFastTagMethod = customFastTagResolver.resolveFastTag(tagName))!=null) {
                    generateFastTagInvocation(sc, fullnameToFastTagMethod, contentMethodName);
                } else {

                    // look for lecacy fastTags
                    if ( legacyFastTagResolver != null && (fullnameToFastTagMethod = legacyFastTagResolver.resolveFastTag(tagName))!=null) {
                        generateLegacyFastTagInvocation(tagName, sc, fullnameToFastTagMethod, contentMethodName);
                    } else {
                        //throw new GTCompilerException("Cannot find tag-implementation for '"+tagName+"' used on line "+(sc.currentLine+1), sc.file, sc.currentLine+1);
                        out.append("//TODO: Missing tag impl.\n");
                    }
                }
            }

        }

        out.append("}\n");

        return new GTFragmentMethodCall(methodName);
    }

    private void generateFastTagInvocation(SourceContext sc, String fullnameToFastTagMethod, String contentMethodName) {
        // must create an inline impl of GTContentRenderer which can render/call the contentMethod and grab the output
        StringBuilder out = sc.out;
        String contentRendererName = "cr_"+(sc.nextMethodIndex++);
        out.append(" play.template2.GTContentRenderer " + contentRendererName + " = new play.template2.GTContentRenderer(){\n" +
                "public play.template2.GTRenderingResult render(){\n");

        // need to capture the output from the contentMethod
        String outputVariableName = "ovn_" + (sc.nextMethodIndex++);
        GTInternalTagsCompiler.generateContentOutputCapturing(contentMethodName, outputVariableName, out);
        out.append( "return new play.template2.GTRenderingResult("+outputVariableName+");\n");
        out.append(" }};\n");

        // invoke the static fast-tag method
        out.append(fullnameToFastTagMethod+"(this, tagArgs, "+contentRendererName+");\n");
        
    }

    private void generateLegacyFastTagInvocation(String tagName, SourceContext sc, String fullnameToFastTagMethod, String contentMethodName) {
        // must create an inline impl of GTContentRenderer which can render/call the contentMethod and grab the output
        StringBuilder out = sc.out;
        String contentRendererName = "cr_"+(sc.nextMethodIndex++);
        out.append(" play.template2.GTContentRenderer " + contentRendererName + " = new play.template2.GTContentRenderer(){\n" +
                "public play.template2.GTRenderingResult render(){\n");

        // need to capture the output from the contentMethod
        String outputVariableName = "ovn_" + (sc.nextMethodIndex++);
        GTInternalTagsCompiler.generateContentOutputCapturing(contentMethodName, outputVariableName, out);
        out.append( "return new play.template2.GTRenderingResult("+outputVariableName+");\n");
        out.append(" }};\n");

        // must wrap this lazy content-renderer in a fake Closure
        String fakeClosureName = contentRendererName + "_fc";
        out.append(" play.template2.legacy.GTContentRendererFakeClosure "+fakeClosureName+" = new play.template2.legacy.GTContentRendererFakeClosure(this, "+contentRendererName+");\n");

        // invoke the static fast-tag method
        out.append(fullnameToFastTagMethod+"(\""+tagName+"\", this, tagArgs, "+fakeClosureName+");\n");

    }


    private void generateCodeForGTFragments(SourceContext sc, List<GTFragment> body, String methodName, String tagName) {

        StringBuilder out = sc.out;

        out.append("public void "+methodName+"() {\n");

        // generate code to store old tlid and set new
        out.append(" int org_tlid = this.tlid;\n");
        out.append(" this.tlid = "+(sc.nextMethodIndex++)+";\n");

        // add current tag to list of parentTags
        out.append(" this.enterTag(\""+tagName+"\");\n");

        out.append(" Object "+varName+";\n");
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
                throw new GTCompilerException("Unknown GTFragment-type " + f, sc.file, sc.currentLine);
            }
        }

        // remove tag from parentTags-list
        // add current tag to list of parentTags
        out.append(" this.leaveTag(\""+tagName+"\");\n");
        // restore the tlid
        out.append(" this.tlid = org_tlid;\n");
        out.append("}\n");

    }


}
