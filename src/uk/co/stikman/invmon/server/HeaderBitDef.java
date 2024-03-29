package uk.co.stikman.invmon.server;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import uk.co.stikman.invmon.Env;
import uk.co.stikman.invmon.datalog.DBRecord;
import uk.co.stikman.invmon.datamodel.ModelField;
import uk.co.stikman.invmon.inverter.util.Format;
import uk.co.stikman.invmon.minidom.MDElement;

public class HeaderBitDef {
	private String			text;
	private List<String>	fields	= new ArrayList<>();

	public void fromDOM(MDElement root) {
		text = root.getAttrib("text");
		String flds = root.getAttrib("fields");
		for (String s : flds.split(",")) {
			s = s.trim();
			fields.add(s);
		}
	}

	public void toDOM(MDElement root) {
		root.setAttrib("text", text);
		root.setAttrib("fields", fields.stream().collect(Collectors.joining(",")));
	}

	public String execute(Env env, DBRecord lastrec) {
		Number[] vals = new Number[fields.size()];
		for (int i = 0; i < fields.size(); ++i) {
			ModelField f = env.getModel().get(fields.get(i));
			vals[i] = lastrec.getNumber(f);
		}

		Format fmt = new Format(text);
		String s = fmt.format((Object[]) vals);
		return boldSquareBits(s);
	}

	protected String boldSquareBits(String s) {
		//
		// this is a bit of a hack, oh well - make anything inside [..] bolded
		//
		s = "<span class=\"a\">" + s;
		s = s.replace("[", "</span><span class=\"b\">");
		s = s.replace("]", "</span><span class=\"a\">");
		s = s + "</span>";
		return s;
	}

	public String getText() {
		return text;
	}

	public List<String> getFields() {
		return fields;
	}

	@Override
	public String toString() {
		return "HeaderBitDef [text=" + text + ", fields=" + fields + "]";
	}

}
