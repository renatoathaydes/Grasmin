package grasmin.test_target;

public class JavaArrays {

    public String[] stringify( int[] ints ) {
        String[] result = new String[ ints.length ];
        for (int i = 0; i < ints.length; i++) {
            result[ i ] = Integer.toString( ints[ i ] );
        }
        return result;
    }
}
