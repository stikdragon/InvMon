package uk.co.stikman.invmon.tooling;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.w3c.dom.Element;

import uk.co.stikman.invmon.InvMonHTTPRequest;
import uk.co.stikman.invmon.client.InvMon;
import uk.co.stikman.invmon.inverter.util.InvUtil;
import uk.co.stikman.invmon.nanohttpd.NanoHTTPD;
import uk.co.stikman.invmon.nanohttpd.NanoHTTPD.Response.Status;
import uk.co.stikman.invmon.server.InvMonHTTPResponse;
import uk.co.stikman.invmon.server.UserSesh;

public class DevMode {

	private File				root;
	private long				lastMod			= 0;
	private File	outputJS	= null;

	public DevMode(Element root) {
		this.root = new File(InvUtil.getAttrib(root, "root"));
	}

	/**
	 * in dev mode we'll intercept classes.js to recompile it if necessary
	 * 
	 * @param url
	 * @param sesh
	 * @param session
	 * @return
	 * @throws Exception
	 */
	public InvMonHTTPResponse serve(String url, UserSesh sesh, InvMonHTTPRequest session) throws Exception {
		synchronized (this) {
			List<File> lst = InvUtil.getAllFiles(root, ".class");
			if (lst.isEmpty())
				throw new Exception("No files found for DevMode");
			long m = lst.stream().mapToLong(x -> x.lastModified()).max().getAsLong();
			if (m > lastMod) {
				recompile();
				lastMod = m;
			}

			InputStream is = new FileInputStream(outputJS);
			return new InvMonHTTPResponse(Status.OK, NanoHTTPD.getMimeTypeForFile(url), is, outputJS.length());

		}
	}

	private void recompile() throws Exception {
		//
		// run teavm compiler and output to temp file
		//
		Path dir = Files.createTempDirectory("invmonteavm");
		System.out.println("Building to temp: " + dir.toString());
		Args args = new Args(new String[] { "-mainclass", InvMon.class.getName(), "-output", dir.toString(), "-obfuscatedJS", "false" });
		new Builder(args).compile();
		outputJS = dir.resolve("classes.js").toFile();
	}

}
