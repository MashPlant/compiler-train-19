package test;

public class TestTwo {
    public static int frobble(int x) {
	return 8-x;
    }

    public static void main (String [] args) {
	int a=0, b=0, c=0, d;

	for (a=0; a<7; a++) {

	    for (d=0; d<7; d++) {
	        c=frobble(c);
	    }
				
	    if (a-b == 2) {
		c++;
	    } else if (c == 4) {
		break;
	    }
	}

	c++;

    }
}
