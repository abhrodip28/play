package play.template2;

import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: mortenkjetland
 * Date: 10/10/11
 * Time: 9:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class GTCompilerException extends RuntimeException {
    public final File file;
    public final int lineNumber;

    public GTCompilerException(String msg, File file, int lineNumber) {
        super(msg);
        this.file = file;
        this.lineNumber = lineNumber;
    }
}
