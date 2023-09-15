package uk.co.stikman.invmon.client;

import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.events.MouseEvent;
import org.teavm.jso.dom.html.HTMLElement;

import uk.co.stikman.invmon.client.DragHelper.DragHandler;

public class DragHelper {

	public interface DragHandler {
		void drag(int newX, int newY);
	}

	public interface DragStartHandler {
		void start();
	}

	private HTMLElement			hdr;
	private DragHandler			dragHandler;
	private DragStartHandler	dragStartHandler;

	private EventListener<?>	mouseUpEL;
	private EventListener<?>	mouseMoveEL;
	private int					downAtX;
	private int					downAtY;

	public DragHelper(HTMLElement hdr) {
		this.hdr = hdr;
		hdr.addEventListener("mousedown", ev -> mouseDown((MouseEvent) ev));
	}

	private void mouseDown(MouseEvent ev) {
		ev.preventDefault();
		downAtX = ev.getClientX();
		downAtY = ev.getClientY();
		mouseMoveEL = x -> drag((MouseEvent) x);
		mouseUpEL = x -> mouseUp((MouseEvent) x);
		InvMon.getDocument().addEventListener("mousemove", mouseMoveEL);
		InvMon.getDocument().addEventListener("mouseup", mouseUpEL);
		if (dragStartHandler != null)
			dragStartHandler.start();
	}

	private void mouseUp(MouseEvent ev) {
		InvMon.getDocument().removeEventListener("mousemove", mouseMoveEL);
		InvMon.getDocument().removeEventListener("mouseup", mouseUpEL);
	}

	private void drag(MouseEvent ev) {
		ev.preventDefault();
		int newX = (ev.getClientX() - downAtX);
		int newY = (ev.getClientY() - downAtY);
		if (dragHandler != null)
			dragHandler.drag(newX, newY);
	}

	public DragHandler getDragHandler() {
		return dragHandler;
	}

	public void setDragHandler(DragHandler dragHandler) {
		this.dragHandler = dragHandler;
	}

	public DragStartHandler getDragStartHandler() {
		return dragStartHandler;
	}

	public void setDragStartHandler(DragStartHandler dragStartHandler) {
		this.dragStartHandler = dragStartHandler;
	}
}
