package assets;

/**
 * Created by achau on 12/27/14.
 */
public class sand {
    public static void main(String[] args) {


    }

    public static String fractionToDecimal(int numerator, int denominator) {
        StringBuffer rtn = new StringBuffer();

        int n = numerator;
        int d = denominator;
        int r = 0;
        boolean first = true;

        while(first) {
            if (n > d) {
                int counter = 0;
                while (counter * d < n)
                    counter ++;
                n = counter*d - n;
                rtn.append(counter);
            } else if (n < d) {
                rtn.append(".");
                longdivision(n, d, r, rtn);

            } else {
                rtn.append("1");
            } //n == d


            first = false;
        }

        return rtn.toString();
    }

    private static void longdivision(int n, int d, int r, StringBuffer rtn) {
        if (n > d) {
            int counter = 0;
            while (counter * d < n)
                counter ++;
            n = counter*d - n;
            rtn.append(counter);
        }

    }
}
