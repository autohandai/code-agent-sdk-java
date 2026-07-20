package ai.autohand.sdk;

/** Fresh-process probe whose timer intentionally excludes JVM startup. */
public final class PackageLoadProbe {
    private PackageLoadProbe() {
    }

    public static void main(String[] args) throws Exception {
        long started = System.nanoTime();
        Class<?> entryPoint = Class.forName("ai.autohand.sdk.AutohandAgentSdk", true,
                PackageLoadProbe.class.getClassLoader());
        entryPoint.getField("VERSION").get(null);
        System.out.println(System.nanoTime() - started);
    }
}
