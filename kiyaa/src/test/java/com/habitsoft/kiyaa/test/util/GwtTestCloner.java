package com.habitsoft.kiyaa.test.util;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;
import com.habitsoft.kiyaa.util.Cloner;

public class GwtTestCloner extends GWTTestCase {

	static class CloneMe implements Serializable {
		private static final long serialVersionUID = 1L;
		
		int a;
		boolean b;
		String c;
		String[] d;
		HashMap<String,String> e;
		
		public CloneMe() {
		}
		
		public CloneMe(int a, boolean b, String c, String[] d,
				HashMap<String, String> e) {
			super();
			this.a = a;
			this.b = b;
			this.c = c;
			this.d = d;
			this.e = e;
		}

		public int getA() {
			return a;
		}
		public void setA(int a) {
			this.a = a;
		}
		public boolean isB() {
			return b;
		}
		public void setB(boolean b) {
			this.b = b;
		}
		public String getC() {
			return c;
		}
		public void setC(String c) {
			this.c = c;
		}
		public String[] getD() {
			return d;
		}
		public void setD(String[] d) {
			this.d = d;
		}
		public HashMap<String, String> getE() {
			return e;
		}
		public void setE(HashMap<String, String> e) {
			this.e = e;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + a;
			result = prime * result + (b ? 1231 : 1237);
			result = prime * result + ((c == null) ? 0 : c.hashCode());
			result = prime * result + Arrays.hashCode(d);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			CloneMe other = (CloneMe) obj;
			if (a != other.a)
				return false;
			if (b != other.b)
				return false;
			if (c == null) {
				if (other.c != null)
					return false;
			} else if (!c.equals(other.c))
				return false;
			if (!Arrays.equals(d, other.d))
				return false;
			return true;
		}
		
	}
	
	static abstract class CloneMeCloner implements Cloner<CloneMe> {
		
	}
	
    public void testClone1() throws Exception {
    	CloneMeCloner cloner = GWT.create(CloneMeCloner.class);
    	CloneMe a = new CloneMe(1, true, "1", null, null);
    	CloneMe b = cloner.clone(a);
    	assertTrue(a != b); // Not the same object
    	assertEquals(a, b);
    	assertEquals(a.getA(), b.getA());
    	assertEquals(a.isB(), b.isB());
    	assertEquals(a.getC(), b.getC());
    	assertTrue(Arrays.equals(a.getD(), b.getD()));
    	assertEquals(a.getE(), b.getE());
    	
    	CloneMe c = new CloneMe(2, false, "2", new String[] {"A", "B", "C"}, null);
    	cloner.clone(c, b);
    	assertTrue(c != b); // Not the same object
    	assertEquals(c, b);
    	assertEquals(c.getA(), b.getA());
    	assertEquals(c.isB(), b.isB());
    	assertEquals(c.getC(), b.getC());
    	assertTrue(Arrays.equals(c.getD(), b.getD()));
    	assertEquals(c.getE(), b.getE());
    }
    
    public void testDiff() throws Exception {
    	CloneMeCloner cloner = GWT.create(CloneMeCloner.class);
    	CloneMe a = new CloneMe(1, true, "1", new String[] {"A", "B", "C"}, null);
    	CloneMe b = cloner.clone(a);
    	b.setA(2);
    	assertTrue(cloner.diff(a, b).containsAll(Arrays.asList("a")));
    	assertFalse(cloner.diff(a, b).containsAll(Arrays.asList("b", "c", "d", "e")));
    	b.setB(!b.isB());
    	assertTrue(cloner.diff(a, b).containsAll(Arrays.asList("a", "b")));
    	assertFalse(cloner.diff(a, b).containsAll(Arrays.asList("c", "d", "e")));
    	b.setC("2");
    	assertTrue(cloner.diff(a, b).containsAll(Arrays.asList("a", "b", "c")));
    	assertFalse(cloner.diff(a, b).containsAll(Arrays.asList("d", "e")));
    	b.setD(new String[] {"A", "B", "E"});
    	assertTrue(cloner.diff(a, b).containsAll(Arrays.asList("a", "b", "c", "d", "d[2]")));
    	assertFalse(cloner.diff(a, b).containsAll(Arrays.asList("e")));
    	b.setB(a.isB());
    	assertTrue(cloner.diff(a, b).containsAll(Arrays.asList("a", "c", "d", "d[2]")));
    	assertFalse(cloner.diff(a, b).containsAll(Arrays.asList("b", "e")));
    }
    
    @Override
    public String getModuleName() {
        return "com.habitsoft.kiyaa.test.KiyaaTests";
    }
}
