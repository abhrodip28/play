package play.template2.exceptions;


import java.io.File;

public class GTCompilationExceptionWithSourceInfo extends GTCompilationException {

    public final String specialMessage;
    public final File srcFile;
    public final int lineNo;

    public GTCompilationExceptionWithSourceInfo(String specialMessage, File srcFile, int lineNo) {
        this.specialMessage = specialMessage;
        this.srcFile = srcFile;
        this.lineNo = lineNo;
    }

    public GTCompilationExceptionWithSourceInfo(String specialMessage, File srcFile, int lineNo, Throwable throwable) {
        super(throwable);
        this.specialMessage = specialMessage;
        this.srcFile = srcFile;
        this.lineNo = lineNo;
    }

    @Override
    public String getMessage() {
        return String.format("CompilationError: %s. Template %s:%d", specialMessage, srcFile, lineNo);
    }

}
