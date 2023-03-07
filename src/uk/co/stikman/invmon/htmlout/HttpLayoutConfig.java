package uk.co.stikman.invmon.htmlout;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.w3c.dom.Element;

import uk.co.stikman.invmon.inverter.InvUtil;

public class HttpLayoutConfig {
	private List<PageLayout>	pages	= new ArrayList<>();
	private PageLayout			defaultPage;

	public void configure(Element root) {
		InvUtil.getElements(root, "Page").forEach(el -> {
			PageLayout pl = new PageLayout();
			pl.configure(el);
			if (pl.isDefault())
				defaultPage = pl;
			pages.add(pl);
		});

		if (defaultPage == null)
			defaultPage = pages.get(0);
	}

	public List<PageLayout> getPages() {
		return pages;
	}

	public PageLayout getDefaultPage() {
		return defaultPage;
	}

	public PageLayout getPage(String name) {
		for (PageLayout x : pages)
			if (name.equals(x.getId()))
				return x;
		throw new NoSuchElementException(name);
	}
}
