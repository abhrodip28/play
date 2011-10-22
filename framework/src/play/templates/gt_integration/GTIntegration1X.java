package play.templates.gt_integration;

import play.template2.GTIntegration;
import play.template2.GTTemplateRepo;
import play.template2.compile.GTPreCompiler;
import play.template2.compile.GTPreCompilerFactory;
import play.templates.BaseTemplate;
import play.templates.TagContext;
import play.utils.HTML;

public class GTIntegration1X implements GTIntegration
{

    public static class PreCompilerFactory implements GTPreCompilerFactory {
        public GTPreCompiler createCompiler(GTTemplateRepo templateRepo) {
            return new GTPreCompiler1xImpl(templateRepo);
        }
    }

    public Class getRawDataClass() {
        return BaseTemplate.RawData.class;
    }

    public String convertRawDataToString(Object rawData) {
        return ((BaseTemplate.RawData)rawData).data;
    }

    public String escapeHTML(String s) {
        return HTML.htmlEscape(s);
    }

    public void renderingStarted() {
        TagContext.init();
    }

    public void enterTag(String tagName) {
        TagContext.enterTag(tagName);
    }

    public void leaveTag() {
        TagContext.exitTag();
    }
}
