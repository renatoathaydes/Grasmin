package grasmin;

/**
 *
 */
public class JavaGcd {

    public int javaRecursiveGcd(int u, int v) {
        if (u < v) {
            int tmp = u;
            u = v;
            v = tmp;
        }
        if (v == 0) {
            return u;
        }
        return javaRecursiveGcd(v, u - v);
    }

    public int javaGcd(int u, int v) {
        int small = u;
        int large = v;
        while (small != 0) {
            if (large < small) {
                int tmp = small;
                small = large;
                large = tmp;
            }
            if (small != 0) large -= small;

        }
        return large;
    }

}
