package uk.co.stikman.invmon;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import uk.co.stikman.log.DefaultLogFormat;
import uk.co.stikman.log.LogEntry;
import uk.co.stikman.log.LogFormat;
import uk.co.stikman.log.LogTarget;

public class FileLogTarget extends LogTarget {

	private OutputStream	output;
	private LogFormat		format	= new DefaultLogFormat();

	public FileLogTarget(File file) throws IOException {
		output = new BufferedOutputStream(new FileOutputStream(file.getAbsoluteFile(), true));
	}

	@Override
	public void log(LogEntry le) {
		synchronized (this) {
			String s = format.format(le);
			try {
				output.write(s.getBytes(StandardCharsets.UTF_8));
				output.write('\n');
				output.flush();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public LogFormat getFormat() {
		return format;
	}

	public void setFormat(LogFormat format) {
		this.format = format;
	}

}
