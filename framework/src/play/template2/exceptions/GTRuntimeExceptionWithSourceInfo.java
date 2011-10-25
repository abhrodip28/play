package play.template2.exceptions;

import play.template2.GTTemplateLocation;

import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: mortenkjetland
 * Date: 10/23/11
 * Time: 12:37 AM
 * To change this template use File | Settings | File Templates.
 */
public class GTRuntimeExceptionWithSourceInfo extends GTRuntimeException {

    public final GTTemplateLocation templateLocation;
    public final int lineNo;

    public GTRuntimeExceptionWithSourceInfo(GTTemplateLocation templateLocation, int lineNo) {
        this.templateLocation = templateLocation;
        this.lineNo = lineNo;
    }

    public GTRuntimeExceptionWithSourceInfo(String s, GTTemplateLocation templateLocation, int lineNo) {
        super(s);
        this.templateLocation = templateLocation;
        this.lineNo = lineNo;
    }

    public GTRuntimeExceptionWithSourceInfo(String s, Throwable throwable, GTTemplateLocation templateLocation, int lineNo) {
        super(s, throwable);
        this.templateLocation = templateLocation;
        this.lineNo = lineNo;
    }

    public GTRuntimeExceptionWithSourceInfo(Throwable throwable, GTTemplateLocation templateLocation, int lineNo) {
        super(throwable);
        this.templateLocation = templateLocation;
        this.lineNo = lineNo;
    }
}
