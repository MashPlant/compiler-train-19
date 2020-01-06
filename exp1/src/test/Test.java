package test;

public class Test {
    public static void main (String [] args) {
	int a, b, c;
	a = 3;
	if (a < 2) {
	    b = 3;
	    c = 5;
	} else {
	    b = 3;
	    c = 6;
	}
	a = -a;
	c = a+b;
	System.out.println(a+" "+b+" "+c);
    }
}
