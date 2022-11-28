package uk.co.stikman.invmon;

public interface ConsumerE<T, E extends Exception> {
	void accept(T t) throws E;
}
