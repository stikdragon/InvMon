package uk.co.stikman.invmon.datamodel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.co.stikman.invmon.inverter.InvUtil;
import uk.co.stikman.table.DataRecord;
import uk.co.stikman.table.DataTable;

public class DataModel implements Iterable<Field> {
	private Map<String, Field>	fields				= new HashMap<>();
	private List<Field>			fieldList			= new ArrayList<>();
	private int					recordWidth;
	private List<Field>			calculatedFields	= Collections.emptyList();

	public void loadXML(InputStream str) throws IOException {
		Document doc = InvUtil.loadXML(str);
		int idx = 0;
		int offset = 0;
		recordWidth = 0;
		for (Element el : InvUtil.getElements(doc.getDocumentElement())) {
			Field f = new Field(InvUtil.getAttrib(el, "id"));
			if (find(f.getId()) != null)
				throw new IllegalArgumentException("Field [" + f.getId() + "] already declared");
			f.setType(FieldType.valueOf(InvUtil.getAttrib(el, "type").toUpperCase()));
			String s = InvUtil.getAttrib(el, "parent", null);
			if (s != null) {
				f.setParent(find(s));
				if (f.getParent() == null)
					throw new IllegalArgumentException("Field [" + f.getId() + "] specifies missing field [" + s + "] as parent");
			}

			if (f.getType() == FieldType.STRING)
				f.setWidth(Integer.parseInt(InvUtil.getAttrib(el, "width")));
			else
				f.setWidth(f.getType().getTypeSize());
			
			if (el.hasAttribute("aggregationMode")) {
				f.setAggregationMode(AggregationMode.valueOf(el.getAttribute("aggregationMode")));
			} else {
				if (f.getType() == FieldType.STRING)
					f.setAggregationMode(AggregationMode.FIRST);
				else
					f.setAggregationMode(AggregationMode.MEAN);
			}

			if (el.hasAttribute("calculated")) {
				if (f.getType().getBaseType() != FieldType.FLOAT)
					throw new IllegalArgumentException("Only FLOAT fields can be calculated");
				f.setCalculated(el.getAttribute("calculated"));
				f.setPosition(-1);
				f.setOffset(-1);
			} else {
				f.setPosition(idx++);
				f.setOffset(offset);
				offset += f.getWidth();
				recordWidth += f.getWidth();
			}

			fields.put(f.getId(), f);
			fieldList.add(f);
		}

		compileExpressions();

		System.out.println(toString());
	}

	public void writeXML(OutputStream str) throws IOException {
		try {
			DocumentBuilder bld = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = bld.newDocument();
			Element root = doc.createElement("Model");
			doc.appendChild(root);
			fieldList.forEach(f -> {
				Element el = doc.createElement("Field");
				el.setAttribute("id", f.getId());
				el.setAttribute("type", f.getType().name());
				if (f.getParent() != null)
					el.setAttribute("parent", f.getParent().getId());
				if (f.getType() == FieldType.STRING)
					el.setAttribute("width", Integer.toString(f.getWidth()));
				if (f.getCalculated() != null)
					el.setAttribute("calculated", f.getCalculated());
				root.appendChild(el);
			});

			Transformer xf = TransformerFactory.newInstance().newTransformer();
			xf.transform(new DOMSource(doc), new StreamResult(str));

		} catch (Exception e) {
			throw new IOException("Failed to write model xml: " + e.getMessage(), e);
		}
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
		dt.addFields("ID", "Type", "Parent", "Offset", "Position");
		fieldList.forEach(f -> {
			DataRecord r = dt.addRecord();
			r.setValue(0, f.getId());
			r.setValue(1, f.getType().name());
			r.setValue(2, f.getParent() == null ? "-" : f.getParent().getId());
			r.setValue(3, f.getOffset());
			r.setValue(4, f.getPosition());
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
		DataModel other = (DataModel) obj;
		if (fields == null) {
			if (other.fields != null)
				return false;
		} else if (!fields.equals(other.fields))
			return false;
		return true;
	}

	/**
	 * returns a group of up to three fields that have the suffixes _V, _I, _F. Is a
	 * convenience thing. Throws exception if non of them can be found
	 * 
	 * @param prefix
	 * @return
	 */
	public FieldVIF getVIF(String prefix) {
		FieldVIF x = new FieldVIF();
		x.setF(find(prefix + "_F"));
		x.setV(find(prefix + "_V"));
		x.setI(find(prefix + "_I"));
		if (x.getF() == null && x.getV() == null && x.getI() == null)
			throw new NoSuchElementException("Field group [" + prefix + "] not found");
		return x;
	}

	@Override
	public Iterator<Field> iterator() {
		return fields.values().iterator();
	}

	public int getRecordWidth() {
		return recordWidth;
	}

	public int getFieldCount() {
		return fields.size();
	}

	public List<Field> getCalculatedFields() {
		return calculatedFields;
	}

	/**
	 * set up calculated fields
	 */
	private void compileExpressions() {
		List<Field> lst = new ArrayList<>();
		for (Field f : this) {
			if (f.getCalculated() != null) {
				lst.add(f);

				//
				// calculations are very basic, just +-*/, and RPN
				//
				// TODO: make this more comprehensive
				String[] bits = f.getCalculated().split(",");
				final List<CalcOp> ops = new ArrayList<>();
				for (String bit : bits) {
					bit = bit.trim();
					if (bit.matches("\\[[a-zA-Z0-9_]+\\]")) {
						ops.add(new FetchValOp(get(bit.substring(1, bit.length() - 1))));
					} else if (bit.matches("[\\-\\+\\*\\/]")) {
						if (bit.charAt(0) == '-')
							ops.add(new SubOp());
						else if (bit.charAt(0) == '+')
							ops.add(new AddOp());
						else if (bit.charAt(0) == '*')
							ops.add(new MulOp());
						else if (bit.charAt(0) == '/')
							ops.add(new DivOp());
					} else
						throw new IllegalArgumentException("Expression invalid");
				}

				f.setCalculationMethod(r -> {
					FloatStack stk = new FloatStack(); // can we cache this?
					for (CalcOp op : ops)
						op.calc(r, stk);
					return stk.pop();
				});
			}
		}
		if (!lst.isEmpty())
			calculatedFields = lst;
	}

}
