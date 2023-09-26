package uk.co.stikman.invmon.controllers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

public class TimeWindow {
	private final LocalDateTime	start;
	private final LocalDateTime	end;

	public TimeWindow(LocalDateTime start, LocalDateTime end) {
		super();
		this.start = start.truncatedTo(ChronoUnit.SECONDS);
		this.end = end.truncatedTo(ChronoUnit.SECONDS);
	}

	public TimeWindow(TimeWindow copy) {
		this.start = copy.start;
		this.end = copy.end;
	}

	public TimeWindow(LocalDate date, LocalTime start, LocalTime end) {
		this.start = LocalDateTime.of(date, start).truncatedTo(ChronoUnit.SECONDS);
		if (start.isAfter(end)) // spans two days
			this.end = LocalDateTime.of(date, end).truncatedTo(ChronoUnit.SECONDS).plusDays(1);
		else // same day
			this.end = LocalDateTime.of(date, end).truncatedTo(ChronoUnit.SECONDS);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((end == null) ? 0 : end.hashCode());
		result = prime * result + ((start == null) ? 0 : start.hashCode());
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
		TimeWindow other = (TimeWindow) obj;
		if (end == null) {
			if (other.end != null)
				return false;
		} else if (!end.equals(other.end))
			return false;
		if (start == null) {
			if (other.start != null)
				return false;
		} else if (!start.equals(other.start))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "TimeWindow [start=" + start + ", end=" + end + "]";
	}

	public LocalDateTime getStart() {
		return start;
	}

	public LocalDateTime getEnd() {
		return end;
	}

	public boolean contains(LocalDateTime dt) {
		LocalDateTime x = dt.truncatedTo(ChronoUnit.SECONDS);
		if (start.equals(x))
			return true;
		if (end.equals(x))
			return true;
		return start.truncatedTo(ChronoUnit.SECONDS).isBefore(x) && end.isAfter(x);
	}

}
