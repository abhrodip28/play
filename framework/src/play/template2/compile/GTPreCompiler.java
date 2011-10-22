package play.template2.compile;

import play.template2.GTFastTagResolver;
import play.template2.GTGroovyBase;
import play.template2.GTJavaBase;
import play.template2.GTTemplateRepo;
import play.template2.exceptions.GTCompilationException;
import play.template2.exceptions.GTCompilationExceptionWithSourceInfo;
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
import java.util.Collections;
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

    private final String varName = "ev";

    private final GTTemplateRepo templateRepo;

    public static class SourceContext {
        public final File file;
        // generated java code
        // generated groovy code
        public StringBuilder _out = new StringBuilder();
        public StringBuilder _gout = new StringBuilder();
        public String[] lines;
        public int currentLineNo;
        public int lineOffset;
        public int nextMethodIndex = 0;


        public final String pimpStart;
        public final String pimpEnd;


        public SourceContext(File file, String pimpStart, String pimpEnd) {
            this.file = file;
            this.pimpStart = pimpStart;
            this.pimpEnd = pimpEnd;
        }

        public void jprintln(String line) {
            _out.append( line +"\n");
        }

            public void jprintln(String line, int lineNo) {
            _out.append( line + "//lineNo:"+(lineNo+1)+"\n");
        }

        public void gprintln(String line) {
            _gout.append( line +"\n");
        }

        public void gprintln(String line, int lineNo) {
            _gout.append( line + "//lineNo:"+(lineNo+1)+"\n");
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

    public GTPreCompiler(GTTemplateRepo templateRepo) {
        this.templateRepo = templateRepo;
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
            throw new GTCompilationException("Error reading the file " + file, e);
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


    public Output compile(String templatePath, File file) {
        String src = readContentAsString(file);
        return compile(src, templatePath, file);
    }

    public Output compile(String src, String templatePath, File file) {


        String[] lines = src.split("\\n");

        // Must generate groovy pimp my lib stuff . myst be used in each groovy method that need pimping
        StringBuilder pimpStart = new StringBuilder();
        StringBuilder pimpEnd = new StringBuilder();
        List<String> javaExtensionClasses = getJavaExtensionClasses();
        for (int i=0; i<javaExtensionClasses.size(); i++) {
            String clazz = javaExtensionClasses.get(i);
            pimpStart.append("use(" + clazz + ") {");
            pimpEnd.append("}");
        }

        SourceContext sc = new SourceContext(file, pimpStart.toString(), pimpEnd.toString());
        sc.lines = lines;

        GTFragment fragment = null;

        List<GTFragment> rootFragments = new ArrayList<GTFragment>();

        String templateClassName = generateTemplateClassname( file );
        String templateClassNameGroovy = templateClassName + "G";

        // generate groovy class
        sc.gprintln("package "+generatedPackageName+";", 1);
        sc.gprintln("class " + templateClassNameGroovy + " extends "+getGroovyBaseClass().getName()+" {", 1);


        sc.gprintln(" public Object run(){", 1);
        sc.gprintln(sc.pimpStart+"", 1);
        sc.gprintln(" java_class._renderTemplate();", 1);
        sc.gprintln(sc.pimpEnd+"", 1);
        sc.gprintln(" }",1);

        // generate java class
        sc.jprintln("package "+generatedPackageName+";",1);

        sc.jprintln("import java.util.*;",1);
        sc.jprintln("import java.io.*;",1);

        sc.jprintln("public class " + templateClassName + " extends "+getJavaBaseClass().getName()+" {",1);

        sc.jprintln(" private "+templateClassNameGroovy+" g;",1);

        // add constructor which initializes the templateClassNameGroovy-instance
        sc.jprintln(" public "+templateClassName+"() {",1);
        sc.jprintln("  super("+templateClassNameGroovy+".class, \""+templatePath+"\", new java.io.File(\""+file.getAbsolutePath()+"\"));",1);
        sc.jprintln(" }",1);

        rootFragments.add( new GTFragmentCode(1,"  this.g = ("+templateClassNameGroovy+")groovyScript;\n"));


        while ( (fragment = processNextFragment(sc)) != null ) {
            rootFragments.add( fragment );
        }
        generateCodeForGTFragments(sc, rootFragments, "_renderTemplate");

        // end of java class
        sc.jprintln("}", sc.currentLineNo);

        //gout.append(sc.pimpEnd+"");
        // end of groovy class
        sc.gprintln("}", sc.lineOffset);

        return new Output( generatedPackageName+"."+templateClassName, sc._out.toString(), generatedPackageName+"."+templateClassNameGroovy, sc._gout.toString());
    }

    private String generateTemplateClassname(File file) {
        return "Template2_"+file.getAbsolutePath().replaceAll(":", "D_").replaceAll("/", "_").replaceAll("\\\\","_").replaceAll("\\.", "_").replaceAll("-", "_");
    }

    public static class GTFragment {

        // The source line number in the template source where this fragment was started to be generated from
        public final int startLine;

        public GTFragment(int startLine) {
            this.startLine = startLine;
        }
    }

    public static class GTFragmentMethodCall extends GTFragment{
        public final String methodName;

        public GTFragmentMethodCall(int startLine, String methodName) {
            super(startLine);
            this.methodName = methodName;
        }
    }

    public static class GTFragmentCode extends GTFragment {
        public final String code;

        public GTFragmentCode(int startLine, String code) {
            super(startLine);
            this.code = code;
        }
    }

    public static class GTFragmentScript extends GTFragment {
        public final String scriptSource;

        public GTFragmentScript(int startLine, String scriptSource) {
            super(startLine);
            this.scriptSource = scriptSource;
        }
    }

    public static class GTFragmentEndOfMultiLineTag extends GTFragment {
        public final String tagName;

        public GTFragmentEndOfMultiLineTag(int startLine, String tagName) {
            super(startLine);
            this.tagName = tagName;
        }
    }

    // pattern that find any of the '#/$/& etc we're intercepting. it find the next one - so we know what to look for
    // and start of comment and code-block
    final static Pattern partsP = Pattern.compile("([#\\$&]|@?@)\\{[^\\}]+\\}|(\\*\\{)|(%\\{)");

    // pattern that finds all kinds of tags
    final static Pattern tagP = Pattern.compile("#\\{([^\\}]+)\\}");
    final Pattern tagBodyP = Pattern.compile("([^\\s]+)(?:\\s*$|\\s+(.+))");

    final static Pattern endCommentP = Pattern.compile("\\}\\*");
    final static Pattern endScriptP = Pattern.compile("\\}%");

    // pattern that finds a $/@/@@ (value) with content/expression
    final static Pattern valueP = Pattern.compile("(?:\\$|@?@|&)\\{([^\\}]+)\\}");

    protected GTFragment processNextFragment( SourceContext sc) {
        // find next something..

        int startLine = sc.currentLineNo;
        int startOffset = sc.lineOffset;
        boolean insideComment = false;
        boolean insideScript = false;
        int commentStartLine = 0;
        int scriptStartLine = 0;
        int scriptStartOffset = 0;

        while ( sc.currentLineNo < sc.lines.length) {

            String currentLine = sc.lines[sc.currentLineNo];

            if ( insideComment) {
                // can only look for end-comment
                Matcher m = endCommentP.matcher(currentLine);
                if (m.find(sc.lineOffset)) {
                    // update offset to after comment
                    sc.lineOffset = m.end();
                    insideComment = false;
                    // must update start-line and startOffset to prevent checkForPlainText() from grabbing the comment
                    startLine = sc.currentLineNo;
                    startOffset = sc.lineOffset;
                } else {
                    // skip to next line
                    sc.currentLineNo++;
                    sc.lineOffset = 0;
                    continue;
                }
                continue;
            } else if(insideScript) {
                // we should only look for end-script
                Matcher m = endScriptP.matcher(currentLine);
                if (m.find(sc.lineOffset)) {
                    // found the end of it.
                    // return it as a Script-fragment

                    // Use plainText-finder to extract our script
                    String scriptPlainText = checkForPlainText(sc, scriptStartLine, scriptStartOffset, m.start());

                    sc.lineOffset = m.end();

                    if( scriptPlainText != null ) {
                        return new GTFragmentScript( scriptStartLine, scriptPlainText );
                    }
                    
                } else {
                    // skip to next line
                    sc.currentLineNo++;
                    sc.lineOffset = 0;
                    continue;
                }
            }

            Matcher m = partsP.matcher(currentLine);

            // do we have anything on this line?
            if ( m.find(sc.lineOffset)) {

                // yes we did find something

                // must check for plain text first..
                String plainText = checkForPlainText(sc, startLine, startOffset, m.start());
                if ( plainText != null) {
                    return createGTFragmentCodeForPlainText(startLine, plainText);
                }

                sc.lineOffset = m.end();

                // what did we find?
                int correctOffset = m.start();

                String type = m.group(1);
                boolean commentStart = m.group(2) != null;
                boolean scriptStart = m.group(3) != null;

                if (commentStart) {
                    // just skipping it
                    insideComment = true;
                    commentStartLine = sc.currentLineNo;

                } else if(scriptStart) {
                    insideScript = true;
                    scriptStartLine = sc.currentLineNo;
                    scriptStartOffset = m.end();
                } else if ("#".equals(type)) {
                    // we found a tag - go' get it

                    m = tagP.matcher( currentLine );

                    if (!m.find(correctOffset)) {
                        throw new GTCompilationException("Where supposed to find the #tag here..");
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



                    m = tagBodyP.matcher(tagBody);
                    if (!m.find()) {
                        throw new GTCompilationException("Not supposed to happen");
                    }
                    String tagName = m.group(1);
                    String tagArgString = m.group(2);
                    if (tagArgString == null) {
                        tagArgString = "";
                    }

                    if ( endedTag ) {
                        return new GTFragmentEndOfMultiLineTag(sc.currentLineNo, tagName);
                    }

                    return processTag(sc, tagName, tagArgString, tagWithoutBody);

                } else if ("$".equals(type)) {
                    m = valueP.matcher(currentLine);
                    if (!m.find(correctOffset)) {
                        throw new GTCompilationException("Where supposed to find the $value here..");
                    }

                    String expression = m.group(1).trim();

                    return generateExpressionPrinter(expression, sc);

                } else if ("@".equals(type) || "@@".equals(type)) {
                    m = valueP.matcher(currentLine);
                    if (!m.find(correctOffset)) {
                        throw new GTCompilationException("Where supposed to find the @value here..");
                    }

                    String action = m.group(1).trim();

                    boolean absolute = "@@".equals(type);
                    return generateRegularActionPrinter(absolute, action, sc);

                } else if ("&".equals(type)) {
                    m = valueP.matcher(currentLine);
                    if (!m.find(correctOffset)) {
                        throw new GTCompilationException("Where supposed to find the &value here..");
                    }

                    String messageArgs = m.group(1).trim();


                    return generateMessagePrinter(sc.currentLineNo +1, messageArgs, sc);

                }else {
                    throw new GTCompilationExceptionWithSourceInfo("Don't know how to handle type '"+type+"'", sc.file, sc.currentLineNo +1);
                }
            } else {
                // skip to next line
                sc.currentLineNo++;
                sc.lineOffset = 0;
            }

        }

        if (insideComment) {
            throw new GTCompilationException("Found unclosed comment starting on line " + commentStartLine);
        }

        if (insideScript) {
            throw new GTCompilationException("Found unclosed groovy script starting on line " + scriptStartLine);
        }


        String plainText = checkForPlainText(sc, startLine, startOffset, -1);
        if (plainText != null) {
            return createGTFragmentCodeForPlainText(startLine, plainText);
        }
        return null;
    }

    // the play framework must check for @{} (actions) and patch tag arguments.
    // default impl is to just return as is - override to customize
    protected String checkAndPatchActionStringsInTagArguments( String tagArguments) {
        return tagArguments;
    }

    // The play framework impl must implement this method so that it returns the java-code needed to print the
    // correct action url when rendering the template.
    // Look at generateExpressionPrinter for an idea of how it can be done.
    protected GTFragmentCode generateRegularActionPrinter( boolean absolute, String expression, SourceContext sc) {
        throw new GTCompilationException("actions not supported - override to implement it");
    }

    private GTFragmentCode generateExpressionPrinter(String expression, SourceContext sc) {
        String methodName = generateGroovyExpressionResolver(expression, sc);

        // return the java-code for retrieving and printing the expression

        String javaCode = varName+" = g."+methodName+"();\n" +
                "if ("+varName+"!=null) out.append( objectToString("+varName+"));\n";
        return new GTFragmentCode(sc.currentLineNo +1, javaCode);
    }

    private String generateGroovyExpressionResolver(String expression, SourceContext sc) {
        // check if we already have generated method for this expression
        String methodName = expression2GroovyMethodLookup.get(expression);

        if ( methodName == null ) {

            // generate the groovy method for retrieving the actual value

            methodName = "expression_"+(sc.nextMethodIndex++);
            sc.gprintln("Object "+methodName+"() {", sc.currentLineNo+1);
            //gout.append(sc.pimpStart+"");
            sc.gprintln(" return "+expression+";", sc.currentLineNo+1);
            //gout.append(sc.pimpEnd+"");
            sc.gprintln( "}", sc.currentLineNo+1);

            expression2GroovyMethodLookup.put(expression, methodName);
        }
        return methodName;
    }


    private GTFragmentCode generateMessagePrinter(int startLine, String messageArgs, SourceContext sc) {

        String methodName = generateGroovyExpressionResolver("["+messageArgs+"]", sc);

        // return the java-code for retrieving and printing the message

        String javaCode = "out.append( handleMessageTag(g."+methodName+"()));\n";
        return new GTFragmentCode(startLine, javaCode);
    }

    private GTFragmentCode createGTFragmentCodeForPlainText(int startLine, String plainText) {
        if (plainText == null) {
            return null;
        }
        
        String oneLiner = plainText.replace("\\", "\\\\").replaceAll("\"", "\\\\\"").replaceAll("\r\n", "\n").replaceAll("\n", "\\\\n");

        if ( oneLiner.length() > 0) {
            return new GTFragmentCode(startLine, "out.append(\""+oneLiner+"\");");
        } else {
            return null;
        }
    }

    private String checkForPlainText(SourceContext sc, int startLine, int startOffset, int endOfLastLine) {
        if (sc.currentLineNo == startLine && sc.lineOffset == startOffset && sc.lineOffset == endOfLastLine) {
            return null;
        }

        StringBuilder sb = new StringBuilder();

        int line = startLine;
        while ( line <= sc.currentLineNo && line < sc.lines.length) {
            if (line == startLine) {
                if ( startLine ==  sc.currentLineNo) {
                    sb.append(sc.lines[line].substring(startOffset, endOfLastLine));

                    // done
                    break;
                } else {
                    sb.append(sc.lines[line].substring(startOffset));
                }
            } else if ( line < sc.currentLineNo) {
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

        // must advance sc-offset
        sc.lineOffset = endOfLastLine;

        return sb.toString();
    }

    protected GTFragment processTag( SourceContext sc, String tagName, String tagArgString, boolean tagWithoutBody) {
        final List<GTFragment> body = new ArrayList<GTFragment>();
        return processTag(sc, tagName, tagArgString, tagWithoutBody, body);
    }

    protected GTFragment processTag( SourceContext sc, String tagName, String tagArgString, boolean tagWithoutBody, final List<GTFragment> body) {

        int startLine = sc.currentLineNo;

        if ( tagWithoutBody) {
            return generateTagCode(startLine, tagName, tagArgString, sc, body);
        }

        GTFragment nextFragment = null;
        while ( (nextFragment = processNextFragment( sc )) != null ) {
            if ( nextFragment instanceof GTFragmentEndOfMultiLineTag) {
                GTFragmentEndOfMultiLineTag f = (GTFragmentEndOfMultiLineTag)nextFragment;
                if (f.tagName.equals(tagName)) {
                    return generateTagCode(startLine, tagName, tagArgString, sc, body);
                } else {
                    throw new GTCompilationExceptionWithSourceInfo("Found unclosed tag #{"+tagName+"}", sc.file, startLine+1);
                }
            } else {
                body.add(nextFragment);
            }
        }

        throw new GTCompilationExceptionWithSourceInfo("Found unclosed tag #{"+tagName+"}", sc.file, startLine+1);
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

            // if only one argument, then we must name it 'arg'
            if (!tagArgString.matches("^[_a-zA-Z0-9]+\\s*:.*$")) {
                tagArgString = "arg:" + tagArgString;
            }

            tagArgString = checkAndPatchActionStringsInTagArguments(tagArgString);

            methodName = "args_"+fixStringForCode(tagName) + "_"+(sc.nextMethodIndex++);
            sc.gprintln("Map<String, Object> "+methodName+"() {", sc.currentLineNo+1);
            //gout.append(sc.pimpStart+"");
            sc.gprintln(" return ["+tagArgString+"];", sc.currentLineNo+1);
            //gout.append(sc.pimpEnd+"");
            sc.gprintln( "}", sc.currentLineNo+1);

            tagArgs2GroovyMethodLookup.put(tagArgString, methodName);
        }

        // must return the javacode needed to get the data
        return " Map tagArgs = (Map)g."+methodName+"();\n";
    }
    
    protected String fixStringForCode( String s) {
        // some tags (tag-files) can contain dots int the name - must remove them
        return s.replace('.','_');
    }

    private String generateMethodName(String hint, SourceContext sc) {
        hint = fixStringForCode(hint);
        return "m_" + hint + "_" + (sc.nextMethodIndex++);
    }



    private GTFragmentMethodCall generateTagCode(int startLine, String tagName, String tagArgString, SourceContext sc, List<GTFragment> body) {

        // generate groovy code for tag-args
        String javaCodeToGetRefToArgs = generateGroovyCodeForTagArgs( sc, tagName, tagArgString);


        String methodName = generateMethodName(tagName, sc);
        String contentMethodName = methodName+"_content";

        // generate method that runs the content..
        generateCodeForGTFragments( sc, body, contentMethodName);


        sc.jprintln("public void "+methodName+"() {", startLine+1);

        // add current tag to list of parentTags
        sc.jprintln(" this.enterTag(\""+tagName+"\");", startLine+1);
        sc.jprintln(" try {", startLine+1);

        // add tag args code
        sc.jprintln(javaCodeToGetRefToArgs, startLine+1);

        if ( !gtInternalTagsCompiler.generateCodeForGTFragments(tagName, contentMethodName, sc, startLine)) {
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
                    GTLegacyFastTagResolver legacyFastTagResolver = getGTLegacyFastTagResolver();
                    GTLegacyFastTagResolver.LegacyFastTagInfo legacyFastTagInfo = null;
                    if ( legacyFastTagResolver != null && (legacyFastTagInfo = legacyFastTagResolver.resolveLegacyFastTag(tagName))!=null) {
                        generateLegacyFastTagInvocation(tagName, sc, legacyFastTagInfo, contentMethodName);
                    } else {

                        // look for tag-file

                        // tag names can contain '.' which should be transoformed into '/'
                        String tagNamePath = tagName.replace('.', '/');


                        String thisTemplateType = getTemplateType( sc );
                        // look for tag-file with same type/extension as this template
                        String tagFilePath = "tags/"+tagNamePath + "."+thisTemplateType;
                        if (templateRepo!= null && thisTemplateType != null && templateRepo.templateExists(tagFilePath)) {
                            generateTagFileInvocation( tagName, tagFilePath, sc, contentMethodName);
                        } else {
                            // look for tag-file with .tag-extension
                            tagFilePath = "tags/"+tagNamePath + ".tag";
                            if (templateRepo!= null && templateRepo.templateExists(tagFilePath)) {
                                generateTagFileInvocation( tagName, tagFilePath, sc, contentMethodName);
                            } else {
                                // we give up
                                throw new GTCompilationExceptionWithSourceInfo("Cannot find tag-implementation for '"+tagName+"'", sc.file, sc.currentLineNo +1);
                            }
                        }
                    }
                }
            }

        }

        // remove tag from parentTags-list
        sc.jprintln("} finally {",startLine+1);
        sc.jprintln(" this.leaveTag(\""+tagName+"\");", startLine+1);
        sc.jprintln("}", startLine+1);

        sc.jprintln("}", startLine+1); // method

        return new GTFragmentMethodCall(startLine, methodName);
    }

    // returns the type/file extension for this template (by looking at filename)
    private String getTemplateType(SourceContext sc) {
        File f = sc.file;
        if ( f == null) {
            return null;
        }

        String name = f.getName();
        int i = name.lastIndexOf('.');
        if ( i<0 ) {
            return null;
        }

        return name.substring(i+1);

    }

    private void generateFastTagInvocation(SourceContext sc, String fullnameToFastTagMethod, String contentMethodName) {
        // must create an inline impl of GTContentRenderer which can render/call the contentMethod and grab the output
        String contentRendererName = "cr_"+(sc.nextMethodIndex++);
        generateGTContentRenderer(sc, contentMethodName, contentRendererName);

        // invoke the static fast-tag method
        sc.jprintln(fullnameToFastTagMethod+"(this, tagArgs, "+contentRendererName+");", sc.currentLineNo+1);
        
    }

    private void generateGTContentRenderer(SourceContext sc, String contentMethodName, String contentRendererName) {
        sc.jprintln(" play.template2.GTContentRenderer " + contentRendererName + " = new play.template2.GTContentRenderer(){\n" +
                "public play.template2.GTRenderingResult render(){", sc.currentLineNo+1);

        // need to capture the output from the contentMethod
        String outputVariableName = "ovn_" + (sc.nextMethodIndex++);
        GTInternalTagsCompiler.generateContentOutputCapturing(contentMethodName, outputVariableName, sc, sc.currentLineNo);
        sc.jprintln( "return new play.template2.GTRenderingResult("+outputVariableName+");", sc.currentLineNo+1);
        sc.jprintln(" }", sc.currentLineNo+1);
        // must implement runtime property get and set
        sc.jprintln(" public Object getRuntimeProperty(String name){ try { return binding.getProperty(name); } catch (groovy.lang.MissingPropertyException mpe) { return null; }}", sc.currentLineNo+1);


        sc.jprintln(" public void setRuntimeProperty(String name, Object value){binding.setProperty(name, value);}", sc.currentLineNo+1);
        sc.jprintln(" };", sc.currentLineNo+1);
    }

    private void generateLegacyFastTagInvocation(String tagName, SourceContext sc, GTLegacyFastTagResolver.LegacyFastTagInfo legacyFastTagInfo, String contentMethodName) {
        // must create an inline impl of GTContentRenderer which can render/call the contentMethod and grab the output
        String contentRendererName = "cr_"+(sc.nextMethodIndex++);
        generateGTContentRenderer(sc, contentMethodName, contentRendererName);

        // must wrap this lazy content-renderer in a fake Closure
        String fakeClosureName = contentRendererName + "_fc";
        sc.jprintln(" play.template2.legacy.GTContentRendererFakeClosure "+fakeClosureName+" = new play.template2.legacy.GTContentRendererFakeClosure(this, "+contentRendererName+");", sc.currentLineNo+1);

        // invoke the static fast-tag method
        sc.jprintln(legacyFastTagInfo.bridgeFullMethodName+"(\""+legacyFastTagInfo.legacyFastTagClassname+"\", \"" + legacyFastTagInfo.legacyFastTagMethodName + "\", this, tagArgs, "+fakeClosureName+");", sc.currentLineNo+1);

    }

    private void generateTagFileInvocation(String tagName, String tagFilePath, SourceContext sc, String contentMethodName) {
        // must create an inline impl of GTContentRenderer which can render/call the contentMethod and grab the output
        String contentRendererName = "cr_"+(sc.nextMethodIndex++);
        generateGTContentRenderer(sc, contentMethodName, contentRendererName);

        // generate the methodcall to invokeTagFile
        sc.jprintln(" this.invokeTagFile(\""+tagName+"\",\""+tagFilePath+"\", "+contentRendererName+", tagArgs);", sc.currentLineNo+1);

    }


    private void generateCodeForGTFragments(SourceContext sc, List<GTFragment> body, String methodName) {

        sc.jprintln("public void "+methodName+"() {", sc.currentLineNo+1);

        // generate code to store old tlid and set new
        sc.jprintln(" int org_tlid = this.tlid;",sc.currentLineNo+1);
        sc.jprintln(" this.tlid = "+(sc.nextMethodIndex++)+";", sc.currentLineNo+1);


        sc.jprintln(" Object "+varName+";", sc.currentLineNo+1);
        for ( GTFragment f : body) {
            if (f instanceof GTFragmentMethodCall) {
                GTFragmentMethodCall m = (GTFragmentMethodCall)f;
                sc.jprintln("  " + m.methodName + "();", sc.currentLineNo+1);
            } else if (f instanceof GTFragmentCode) {
                GTFragmentCode c = (GTFragmentCode)f;
                if ( c.code.length() > 0) {
                    sc.jprintln("  " + c.code + "", sc.currentLineNo+1);
                }
            } else if(f instanceof GTFragmentScript){
                GTFragmentScript s = (GTFragmentScript)f;
                // first generate groovy method with script code
                String groovyMethodName = "custom_script_" + (sc.nextMethodIndex++);
                sc.gprintln(" void "+groovyMethodName+"(java.io.PrintWriter out){", s.startLine);
                int lineNo = s.startLine;
                //gout.append(sc.pimpStart+"");
                for ( String line : s.scriptSource.split("\n")) {
                    sc.gprintln( line, lineNo++);
                }
                //gout.append(sc.pimpEnd+"");
                sc.gprintln(" }", lineNo);

                // then generate call to that method from java
                sc.jprintln(" g."+groovyMethodName+"(new PrintWriter(out));", s.startLine);

            } else if(f instanceof GTFragmentEndOfMultiLineTag){
                GTFragmentEndOfMultiLineTag _f = (GTFragmentEndOfMultiLineTag)f;
                throw new GTCompilationExceptionWithSourceInfo("#{/"+_f.tagName+"} is not opened", sc.file, f.startLine+1);

            } else {
                throw new GTCompilationExceptionWithSourceInfo("Unknown GTFragment-type " + f, sc.file, f.startLine+1);
            }
        }


        // restore the tlid
        sc.jprintln(" this.tlid = org_tlid;", sc.currentLineNo+1);
        sc.jprintln("}", sc.currentLineNo+1);

    }

    public Class<? extends GTGroovyBase> getGroovyBaseClass() {
        return GTGroovyBase.class;
    }
    public Class<? extends GTJavaBase> getJavaBaseClass() {
        return GTJavaBase.class;
    }

    public List<String> getJavaExtensionClasses() {
        return Collections.emptyList();
    }

    // override it to return correct GTLegacyFastTagResolver
    public GTLegacyFastTagResolver getGTLegacyFastTagResolver() {
        return null;
    }

}
