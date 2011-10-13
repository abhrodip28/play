package play.template2;

import java.io.File;

public class GTDefaultTemplateFileResolver implements GTTemplateFileResolver {

    // when null we look for templates in working directory, if list, we look for template in thouse folders..
    public static File[] templateFolders = new File[]{null};

    // Translates templatePath to actual File.
    // return null if not found
    public File resolveTemplatePathToFile(String templatePath) {
        if ( templatePath == null) {
            return null;
        }

        // look for template file in all folders in templateFolders-list
        for ( File folder : templateFolders) {

            if ( folder == null) {
                // look for template in working dir.
                File file = new File(templatePath);
                if (file.exists() && file.isFile()) {
                    // did not find template
                    return file;
                }
            } else {

                File file = new File ( folder, templatePath);
                if (file.exists() && file.isFile()) {
                    return file;
                }
            }
        }
        // didn't find it
        return null;
    }
}
