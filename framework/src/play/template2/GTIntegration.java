package play.template2;


/**
 * This interface includes functionality tah must be implemented by the framework itself
 */
public interface GTIntegration {

    /**
     * return the class/interface that, when an object is instanceof it, we should use
     * convertRawDataToString when converting it to String
     */
    public Class getRawDataClass();

    /**
     *  See getRawDataClass for info
     */
    public String convertRawDataToString(Object rawData);


    public String escapeHTML( String s);

    public void renderingStarted();
    public void enterTag(String tagName);
    public void leaveTag();

}
