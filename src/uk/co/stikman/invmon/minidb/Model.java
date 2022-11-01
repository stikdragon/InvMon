package uk.co.stikman.invmon.minidb;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.co.stikman.invmon.inverter.InvUtil;
import uk.co.stikman.table.DataRecord;
import uk.co.stikman.table.DataTable;

public class Model {
	private Map<String, Field> fields = new HashMap<>();

	public void loadXML(InputStream str) throws IOException {
		Document doc = InvUtil.loadXML(str);
		for (Element el : InvUtil.getElements(doc.getDocumentElement())) {
			Field f = new Field(InvUtil.getAttrib(el, "id"));
			if (find(f.getId()) != null)
				throw new IllegalArgumentException("Field [" + f.getId() + "] already declared");
			f.setKey(InvUtil.getAttrib(el, "key", "false").equals("true"));
			f.setType(FieldType.valueOf(InvUtil.getAttrib(el, "type").toUpperCase()));
			String s = InvUtil.getAttrib(el, "parent", null);
			if (s != null) {
				f.setParent(find(s));
				if (f.getParent() == null)
					throw new IllegalArgumentException("Field [" + f.getId() + "] specifies missing field [" + s + "] as parent");
			}
			fields.put(f.getId(), f);
		}
	}

	public void writeXML(OutputStream str) {
		throw new RuntimeException("Not implemented yet");
	}

	public Field get(String name) {
		Field x = find(name);
		if (x == null)
			throw new NoSuchElementException("Field [" + name + "] does not exist");
		return x;
	}

	public Field find(String name) {
		return fields.get(name);
	}

	public void add(Field fld) {
		if (find(fld.getId()) != null)
			throw new IllegalStateException("Field [" + fld.getId() + "] already exists");
		fields.put(fld.getId(), fld);
	}

	@Override
	public String toString() {
		DataTable dt = new DataTable();
		dt.addFields("ID", "Type", "Parent");
		fields.keySet().stream().sorted().forEach(k -> {
			DataRecord r = dt.addRecord();
			Field f = fields.get(k);
			r.setValue(0, f.getId());
			r.setValue(1, f.getType().name());
			r.setValue(2, f.getParent() == null ? "-" : f.getParent().getId());
		});
		return dt.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fields == null) ? 0 : fields.hashCode());
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
		Model other = (Model) obj;
		if (fields == null) {
			if (other.fields != null)
				return false;
		} else if (!fields.equals(other.fields))
			return false;
		return true;
	}

}
