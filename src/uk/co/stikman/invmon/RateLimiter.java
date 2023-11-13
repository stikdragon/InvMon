package uk.co.stikman.invmon;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * this works by tracking event times and counting them. this means if you give
 * it a massive <code>count</code> property then it will store that many events
 * (which are {@link Long} instances) and might take a long time to count them.
 * it's best used with small counts over small times, like "50 events in 20s" or
 * that kind of order of magnitude. don't use it for "50,000,000 events over an
 * hour"
 * 
 * @author stik
 *
 */
public class RateLimiter {

	public interface TimeSource {
		long get();
	}

	private final int			duration;
	private final int			maxEvents;
	private LinkedList<Long>	events	= new LinkedList<>();
	private TimeSource			timeSource;

	public RateLimiter(int count, int seconds) {
		this(count, seconds, () -> System.currentTimeMillis());
	}

	public RateLimiter(int count, int seconds, TimeSource timesource) {
		this.maxEvents = count;
		this.duration = seconds;
		this.timeSource = timesource;
	}

	public int getDuration() {
		return duration;
	}

	public int getCount() {
		return maxEvents;
	}

	public synchronized void trigger() {
		events.addFirst(Long.valueOf(timeSource.get()));
		while (events.size() > maxEvents)
			events.removeLast();
	}

	/**
	 * returns <code>true</code> if we've triggered the set limit
	 * 
	 * @return
	 */
	public synchronized boolean test() {
		//
		// work backwards until our duration and count them
		//
		int c = 0;
		long cutoff = timeSource.get() - duration * 1000;
		Iterator<Long> iter = events.iterator();
		while (iter.hasNext()) {
			Long l = iter.next();
			if (l.longValue() < cutoff)
				break;
			++c;
		}

		return c >= maxEvents;
	}

}
