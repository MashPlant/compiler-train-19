package examples;

class ExprTest {
  int test (int a) {
    int b, c, d, e, f;
    c = a + 10;
    f = a + c;
    if (f > 2) {
        f = f - c;
    }
    return f;
  }
}
