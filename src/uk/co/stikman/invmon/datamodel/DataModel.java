package uk.co.stikman.invmon.datamodel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.co.stikman.invmon.ConsumerE;
import uk.co.stikman.invmon.InvMonException;
import uk.co.stikman.invmon.datalog.DBRecord;
import uk.co.stikman.invmon.datamodel.expr.AddOp;
import uk.co.stikman.invmon.datamodel.expr.CalcMethod;
import uk.co.stikman.invmon.datamodel.expr.CalcOp;
import uk.co.stikman.invmon.datamodel.expr.DivOp;
import uk.co.stikman.invmon.datamodel.expr.FetchValOp;
import uk.co.stikman.invmon.datamodel.expr.FloatStack;
import uk.co.stikman.invmon.datamodel.expr.InvertOp;
import uk.co.stikman.invmon.datamodel.expr.MaxOp;
import uk.co.stikman.invmon.datamodel.expr.MulOp;
import uk.co.stikman.invmon.datamodel.expr.PushFloatOp;
import uk.co.stikman.invmon.datamodel.expr.SubOp;
import uk.co.stikman.invmon.inverter.InvUtil;
import uk.co.stikman.table.DataRecord;
import uk.co.stikman.table.DataTable;

public class DataModel implements Iterable<Field> {
	private Map<String, Field>	fields				= new HashMap<>();
	private List<Field>			fieldList			= new ArrayList<>();
	private List<Field>			calculatedFields	= Collections.emptyList();
	private FieldCounts			fieldCounts			= new FieldCounts();
	private RepeatSettings		repeatSettings		= new RepeatSettings();

	public void loadXML(InputStream str) throws IOException, InvMonException {
		Document doc = InvUtil.loadXML(str);
		fieldCounts = new FieldCounts();
		for (Element el : InvUtil.getElements(doc.getDocumentElement())) {
			if ("Field".equals(el.getTagName())) {
				readField(el, -1);
			} else if ("Repeat".equals(el.getTagName())) {
				String grp = InvUtil.getAttrib(el, "group");
				int cnt = repeatSettings.getCountForGroup(grp, -1);
				if (cnt == -1)
					throw new InvMonException("<Repeat> element encountered with group name [" + grp + "] but the repeatCount has not been set in the RepeatSettings object");
				for (Element el2 : InvUtil.getElements(el)) {
					if (!el2.getTagName().equals("Field"))
						throw new InvMonException("Can only have <Field> elements in a <Repeat> block");
					for (int i = 0; i < cnt; ++i) {
						readField(el2, (i + 1));
					}
				}
			} else
				throw new InvMonException("Invalid element in model: " + el.getTagName());
		}

		//
		// do some checks
		//
		Field ftimestamp = null;
		for (Field f : fields.values()) {
			if (f.getType() == FieldType.TIMESTAMP) {
				if (ftimestamp != null)
					throw new IOException("a TIMESTAMP field has been declared multiple times, there can only be one");
				ftimestamp = f;
			}
		}

		compileExpressions();
	}

