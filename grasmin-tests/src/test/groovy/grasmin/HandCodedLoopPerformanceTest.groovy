package grasmin

import grasmin.test_target.ArraysCode
import grasmin.test_target.JavaArrays
import groovy.transform.CompileStatic
import javafx.util.Pair

import java.util.function.Function

class HandCodedLoopPerformanceTest {

    static final File reportFile = new File( 'loop-performance.csv' )
    static final Random random = new Random()

    static void main( String[] args ) {
        def arrays = new ArraysCode()
        def java = new JavaArrays()

        run 100, [
                new Pair<String, Function<int[], Long>>( 'ArraysCode', arrays.&stringify ),
                new Pair<String, Function<int[], Long>>( 'JavaArrays', java.&stringify ),
        ]
    }

    @CompileStatic
    static void run( int count, List<Pair<String, Function<int[], Long>>> tests ) {
        reportFile.delete()
        final writer = reportFile.newPrintWriter()

        writer.println( 'run,' + tests*.key.join( ',' ) )

        count.times { int i ->
            def times = tests*.value.collect { action ->
                System.gc()
                def input = ( 1..10000 ).collect { PerformanceTest.randomPositiveInt( random ) } as int[]
                withTimer { ( action as Function<int[], Long> ).apply( input ) }
            }
            writer.println( "$i," + times.join( ',' ) )
        }

        writer.close()
    }

    @CompileStatic
    static long withTimer( Runnable action ) {
        long result = -1
        long startTime = System.nanoTime()
        try {
            action.run()
            result = System.nanoTime() - startTime
        } catch ( e ) {
            e.printStackTrace()
        }

        return result
    }

}
