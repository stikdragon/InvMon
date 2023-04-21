package uk.co.stikman.invmon.server;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.w3c.dom.Element;

import uk.co.stikman.invmon.InvMonException;
import uk.co.stikman.invmon.inverter.util.InvUtil;

public class HttpLayoutConfig {
	private List<PageLayout>	pages	= new ArrayList<>();
	private PageLayout			defaultPage;

	public void configure(Element root) throws InvMonException {
		for (Element el : InvUtil.getElements(root, "Page")) {
			if (!InvUtil.getElements(el).isEmpty())
				throw new InvMonException("<Page> configuration elements must be in their own file");

			String s = InvUtil.getAttrib(el, "source", null);
			if (s == null)
				throw new InvMonException("<Page> configuration element must have a `source` attribute");

			PageLayout pl = new PageLayout(new File(s));
			pl.configure(el);
			if (pl.isDefault())
				defaultPage = pl;
			pages.add(pl);
		}

		if (defaultPage == null)
			defaultPage = pages.get(0);
		
		loadPages();

	}

	/**
	 * load all the page config files, if their files have changed on disk
	 * 
	 * @throws InvMonException
	 */
	public void loadPages() throws InvMonException {
		for (PageLayout page : pages) {
			if (page.getFile().lastModified() != page.getLastModifiedTime())
				page.loadFromSource();
		}
	}

	public List<PageLayout> getPages() {
		return pages;
	}

	public PageLayout getDefaultPage() {
		return defaultPage;
	}

	public PageLayout getPage(String name) throws InvMonException {
		loadPages();
		if (name == null)
			return defaultPage;
		for (PageLayout x : pages)
			if (name.equals(x.getId()))
				return x;
		throw new NoSuchElementException(name);
	}
}
