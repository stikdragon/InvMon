package uk.co.stikman.invmon.inverter;

public class InverterUtils {
	public static String padLeft(String s, int len) {
		if (s == null)
			s = "";
		if (s.length() >= len)
			return s;
		char[] res = new char[len - s.length()];
		for (int i = 0; i < res.length; ++i) 
			res[i] = ' ';
		return new String(res).concat(s);
	}
}
