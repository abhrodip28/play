package play.template2;


import play.template2.exceptions.GTCompilationException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

public abstract class IO {
    /**
     * Read file content to a String
     * @param file The file to read
     * @return The String content
     */
    public static String readContentAsString(File file, String encoding) {
        InputStream is = null;
        try {
            is = new FileInputStream(file);
            StringWriter result = new StringWriter();
            PrintWriter out = new PrintWriter(result);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, encoding));
            String line = null;
            while ((line = reader.readLine()) != null) {
                out.println(line);
            }
            return result.toString();
        } catch(IOException e) {
            throw new GTCompilationException("Error reading the file " + file, e);
        } finally {
            if(is != null) {
                try {
                    is.close();
                } catch(Exception e) {
                    //
                }
            }
        }
    }

    /**
     * Read file content to a String (always use utf-8)
     * @param file The file to read
     * @return The String content
     */
    public static String readContentAsString(File file) {
        return readContentAsString(file, "utf-8");
    }

    /**
     * Write binay data to a file
     * @param data The binary data to write
     * @param file The file to write
     */
    public static void write(byte[] data, File file) {
        OutputStream os = null;
        try {
            os = new FileOutputStream(file);
            os.write(data);
            os.flush();
        } catch(IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if(os != null) os.close();
            } catch(Exception e) {
                //
            }
        }
    }



}
