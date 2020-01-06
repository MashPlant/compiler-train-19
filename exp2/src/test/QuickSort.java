package test;

public class QuickSort {
    public static final int TEST_VALUE = 200;

    public static void sort(int[] values, int low, int high) {
        int i = low, j = high;
        int pivot = values[low + (high - low) / 2];

        // Partition
        while (i <= j) {
            while (values[i] < pivot) {
                i++;
            }
            while (values[j] > pivot) {
                j--;
            }

            if (i <= j) {
                int temp = values[i];
                values[i] = values[j];
                values[j] = temp;
                i++;
                j--;
            }
        }

        // Recursion
        if (low < j) {
            sort(values, low, j);
        }
        if (i < high) {
            sort(values, i, high);
        }
    }

    private static void run(int n) {
        int[] values = new int[n];

        int x = 7;
        for (int i = 0; i < n; i++) {
            x = (x * 8597) % 2879;
            values[i] = x;
        }

        sort(values, 0, n - 1);

        for (int i = 0; i < n; i++) {
            System.out.print(values[i] + " ");
        }
        System.out.println();
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Please specify the number of values.");
        } else {
            run(Integer.valueOf(args[0]));
        }
    }

    public static void test() {
        run(TEST_VALUE);
    }
}
