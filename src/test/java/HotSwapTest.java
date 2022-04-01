import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.concurrent.ThreadFactory;

/**
 * +---------------------------+---------+
 * | feature                   | checked |
 * +---------------------------+---------+
 * | method - body change      | ✔️       |
 * +---------------------------+---------+
 * | method - add/delete       | ✔️       |
 * +---------------------------+---------+
 * | field  - add/delete       | ✔️       |
 * +---------------------------+---------+
 * | field  - change static    | ✔️       |
 * +---------------------------+---------+
 * | annota - change           | ✔️       |
 * +---------------------------+---------+
 * | class  - add interface    | ✔️       |
 * +---------------------------+---------+
 * | class  - change hierarchy | ✔️       |
 * +---------------------------+---------+
 */

public class HotSwapTest {

    public static void main(String[] args) throws Exception {
        /**
         * works for all vms
         */
        extraClassPathPrecede();
        methodChangeBody();
        annotationsChange();

        /**
         * Only dcevm full
         * full patches support full redefenition capabilities (including removal of superclasses, for example).
         * light patches are easier to maintain, but they only support limited functionality
         * (generally, additions to class hierarchies are fine, removals are not).
         */
        methodAddAndDelete();
        methodChangeParameterAndReturnValue();
        fieldAddAndDelete();
        fieldStaticAdd();
        // only full patch support this
//        hierarchyChange();

    }

    private static void extraClassPathPrecede() throws Exception {
        System.out.println(HelloWorldHotswap.hello());
        always(HelloWorldHotswap.hello().equals("Hello World Extra"), "extra classpath precede");
    }

    private static void methodChangeBody() throws Exception {
        boolean usingExtraVersion = true;
        for (int i = 0; i < 4; i++) {
            copyClassFile("HelloWorldHotswap", usingExtraVersion);
            System.out.println("===================================");
            System.out.println("using " + (usingExtraVersion ? " extra " : " hotswap "));
            sleep(); // reload may not finish in a shor time
            String currentHello = HelloWorldHotswap.hello();
            System.out.println(currentHello);

            never(currentHello.endsWith("Extra") && !usingExtraVersion, "unexpected class version");

            usingExtraVersion = !usingExtraVersion;
            System.out.println("===================================");
        }
    }

    private static void methodChangeParameterAndReturnValue() throws Exception {
        copyClassFile("HelloWorldHotswap", true);
        sleep();

        String parm1 = "hello";
        HelloWorldHotswap obj = new HelloWorldHotswap();
        Method originM = HelloWorldHotswap.class.getDeclaredMethod("toBeModified", String.class);
        originM.setAccessible(true);
        Object ret = originM.invoke(obj, parm1);
        always(ret instanceof String, "why not");
        always(ret.equals("hello!"), "why not");

        copyClassFile("HelloWorldHotswap", false);
        sleep();
        Method newM = HelloWorldHotswap.class.getDeclaredMethod("toBeModified", Object.class);
        newM.setAccessible(true);
        ret = newM.invoke(obj, ret);
        always(ret instanceof Boolean, "why not");
        always(ret == Boolean.TRUE, "must contains !");
    }

    private static void methodAddAndDelete() throws Exception {
        copyClassFile("HelloWorldHotswap", true);
        sleep();

        HelloWorldHotswap obj = new HelloWorldHotswap();
        Method tobeDeleted = HelloWorldHotswap.class.getDeclaredMethod("toBeDeleted");
        tobeDeleted.setAccessible(true);
        tobeDeleted.invoke(obj, null);

        Method tobeAdded = null;
        try {
            tobeAdded = HelloWorldHotswap.class.getDeclaredMethod("tobeAdded");
        } catch (NoSuchMethodException noSuchMethodException) {
            // expected
        }
        always(Objects.isNull(tobeAdded), "tobeAdded is null");

        copyClassFile("HelloWorldHotswap", false);
        sleep();

        tobeDeleted = null;
        try {
            tobeDeleted = HelloWorldHotswap.class.getDeclaredMethod("toBeDeleted"); // still get the method?
        } catch (NoSuchMethodException noSuchMethodException) {
            // expected
        }
        always(Objects.isNull(tobeDeleted), "tobeDeleted is null");

        tobeAdded = HelloWorldHotswap.class.getDeclaredMethod("tobeAdded");
        tobeAdded.setAccessible(true);
        System.out.println(tobeAdded); // ????

    }

