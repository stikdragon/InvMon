package uk.co.stikman.invmon.datamodel.expr;

public class FloatStack {
	private float[]	stack	= new float[64];
	private int		size;

	public float pop() {
		return stack[--size];
	}

	public void push(float f) {
		stack[size++] = f;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < size; ++i)
			sb.append(stack[i]).append("\n");
		return sb.toString();
	}

}
