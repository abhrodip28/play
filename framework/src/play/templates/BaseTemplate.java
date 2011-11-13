package play.templates;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import play.Logger;
import play.Play;
import play.classloading.BytecodeCache;
import play.exceptions.JavaExecutionException;
import play.exceptions.NoRouteFoundException;
import play.exceptions.TagInternalException;
import play.exceptions.TemplateExecutionException;
import play.exceptions.TemplateExecutionException.DoBodyException;
import play.libs.Codec;
import play.libs.IO;

/**
 * This class is not removed (after starting to use the new groovy template engine)
 * to not break old fast tags.
 * We cannot remove the layoutData-map and RawData.
 */
public abstract class BaseTemplate extends Template {

    public static ThreadLocal<Map<Object, Object>> layoutData = new ThreadLocal<Map<Object, Object>>();

    public static class RawData {

        public String data;

        public RawData(Object val) {
            if (val == null) {
                data = "";
            } else {
                data = val.toString();
            }
        }

        @Override
        public String toString() {
            return data;
        }
    }
}
