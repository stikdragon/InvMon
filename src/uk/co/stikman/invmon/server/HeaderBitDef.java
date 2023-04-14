package uk.co.stikman.invmon.server;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import uk.co.stikman.invmon.datalog.DBRecord;
import uk.co.stikman.invmon.datalog.QueryResults;
import uk.co.stikman.invmon.datamodel.Field;
import uk.co.stikman.invmon.inverter.util.Format;
import uk.co.stikman.invmon.inverter.util.InvUtil;

public class HeaderBitDef {
	private String			text;
	private List<String>	fields	= new ArrayList<>();

	public void configure(Element root) {
		text = InvUtil.getAttrib(root, "text");
		String flds = InvUtil.getAttrib(root, "fields");
		for (String s : flds.split(",")) {
			s = s.trim();
			fields.add(s);
		}
	}

	public String execute(WidgetExecuteContext data) {
		Number[] vals = new Number[fields.size()];
		for (int i = 0; i < fields.size(); ++i) {
			Field f = data.getOwner().getEnv().getModel().get(fields.get(i));
			vals[i] = data.getMostRecent().getNumber(f);
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
