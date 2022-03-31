import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

public class HotSwapTest {

    public static void main(String[] args) throws Exception {

        /**
         * works for all vms
         */

//        extraClassPathPrecede();
//        changeMethodBody();
//        checkAnnotations(HelloWorldHotswap.class);

        /**
         * Only dcevm
         */

        addAndDeleteMethod();
        changeMethodParameterAndReturnValue();
    }

    private static void extraClassPathPrecede() throws Exception {
        System.out.println(HelloWorldHotswap.hello());
        always(HelloWorldHotswap.hello().equals("Hello World Extra"), "extra classpath precede");
    }

    private static void changeMethodBody() throws Exception {
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

    private static void changeMethodParameterAndReturnValue() throws Exception {
        copyClassFile("HelloWorldHotswap", true);
        String parm1 = "hello";
        HelloWorldHotswap obj = new HelloWorldHotswap();
        Method originM = HelloWorldHotswap.class.getDeclaredMethod("toBeModified", String.class);
        originM.setAccessible(true);
        Object ret = originM.invoke(obj, parm1);
        always(ret instanceof String, "why not");
        always(ret.equals("hello!"), "why not");

        copyClassFile("HelloWorldHotswap", false);
        sleep();
        Method newM = HelloWorldHotswap.class.getDeclaredMethod("toBeModified", String.class);
        newM.setAccessible(true);
        ret = newM.invoke(obj, parm1);
        always(ret instanceof Boolean, "why not");

    }

    private static void addAndDeleteMethod() throws Exception {
        copyClassFile("HelloWorldHotswap", true);
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


    // annotation updated
    private static void checkAnnotations(Class clazz) throws Exception {
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
