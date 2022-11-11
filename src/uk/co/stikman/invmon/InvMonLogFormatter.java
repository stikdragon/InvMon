package uk.co.stikman.invmon;

import java.text.SimpleDateFormat;

import uk.co.stikman.log.LogEntry;
import uk.co.stikman.log.LogFormat;

public class InvMonLogFormatter extends LogFormat {

	private SimpleDateFormat	dateFormat	= new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

	@Override
	public String format(LogEntry le) {
		StringBuilder sb = new StringBuilder();
		sb.append(padLeft(dateFormat.format(le.getTime()), 20));
		sb.append(" ");
		sb.append(padLeft(le.getLevel().name(), 6));
		sb.append(" [");
		String s = le.getLogger();
		if (s.startsWith("uk.co.stikman.invmon."))
			s = "*." + s.substring(21);
		sb.append(padRight(s, 30));
		sb.append("] ");
		sb.append(le.getMessage());
		return sb.toString();
	}

	private static String padLeft(String s, int len) {
		if (s.length() >= len)
			return s;
		char[] buf = new char[len];
		int slen = s.length();
		System.arraycopy(s.toCharArray(), 0, buf, len - slen, slen);
		len -= slen;
		for (int i = 0; i < len; ++i)
			buf[i] = ' ';
		return new String(buf);
	}
	
	private static String padRight(String s, int len) {
		if (s.length() >= len)
			return s;
		char[] buf = new char[len];
		int slen = s.length();
		System.arraycopy(s.toCharArray(), 0, buf, 0, slen);
		for (int i = slen; i < len; ++i)
			buf[i] = ' ';
		return new String(buf);
	}

	public SimpleDateFormat getDateFormat() {
		return dateFormat;
	}

	public void setDateFormat(SimpleDateFormat dateFormat) {
		this.dateFormat = dateFormat;
	}

}