    private static void fieldAddAndDelete() throws Exception {
        copyClassFile("HelloWorldHotswap", true);
        sleep();

        HelloWorldHotswap helloWorldHotswap = new HelloWorldHotswap();
        Field tobeDeleted = HelloWorldHotswap.class.getDeclaredField("toBeDeleted");
        Object f =  tobeDeleted.get(helloWorldHotswap);
        always(f instanceof String, "field type");
        never(Objects.isNull(f), "null initial value");

        copyClassFile("HelloWorldHotswap", false);
        sleep();

        Field tobeAdded = HelloWorldHotswap.class.getDeclaredField("toBeAdded");
        f = tobeAdded.get(helloWorldHotswap); // old obj
        always(f == null, "empty for old obj");
        f = tobeAdded.get(new HelloWorldHotswap());
        always(f instanceof Integer, "field type");
        always(43 == (Integer) f, "43 initial value");
    }

    private static void fieldStaticAdd() throws Exception {
        copyClassFile("HelloWorldHotswap", false);
        sleep();

        HelloWorldHotswap helloWorldHotswap = new HelloWorldHotswap();
        Field staticAdded = HelloWorldHotswap.class.getDeclaredField("staticAdded");
        always(staticAdded.get(null).equals("helloStatic"), "static injected");
    }

    private static void hierarchyChange() throws Exception {
        copyClassFile("HelloWorldHotswap", true);
        sleep();
        HelloWorldHotswap oldHello = new HelloWorldHotswap();
        always(Thread.class.isAssignableFrom(oldHello.getClass()), "thread Hierarchy");

        copyClassFile("HelloWorldHotswap", false);
        sleep();

        HelloWorldHotswap newHello = new HelloWorldHotswap();
        always(Thread.class.isAssignableFrom(oldHello.getClass()), "thread Hierarchy changed");
        always(ThreadFactory.class.isAssignableFrom(newHello.getClass()), "factory now");
        ThreadFactory factory = (ThreadFactory) newHello;
        always(factory.newThread(null) == null, "null impl");

    }

    // annotation updated
    private static void annotationsChange() throws Exception {
        Class clazz = HelloWorldHotswap.class;
        HelloAnnotation clazzAnnotation = (HelloAnnotation) clazz.getAnnotation(HelloAnnotation.class);
        if (clazzAnnotation != null) // clazzAnnotation is not available for anonymous inner class
            always(Objects.equals(clazzAnnotation.value(), "hello hotswap"), "hello hotswap");

        Method helloMethod = clazz.getDeclaredMethod("hello", String.class);
        HelloAnnotation methodAnnotation = (HelloAnnotation) helloMethod.getAnnotation(HelloAnnotation.class);
        always(Objects.equals(methodAnnotation.value(), "hello hotswap"), "hello hotswap");
        HelloAnnotation paramAnnotation = (HelloAnnotation) helloMethod.getParameterAnnotations()[0][0];
        always(Objects.equals(paramAnnotation.value(), "par hotswap"), "par hotswap");

    }

    private static void copyClassFile(String name, boolean usingExtraVersion) throws IOException {
        String fileName = name.replace(".", "/") + ".class";
        String rootp = System.getProperty("user.dir");
        Path source = Paths.get(rootp + (usingExtraVersion ? "/target/extraBackup/" : "/target/hotswap/") + fileName);
        Path target = Paths.get(rootp + "/target/extra/" + fileName);
        System.out.println("copy from " + source + " to " + target);

        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    private static void always(boolean mustTrue, String msg) {
        if (!mustTrue)
            throw new AssertionError(msg);
    }

    private static void never(boolean mustFalse, String msg) {
        if (mustFalse)
            throw new AssertionError(msg);
    }

    private static void sleep() {
        try {
            Thread.sleep(3000);
        } catch (Exception e) {}
    }
}
