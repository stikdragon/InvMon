package uk.co.stikman.invmon.datalog;

import org.h2.util.StringUtils;

public class SQLBuilder {
	private StringBuilder sb = new StringBuilder();

	@Override
	public String toString() {
		return sb.toString();
	}

	public SQLBuilder append(String s) {
		sb.append(s);
		return this;
	}

	/**
	 * wraps it in " chars
	 * 
	 * @param s
	 * @return
	 */
	public SQLBuilder appendName(String s) {
		sb.append("\"").append(s).append("\"");
		return this;
	}

	public SQLBuilder appendEscaped(String s) {
		sb.append(StringUtils.quoteStringSQL(s));
		return this;
	}

	public SQLBuilder append(float f) {
		sb.append(f);
		return this;
	}

}
