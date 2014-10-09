package grasmin

import grasmin.PerformanceTest.TestThread

import static java.math.BigInteger.valueOf as Int

/**
 *
 */
class PerformanceTestResultCollector {

    void writeHeaders( File location, String... names ) {
        if ( location.exists() ) location.delete()
        location << names.join( ',' ) + '\n'
    }

    void writeResults( File location, TestThread... threads ) {
        location << threads.collect { verify( it ) ? it.cpuTime : -1 }.join( ',' ) + '\n'
    }

    private boolean verify( TestThread testThread ) {
        try {
            assert testThread.results.size() == PerformanceTest.MAX_DATA_POINTS_PER_RUN
            assert testThread.datapoints.length == PerformanceTest.MAX_DATA_POINTS_PER_RUN
            for ( point in testThread.datapoints ) {
                assert gcd( *point ) == testThread.results.remove( 0 )
            }
            return true
        } catch ( AssertionError e ) {
            println "Error! $e"
            return false
        }
    }

    private int gcd( int a, int b ) {
        verifyInts( a, b )
        Int( a ).gcd( Int( b ) )
    }

    private void verifyInts( int ... xs ) {
        xs.each { assert 0 < it && it < PerformanceTest.MAX_INT }
    }

}
