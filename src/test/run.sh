set -e


cd /Users/joeylee/workspace/HotswapPlayground
javac -cp target/test-classes/:target/classes/:target/extra /Users/joeylee/workspace/HotswapPlayground/src/test/java/HotSwapTest.java -d /Users/joeylee/workspace/HotswapPlayground/target/test-classes/
cd -

/Library/Java/JavaVirtualMachines/jdk1.8.0_181.jdk/Contents/Home/bin/java -version
# shellcheck disable=SC2211
cd ../../
#java  -cp target/test-classes/:target/classes/  -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=6798 -javaagent:/Users/joeylee/workspace/HotswapAgent/hotswap-agent/target/hotswap-agent.jar  HotSwapTest
/Library/Java/JavaVirtualMachines/jdk1.8.0_181.jdk/Contents/Home/bin/java  -cp target/test-classes/:target/classes/  -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=6798 -javaagent:/Users/joeylee/workspace/HotswapAgent/hotswap-agent/target/hotswap-agent.jar  HotSwapTest

