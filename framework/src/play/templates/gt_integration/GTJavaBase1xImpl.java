package play.templates.gt_integration;

import play.Play;
import play.data.validation.Validation;
import play.i18n.Messages;
import play.mvc.Router;
import play.template2.GTGroovyBase;
import play.template2.GTJavaBase;
import play.templates.BaseTemplate;
import play.templates.TagContext;
import play.utils.HTML;

import java.io.File;

public abstract class GTJavaBase1xImpl extends GTJavaBase {

    public GTJavaBase1xImpl(Class<? extends GTGroovyBase> groovyClass, String templatePath, File templateFile) {
        super(groovyClass, templatePath, templateFile);
    }

    // add extra methods used when resolving actions
    public String __reverseWithCheck_absolute_true(String action) {
        return __reverseWithCheck(action, true);
    }

    public String __reverseWithCheck_absolute_false(String action) {
        return __reverseWithCheck(action, false);
    }

    public static String __reverseWithCheck(String action, boolean absolute) {
        return Router.reverseWithCheck(action, Play.getVirtualFile(action), absolute);
    }

    @Override
    public boolean validationHasErrors() {
        return Validation.hasErrors();
    }

    @Override
    public boolean validationHasError(String key) {
        return Validation.hasError( key );
    }

    @Override
    public String messagesGet(Object key, Object... args) {
        return Messages.get(key, args);
    }

    @Override
    public Class getRawDataClass() {
        return BaseTemplate.RawData.class;
    }

    @Override
    public String convertRawDataToString(Object rawData) {
        return ((BaseTemplate.RawData)rawData).data;
    }

    @Override
    public String escapeHTML(String s) {
        return HTML.htmlEscape(s);
    }

    @Override
    protected void renderingStarted() {
        super.renderingStarted();
        TagContext.init();
    }

    @Override
    public void enterTag(String tagName) {
        super.enterTag(tagName);
        TagContext.enterTag(tagName);
    }

    @Override
    public void leaveTag(String tagName) {
        super.leaveTag(tagName);
        TagContext.exitTag();
    }

}
