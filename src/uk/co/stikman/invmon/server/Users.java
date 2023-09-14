package uk.co.stikman.invmon.server;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import uk.co.stikman.invmon.InvMonException;
import uk.co.stikman.invmon.inverter.util.InvUtil;

public class Users {
	private List<User> users = new ArrayList<>();

	public void configure(Element root) throws InvMonException {
		for (Element el : InvUtil.getElements(root, "User")) {

			String name = InvUtil.getAttrib(el, "name");
			if (findByName(name) != null)
				throw new InvMonException("User [" + name + "] already defined");

			UserRole role = UserRole.PUBLIC;
			if (el.hasAttribute("role"))
				role = UserRole.valueOf(el.getAttribute("role").toUpperCase());
			User u = new User(name, InvUtil.getAttrib(el, "password"), role);
			users.add(u);
		}
	}

	public User findByName(String name) {
		return users.stream().filter(x -> name.equals(x.getName())).findFirst().orElse(null);
	}

}
