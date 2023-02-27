package uk.co.stikman.invmon.client.res;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import uk.co.stikman.invmon.htmlout.NotFoundException;

public class ClientRes {
	private long	size;
	private String	name;

	public static ClientRes get(String name) throws NotFoundException {
		try (InputStream is = ClientRes.class.getResourceAsStream(name)) {
			if (is == null)
				throw new NotFoundException(name);
			ClientRes r = new ClientRes();
			r.name = name;
			r.size = getLength(is);
			return r;
		} catch (IOException e) {
			throw new NotFoundException(name);
		}
	}

	private static long getLength(InputStream is) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] arr = new byte[4096];
		for (int n; (n = is.read(arr)) != -1;)
			baos.write(arr, 0, n);
		return baos.size();
	}

	public long getSize() {
		return size;
	}

	public String getName() {
		return name;
	}
	
	public InputStream makeStream() {
		return ClientRes.class.getResourceAsStream(name);
	}

}
