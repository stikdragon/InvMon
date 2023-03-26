package uk.co.stikman.invmon.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ReallySimpleEventBus {

	private abstract class Subscriber implements HandlerReg {

		private String				event;
		private Consumer<Object>	handler;

		public Subscriber(String event, Consumer<Object> handler) {
			this.event = event;
			this.handler = handler;
		}
	}

	private List<Subscriber> subscribers = new ArrayList<>();

	public HandlerReg subscribe(String event, Consumer<Object> handler) {
		Subscriber s = new Subscriber(event, handler) {
			@Override
			public void remove() {
				subscribers.remove(this);
			}
		};
		subscribers.add(s);
		return s;
	}

	public void fire(String event, Object data) {
		for (Subscriber s : subscribers)
			if (s.event.equals(event))
				s.handler.accept(data);
	}

}
