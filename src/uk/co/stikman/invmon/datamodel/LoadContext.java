package uk.co.stikman.invmon.datamodel;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

class LoadContext {
	private final List<RepeatGroup>	groups	= new ArrayList<>();
	private int						version;

	public LoadContext(int ver) {
		this.version = ver;
	}

	public List<RepeatGroup> getGroups() {
		return groups;
	}

	public RepeatGroup findGroup(String id) {
		return groups.stream().filter(x -> x.getId().equals(id)).findFirst().orElse(null);
	}

	public RepeatGroup getGroup(String id) {
		RepeatGroup g = findGroup(id);
		if (g == null)
			throw new NoSuchElementException();
		return g;
	}

	public int getVersion() {
		return version;
	}

}
