package utils;

import play.template2.*;
import java.util.*;


public class CustomGTFastTag extends GTFastTag {
    
    public static void tag_customGTFastTag(GTJavaBase template, Map<String, Object> args, GTContentRenderer content ) {
        template.out.append("from tag");
    }
    
}