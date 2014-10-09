package grasmin;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 *
 */
public class PerformanceTest {

    static final int MAX_INT = (int) Math.pow(2, 30);
    static final int MAX_DATA_POINTS_PER_RUN = 10000;

    abstract static class TestThread extends Thread {

        public final List<Integer> results = new ArrayList<Integer>(MAX_DATA_POINTS_PER_RUN);
        public volatile int[][] datapoints;
        public volatile long cpuTime;

        TestThread(String name) {
            super(name);
        }

        abstract int gcd(int a, int b);

        @Override
        public void run() {
            try {
                long start = System.nanoTime();
                for (int[] point : datapoints) {
                    results.add(gcd(point[0], point[1]));
                }
                cpuTime = System.nanoTime() - start;
            } catch (Throwable t) {
                System.out.println("Ignoring problem running thread '" + getName() + "': " + t);
            }
        }

    }

    final GrasminTest grasminTest = new GrasminTest();
    final JavaGcd javaGcd = new JavaGcd();
    final PerformanceTestResultCollector collector = new PerformanceTestResultCollector();
    final File report = new File("/temp/grasmin-report.csv");

    TestThread java() {
        return new TestThread("java") {
            @Override
            int gcd(int a, int b) {
                return javaGcd.javaGcd(a, b);
            }
        };
    }

    TestThread groovyStatic() {
        return new TestThread("groovyStatic") {
            @Override
            int gcd(int a, int b) {
                return grasminTest.groovyStaticGcd(a, b);
            }
        };
    }

    TestThread groovyTailRecursive() {
        return new TestThread("groovyTailRecursive") {
            @Override
            int gcd(int a, int b) {
                return grasminTest.tailRecursiveGcd(a, b);
            }
        };
    }

    TestThread grasmine() {
        return new TestThread("grasmine") {
            @Override
            int gcd(int a, int b) {
                return grasminTest.grasmineGcd(a, b);
            }
        };
    }

    void testPerformance() throws InterruptedException {

        // warmup
        runAll(java(), groovyStatic(), groovyTailRecursive(), grasmine());

        // tests
        collector.writeHeaders(report, "java", "groovyStatic", "groovyTailRecursive", "grasmine");
        for (int i = 0; i < 100; i++) {
            System.gc();
            TestThread java = java();
            TestThread groovyStatic = groovyStatic();
            TestThread groovyTailRecursive = groovyTailRecursive();
            TestThread grasmine = grasmine();
            runAll(java, groovyStatic, groovyTailRecursive, grasmine);
            collector.writeResults(report, java, groovyStatic, groovyTailRecursive, grasmine);
        }

    }

    private void runAll(TestThread... threads) throws InterruptedException {
        for (TestThread t : threads) runTest(t);
    }

    private void runTest(TestThread thread) throws InterruptedException {
        thread.datapoints = randomIntPairs();
        thread.start();
        thread.join();
    }

    public static void main(String[] args) throws InterruptedException {
        new PerformanceTest().testPerformance();
    }

    static int[][] randomIntPairs() {
        Random random = new Random();
        int[][] datapoints = new int[MAX_DATA_POINTS_PER_RUN][2];
        for (int[] point : datapoints) {
            point[0] = randomPositiveInt(random);
            point[1] = randomPositiveInt(random);
            //System.out.print(Arrays.toString(point));
        }
        //System.out.println();
        return datapoints;
    }

    static int randomPositiveInt(Random random) {
        return random.nextInt(MAX_INT) + 1;
    }

}
