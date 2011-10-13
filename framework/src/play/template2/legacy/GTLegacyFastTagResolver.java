package play.template2.legacy;

import play.template2.GTFastTagResolver;

/**
 * Legacy fast-tags is only used to be backward compatible with old play 1.x FastTag-methods.
 * Legacy fastTags are slower than the new ones.
 * Legacy-Fast-tag methods must look like this one:
 *
 *
public static void tag_testFastTag(String tagName, GTJavaBase template, Map<String, Object> args, Closure body ) {
        template.out.append("[testFastTag before]");
        template.insertOutput( content.render());
        template.out.append("[from testFastTag after]");
    }


 */
public interface GTLegacyFastTagResolver extends GTFastTagResolver {
}