	private void readField(Element el, int repeatIndex) throws InvMonException {
		String id = InvUtil.getAttrib(el, "id");
		if (repeatIndex != -1)
			id = id.replace("$", Integer.toString(repeatIndex));
		Field f = new Field(id);
		if (find(f.getId()) != null)
			throw new IllegalArgumentException("Field [" + f.getId() + "] already declared");
		f.setType(FieldType.valueOf(InvUtil.getAttrib(el, "type").toUpperCase()));

		if (f.getType() == FieldType.TIMESTAMP) {
			//
			// nothing else to do, this is a special field
			//
		} else {
			if (f.getType() == FieldType.STRING)
				f.setWidth(Integer.parseInt(InvUtil.getAttrib(el, "width")));
			else
				f.setWidth(f.getDataType().getTypeSize());

			if (el.hasAttribute("aggregationMode")) {
				f.setAggregationMode(AggregationMode.valueOf(el.getAttribute("aggregationMode")));
			} else {
				if (f.getType() == FieldType.STRING)
					f.setAggregationMode(AggregationMode.FIRST);
				else
					f.setAggregationMode(AggregationMode.MEAN);
			}

			if (el.hasAttribute("calculated")) {
				if (f.getDataType() != FieldDataType.FLOAT)
					throw new IllegalArgumentException("Only FLOAT fields can be calculated");
				String t = el.getAttribute("calculated");
				if (repeatIndex != -1)
					t = t.replace("$", Integer.toString(repeatIndex));
				f.setCalculated(t);
			}
			f.setPosition(fieldCounts.getAndInc(f.getDataType()));

			if (el.hasAttribute("calculatedRepeat")) {
				if (repeatIndex != -1)
					throw new InvMonException("<Field> with calculatedRepeat attribute is only valid outside a <Repeat> element");
				int repeatCount = repeatSettings.getCountForGroup(InvUtil.getAttrib(el, "repeatGroup")) ;
				
				String t = el.getAttribute("calculatedRepeat");
				String[] bits = t.split(",");
				if (bits.length != 2)
					throw new InvMonException("Illegal calcaultedRepeat attrib: " + t);
				if (!bits[0].matches("\\[.*\\]"))
					throw new InvMonException("Illegal calcaultedRepeat attrib (invalid field): " + t);
				if (!bits[1].equals("sum") && !bits[1].equals("avg"))
					throw new InvMonException("Illegal calcaultedRepeat attrib (expected \"sum\" or \"avg\"): " + t);
				StringBuilder sb = new StringBuilder();
				String sep = "";
				for (int i = 1; i <= repeatCount; ++i) {
					String s = bits[0].replace("$", Integer.toString(i));
					sb.append(sep).append(s);
					sep = ", ";
				}
				for (int i = 1; i < repeatCount; ++i)
					sb.append(sep).append("+");
				if (bits[1].equals("avg")) // divide through by N for an average
					sb.append(", ").append(repeatIndex).append(", /");
				f.setCalculated(sb.toString());
			}
		}

		fields.put(f.getId(), f);
		fieldList.add(f);
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
		dt.addFields("ID", "Type", "Offset", "Position", "Calc");
		fieldList.forEach(f -> {
			DataRecord r = dt.addRecord();
			r.setValue(0, f.getId());
			r.setValue(1, f.getType().name());
			r.setValue(3, f.getPosition());
			r.setValue(4, f.getCalculated());
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

	public int getFieldCount() {
		return fields.size();
	}

	/**
	 * this list is ordered such that dependent fields come first, so if you
	 * evaluate them from first to last then you'll correctly calculate values
	 * 
	 * @return
	 */
	public List<Field> getCalculatedFields() {
		return calculatedFields;
	}

	/**
	 * set up calculated fields
	 * 
	 * @throws InvMonException
	 */
	private void compileExpressions() throws InvMonException {
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
					} else if (isNumber(bit)) {
						ops.add(new PushFloatOp(Float.parseFloat(bit)));
					} else if (bit.matches("![a-z0-9]+")) {
						if (bit.equals("!invert"))
							ops.add(new InvertOp());
						else if (bit.equals("!max"))
							ops.add(new MaxOp());
						else
							throw new IllegalArgumentException("Unknown function: " + bit);
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
						throw new IllegalArgumentException("Expression invalid for [" + f.getId() + "]: " + f.getCalculated());
				}

				f.setCalculationMethod(new CalcMethod() {
					@Override
					public List<CalcOp> getOps() {
						return ops;
					}

					@Override
					public float calc(DBRecord r) {
						FloatStack stk = new FloatStack(); // can we cache this?
						for (CalcOp op : ops)
							op.calc(r, stk);
						return stk.pop();
					}
				});

			}
		}

		//
		// work out order
		//
		lst = sortByCalcOrder(lst);

		if (!lst.isEmpty())
			calculatedFields = lst;
	}

	private static class Node {
		Field		field;
		List<Node>	children	= new ArrayList<>();

		@Override
		public String toString() {
			return field.getId();
		}
	}

	private List<Field> sortByCalcOrder(List<Field> lst) throws InvMonException {
		//
		// turn them into a tree, then walk the roots for order
		// also a good place to check for circular refs
		//
		Map<Field, Node> lkp = new HashMap<>();
		Set<Node> roots = new HashSet<>();
		for (Field f : lst) {
			Node n = new Node();
			n.field = f;
			roots.add(n);
			lkp.put(f, n);
		}

		for (Field f : lst) {
			for (CalcOp op : f.getCalculationMethod().getOps()) {
				if (op instanceof FetchValOp) {
					FetchValOp fvo = (FetchValOp) op;
					Node n2 = lkp.get(fvo.getField());
					if (n2 != null) { // if it's null then it depends on a non-calculated field, which doesn't matter here
						Node n = lkp.get(f);
						roots.remove(n);
						n2.children.add(n);
					}
				}
			}
		}

		//
		// circ refs
		//
		for (Node root : roots) {
			final Set<Node> visited = new HashSet<>();
			walk(root, n -> {
				if (!visited.add(n))
					throw new InvMonException("Circular reference in field [" + n.field.getId() + "]");
			});
		}

		List<Field> output = new ArrayList<>();
		for (Node root : roots) {
			walk(root, n -> {
				output.remove(n.field);
				output.add(n.field);
			});
		}

		return output;
	}

	private static void walk(Node n, ConsumerE<Node, InvMonException> visitor) throws InvMonException {
		visitor.accept(n);
		for (Node x : n.children)
			walk(x, visitor);
	}

	private boolean isNumber(String bit) {
		try {
			Float.parseFloat(bit);
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}

	public FieldCounts getFieldCounts() {
		return fieldCounts;
	}

	public List<Field> getFields() {
		return fieldList;
	}

	public RepeatSettings getRepeatSettings() {
		return repeatSettings;
	}

	public void setRepeatSettings(RepeatSettings repeatSettings) {
		this.repeatSettings = repeatSettings;
	}

}
