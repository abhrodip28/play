package play.template2;

public interface GTFastTagResolver {

    // if fastTag is valid, this method returns full method-name for a static method..
    public String resolveFastTag(String tagName);
}
