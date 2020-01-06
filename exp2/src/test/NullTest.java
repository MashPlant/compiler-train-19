package test;

public class NullTest {
    private static boolean st_q;

    public static Integer getInteger(boolean q) {
        if (q) {
            return new Integer(7);
        } else {
            return null;
        }
    }

    public static void Test1(boolean q, Integer b) {
        Integer a, c;

        a = new Integer(1);

        if (b != null) {
            b.toString();
        }

        if (a == b) {
            b.toString();
            c = a;
        } else {
            if (b == null) {
                b = new Integer(2);
            }
            c = a;
        }

        b.toString();
        a.toString();
        c.toString();
    }

    public static void Test2(boolean q, Integer a) {
        Integer b, c, d, e;

        b = a;
        a.toString();
        b.toString();

        a = b;
        a.toString();

        c = getInteger(false);

        if (q) {
            c = a;
            c.toString();
        }

        b = c;
        b.toString();
        c.toString();

        d = getInteger(true);
        e = getInteger(false);

        if (d == e) {
            d.toString();
            e.toString();
        }
    }

    public static Integer getInteger() {
        if (st_q) {
            return new Integer(7);
        } else {
            return null;
        }
    }

    public static void Test3(boolean q, Integer b) {
        Integer a, c;

        Integer[] arr0 = new Integer[4];
        arr0.toString();

        a = new Integer(1);
        a = getInteger(true);
        a.toString();

        if (b == null) {
            a = null;
        } else {
            a = getInteger(true);
        }

        a.toString();
        for (int i = 0; i < 2; ++i) {
            a.toString();
            a = getInteger(true);
        }

        Integer[] arr;
        if (q) {
            arr = new Integer[1];
        } else {
            arr = null;
        }

        arr.toString();
        arr = new Integer[2];
        arr[0] = a;
        arr.toString();
        arr[0].toString();
    }

    public static void main(String[] args) {
        Test1(true, new Integer(0));
        Test2(true, new Integer(0));
        Test3(true, new Integer(0));
    }
}
