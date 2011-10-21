package play.template2.exceptions;


import java.io.File;

public class GTCompilationExceptionWithSourceInfo extends GTCompilationException {

    public final String specialMessage;
    public final File templatePath;
    public final int lineNo;

    public GTCompilationExceptionWithSourceInfo(String specialMessage, File templatePath, int lineNo) {
        this.specialMessage = specialMessage;
        this.templatePath = templatePath;
        this.lineNo = lineNo;
    }

    public GTCompilationExceptionWithSourceInfo(String specialMessage, File templatePath, int lineNo, Throwable throwable) {
        super(throwable);
        this.specialMessage = specialMessage;
        this.templatePath = templatePath;
        this.lineNo = lineNo;
    }

    @Override
    public String getMessage() {
        return String.format("CompilationError: %s. Template %s:%d", specialMessage, templatePath, lineNo);
    }

}
