

import java.io.Serializable;
import java.lang.Cloneable;
import java.util.concurrent.ThreadFactory;

/**
 * Hello world class.
 */
@HelloAnnotation("hello hotswap")
public class HelloWorldHotswap extends Thread {
    public Integer toBeAdded  = 43;

    public static String staticAdded = "helloStatic";

    public static String hello() {
        return "Hello World Hotswap";
    }

    @HelloAnnotation("hello hotswap")
    public String hello(final @HelloAnnotation("par hotswap") String par) {
        HelloInterface anonymous = new HelloInterface() {
            @Override
            @HelloAnnotation("hello hotswap")
            public String hello(final @HelloAnnotation("par hotswap") String dummy) {
                return "Hello Hotswap " + par;
            }
        };
        return anonymous.hello(null);
    }

    // chage ordering of annonymous classes. HotswapperPlugin should kick in.
    public void dummy() {
        new Cloneable() {};
    }

    public String tobeAdded() {
        return "added method";
    }

    public Boolean toBeModified(Object input) {
        return ((String) input).contains("!");
    }
//
//    @Override
//    public Thread newThread(Runnable r) {
//        return null;
//    }
}
