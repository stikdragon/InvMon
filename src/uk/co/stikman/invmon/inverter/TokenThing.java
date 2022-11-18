package uk.co.stikman.invmon.inverter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TokenThing implements Iterable<Tok> {

	private String		input;
	private List<Tok>	bits	= new ArrayList<>();

	public TokenThing(String input) {
		this.input = input;
		boolean b = false;
		int last = -1;
		for (int i = 0; i < input.length(); ++i) {
			char ch = input.charAt(i);
			if (ch == '[') {
				if (i > 0)
					add(last + 1, i, b);
				last = i;
				b = true;
			} else if (ch == ']') {
				add(last + 1, i, b);
				last = i;
				b = false;
			}
		}
		if (last != input.length() - 1) {
			add(last + 1, input.length(), b);
		}
	}

	private void add(int from, int to, boolean sq) {
		Tok t = new Tok();
		t.text = input.substring(from, to);
		t.square = sq;
		bits.add(t);
	}

	@Override
	public Iterator<Tok> iterator() {
		return bits.iterator();
	}

}
