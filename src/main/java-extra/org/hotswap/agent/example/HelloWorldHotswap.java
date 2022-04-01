

/**
 * Hello world class to be replaced by hotswap.
 */
@HelloAnnotation("hello extra")
public class HelloWorldHotswap extends Thread {
    public String toBeDeleted = "123";

    public static String hello() {
        return "Hello World Extra";
    }

    // create new annonymous class before HelloInterface. HotswapperPlugin should kick in.
    public void dummy() {
        new Cloneable() {};
    }

    @HelloAnnotation("hello extra")
    public String hello(final @HelloAnnotation("par extra") String par) {
        HelloInterface anonymous = new HelloInterface() {
            @Override
            public String hello(String dummy) {
                return "Hello Extra " + par;
            }
        };
        return anonymous.hello(null);
    }

    public void toBeDeleted() {
    }

    public String toBeModified(String input) {
        return input + "!";
    }

}
