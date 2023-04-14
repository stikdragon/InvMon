package uk.co.stikman.invmon;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.function.Consumer;

import uk.co.stikman.log.DefaultLogFormat;
import uk.co.stikman.log.LogEntry;
import uk.co.stikman.log.LogFormat;
import uk.co.stikman.log.LogTarget;

public class EnvLog extends LogTarget {
	private static final int	MAX_LINES	= 1000;

	private LogFormat			format		= new DefaultLogFormat();
	private LinkedList<String>	list		= new LinkedList<>();

	@Override
	public void log(LogEntry le) {
		synchronized (this) {
			list.add(format.format(le));
			while (list.size() > MAX_LINES)
				list.removeFirst();
		}
	}

	public LogFormat getFormat() {
		return format;
	}

	public void setFormat(LogFormat format) {
		this.format = format;
	}

	public LinkedList<String> getLogLines() {
		return list;
	}

	public void forEach(Consumer<String> callback) {
		synchronized (this) {
			Iterator<String> iter = list.iterator();
			while (iter.hasNext())
				callback.accept(iter.next());
		}
	}
}
