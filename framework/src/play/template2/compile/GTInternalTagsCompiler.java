package play.template2.compile;

import play.template2.exceptions.GTCompilationExceptionWithSourceInfo;

import java.lang.reflect.Method;

/**
 * Created by IntelliJ IDEA.
 * User: mortenkjetland
 * Date: 10/11/11
 * Time: 12:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class GTInternalTagsCompiler {


    public boolean generateCodeForGTFragments( String tagName, String contentMethodName, GTPreCompiler.SourceContext sc, int startLine) {

        // Check if we have a method named 'tag_tagName'

        Method tagMethod = null;
        try {
            tagMethod = getClass().getMethod("tag_"+tagName, String.class, String.class, GTPreCompiler.SourceContext.class, Integer.TYPE);
        } catch( Exception e) {
            // did not find a method to handle this tag
            return false;
        }

        try {
            tagMethod.invoke(this, tagName, contentMethodName, sc, startLine);
        } catch (Exception e) {
            throw new GTCompilationExceptionWithSourceInfo("Error generating code for tag '"+tagName+"'", sc.file, startLine+1, e);
        }

        return true;
    }

    public void tag_list(String tagName, String contentMethodName, GTPreCompiler.SourceContext sc, int startLine) {


        // one can else if list is empty - must clear the else flag
        sc.jprintln(" clearElseFlag();", startLine + 1);


        sc.jprintln(" String as = (String)tagArgs.get(\"as\");");
        sc.jprintln(" String itemName = (as==null?\"_\":as);");
        sc.jprintln(" as = (as == null ? \"\" : as);");

        sc.jprintln(" Object _items = tagArgs.get(\"items\");");
        sc.jprintln(" if (_items == null ) _items = tagArgs.get(\"arg\");");
        sc.jprintln(" if (_items == null ) return ;");

        sc.jprintln(" int i=0;");
        sc.jprintln(" Iterator it = convertToIterator(_items);");
        sc.jprintln(" while( it.hasNext()) {");
        // prepare for next iteration
        sc.jprintln("   Object item = it.next();");
        sc.jprintln("   i++;");
        sc.jprintln("   binding.setProperty(itemName, item);");
        sc.jprintln("   binding.setProperty(as+\"_index\", i);");
        sc.jprintln("   binding.setProperty(as+\"_isLast\", !it.hasNext());");
        sc.jprintln("   binding.setProperty(as+\"_isFirst\", i==1);");
        sc.jprintln("   binding.setProperty(as+\"_parity\", (i%2==0?\"even\":\"odd\"));");

        // call list tag content
        sc.jprintln("   " + contentMethodName + "();");

        sc.jprintln(" }");

        // if we did not iterate over anything, we must set the else-flag so that the next else-block is executed
        sc.jprintln(" if(i==0) { setElseFlag(); }");

    }


    public void tag_if(String tagName, String contentMethodName, GTPreCompiler.SourceContext sc, int startLine) {
        // extract the argument named "arg"
        sc.jprintln(" Object e = tagArgs.get(\"arg\");", startLine + 1);
        // clear the runNextElse
        sc.jprintln(" clearElseFlag();");
        // do the if
        sc.jprintln(" if(evaluateCondition(e)) {" + contentMethodName + "();} else { setElseFlag(); }");
    }

    public void tag_ifnot(String tagName, String contentMethodName, GTPreCompiler.SourceContext sc, int startLine) {
        // extract the argument named "arg"
        sc.jprintln(" Object e = tagArgs.get(\"arg\");", startLine + 1);

        // clear the runNextElse
        sc.jprintln(" clearElseFlag();");
        // do the if
        sc.jprintln(" if(!evaluateCondition(e)) {" + contentMethodName + "();} else { setElseFlag(); }");
    }

    public void tag_else(String tagName, String contentMethodName, GTPreCompiler.SourceContext sc, int startLine) {
        // run the else if runNextElse is true

        // do the if
        sc.jprintln(" if( elseFlagIsSet()) {" + contentMethodName + "();}", startLine + 1);

        // clear runNextElse
        sc.jprintln(" clearElseFlag();");
    }

    public void tag_elseif(String tagName, String contentMethodName, GTPreCompiler.SourceContext sc, int startLine) {
        // run the elseif if runNextElse is true AND expression is true

        // do the if
        sc.jprintln(" if( elseFlagIsSet()) {", startLine + 1);

        // Just include the regluar if-tag here..
        tag_if(tagName, contentMethodName, sc, startLine);

        sc.jprintln(" }");
    }

    public void tag_extends(String tagName, String contentMethodName, GTPreCompiler.SourceContext sc, int startLine) {
        // the template we extends is the single argument named 'args'

        String templateNameVar = "_tn_"+ (sc.nextMethodIndex++);
        sc.jprintln(" String "+templateNameVar + " = (String)tagArgs.get(\"arg\");", startLine +1 );
        // must check runtime that the template exists
        sc.jprintln(" if(!this.templateRepo.templateExists("+templateNameVar+")) " +
                "{throw new play.template2.exceptions.GTTemplateNotFoundWithSourceInfo("+templateNameVar+", this.templateFile, "+(startLine+1)+");}", startLine+1);

        sc.jprintln(" this.extendsTemplatePath = "+templateNameVar+";");

        // that's it..
    }

    // used when dumping the output from the template that extended this one
    public void tag_doLayout(String tagName, String contentMethodName, GTPreCompiler.SourceContext sc, int startLine) {
        // someone is extending us - and we are supposed to dump the output now..
        sc.jprintln(" if( this.extendingTemplate == null) throw new play.template2.exceptions.GTRuntimeException(\"No template is currently extending this template\");", startLine + 1);
        // inject all the output from the extending template into our output stream
        sc.jprintln(" this.insertOutput(this.extendingTemplate);");

        // done..
    }



    protected static void generateContentOutputCapturing( String contentMethodName, String outputVariableName, GTPreCompiler.SourceContext sc, int line) {
        sc.jprintln("//generateContentOutputCapturing", line + 1);
        // remember the original out
        sc.jprintln("StringWriter org = out;");
        // remember the original list
        sc.jprintln("List<StringWriter> orgAllOuts = allOuts;");

        // create a new one for capture
        sc.jprintln("allOuts = new ArrayList<StringWriter>();");
        sc.jprintln("initNewOut();");

        // call the content-method
        sc.jprintln(contentMethodName + "();");
        // store the output
        sc.jprintln("List<StringWriter> " + outputVariableName + " = allOuts;");
        // restore the original out
        sc.jprintln("out = org;");
        // restore the list
        sc.jprintln("allOuts = orgAllOuts;");

    }
}
