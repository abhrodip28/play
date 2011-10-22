package play.template2.exceptions;

import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: mortenkjetland
 * Date: 10/23/11
 * Time: 12:37 AM
 * To change this template use File | Settings | File Templates.
 */
public class GTRuntimeExceptionWithSourceInfo extends GTRuntimeException {

    public final File srcFile;
    public final int lineNo;

    public GTRuntimeExceptionWithSourceInfo(File srcFile, int lineNo) {
        this.srcFile = srcFile;
        this.lineNo = lineNo;
    }

    public GTRuntimeExceptionWithSourceInfo(String s, File srcFile, int lineNo) {
        super(s);
        this.srcFile = srcFile;
        this.lineNo = lineNo;
    }

    public GTRuntimeExceptionWithSourceInfo(String s, Throwable throwable, File srcFile, int lineNo) {
        super(s, throwable);
        this.srcFile = srcFile;
        this.lineNo = lineNo;
    }

    public GTRuntimeExceptionWithSourceInfo(Throwable throwable, File srcFile, int lineNo) {
        super(throwable);
        this.srcFile = srcFile;
        this.lineNo = lineNo;
    }
}
