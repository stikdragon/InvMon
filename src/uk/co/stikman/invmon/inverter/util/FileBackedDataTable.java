package uk.co.stikman.invmon.inverter.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

import uk.co.stikman.table.DataTable;
import uk.co.stikman.table.DataTableImporter;

public class FileBackedDataTable {

	private File							file;
	private long							lastmod;
	private DataTable						table;
	private Consumer<FileBackedDataTable>	onReload;
	private DataTableImporter				importer;

	public FileBackedDataTable(File file) {
		this.file = file;
		this.lastmod = -1;
	}

	public synchronized DataTable getTable() {
		try {
			if (file.lastModified() != lastmod)
				reload();
			return table;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void reload() throws IOException {
		table = new DataTable();
		try (InputStream fos = new BufferedInputStream(new FileInputStream(file))) {
			importer.inport(table, fos);
		}
		lastmod = file.lastModified();

		if (onReload != null)
			onReload.accept(this);
	}

	public Consumer<FileBackedDataTable> getOnReload() {
		return onReload;
	}

	public void setOnReload(Consumer<FileBackedDataTable> onReload) {
		this.onReload = onReload;
	}

	public DataTableImporter getImporter() {
		return importer;
	}

	public void setImporter(DataTableImporter importer) {
		this.importer = importer;
	}

}
