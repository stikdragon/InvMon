package uk.co.stikman.invmon.datamodel;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class RepeatGroup {
	private final String		id;
	private int					count;
	private List<ReplaceToken>	replaceTokens	= new ArrayList<>();

	public RepeatGroup(String id) {
		super();
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public int getCount() {
		return count;
	}

	public List<ReplaceToken> getReplaceTokens() {
		return replaceTokens;
	}

	public void setCount(int count) {
		this.count = count;
	}

	/**
	 * apply all the replaceTokens to this input string
	 * 
	 * @param in
	 * @param idx
	 * @return
	 */
	public String subst(String s, int idx) {
		//
		// eg: <Field id="INV_$ID$_TEMP" type="float" source="$SRC$.temp" /> becomes 
		//
		for (ReplaceToken rt : replaceTokens) {
			String f = Pattern.quote("$" + rt.getId() + "$");
			s = s.replaceAll(f, rt.getValues().get(idx));
		}
		
		return s;
	}

}
