package uk.co.stikman.invmon.datamodel;

public class FloatStack {
	private float[] stack = new float[64];
	private int size;
	
	public float pop() {
		return stack[--size];
	}
	
	public void push(float f) {
		stack[size++] = f;
	}
	
}
