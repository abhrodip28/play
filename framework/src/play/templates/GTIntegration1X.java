package play.templates;

import play.template2.GTIntegration;
import play.utils.HTML;

/**
 * Created by IntelliJ IDEA.
 * User: mortenkjetland
 * Date: 10/13/11
 * Time: 10:36 AM
 * To change this template use File | Settings | File Templates.
 */
public class GTIntegration1X implements GTIntegration
{
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
