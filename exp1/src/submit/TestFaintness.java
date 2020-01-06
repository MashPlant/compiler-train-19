package submit;

class TestFaintness {
  /**
   * In this method all variables are faint because the final value is never used.
   * Sample out is at src/test/Faintness.out
   */
  void test1() {
    int x = 2;
    int y = x + 2;
    int z = x + y;
    return;
  }

  // the testcase in readme cannot be used, since the "return x" will be optimized to "return1"
  int test2(int x0) {
    int x = x0; // x is not faint
    int y = x + 2; // y & z are faint
    int z = x + y;
    return x;
  }

  void test3(int x0) {
    int a = x0; // a is not faint
    int[] b = new int[a]; // b is faint
    int c = x0; // c is not faint
    System.out.println(c);
    int d = x0; // d is not faint (because Unary doesn't participate in faint analysis)
    int e = -d; // e is faint
    for (int i = 0; ; i = i + 1) { // i is faint

    }
  }
}
