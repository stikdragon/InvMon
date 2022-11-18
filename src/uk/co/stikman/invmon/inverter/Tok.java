package uk.co.stikman.invmon.inverter;

public class Tok {
	String	text;
	boolean	square;

	public String getText() {
		return text;
	}

	public boolean isSquare() {
		return square;
	}

	@Override
	public String toString() {
		return text + (square ? " *" : "");
	}
}