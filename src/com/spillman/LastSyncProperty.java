package com.spillman;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SuppressWarnings("serial")
public class LastSyncProperty extends Properties {
	private static final Logger logger = LogManager.getLogger();

	private static final String DEFAULT_LAST_SYNC_DATE	= "2015-01-01 00:00:00";
	private static final String PROP_LAST_SYNC_DATE 	= "LastSyncDate";
	private static final String PROPERTIES_FILE_NAME 	= "sync.last";
	private static final String DATE_FORMAT 			= "yyyy-MM-dd HH:mm:ss";
	
	private static SimpleDateFormat formatter = null;

	public LastSyncProperty() throws IOException {
		super();

		formatter = new SimpleDateFormat(DATE_FORMAT);

		logger.debug("Loading properties file '{}{}{}'", System.getProperty("user.dir"), File.separator, PROPERTIES_FILE_NAME);
		
		try {
			FileInputStream in = new FileInputStream(PROPERTIES_FILE_NAME);
			this.load(in);
			in.close();
		} catch (FileNotFoundException e) {
			logger.catching(e);
		}
	}

	public void save() throws IOException {
		logger.debug("Saving properties file '{}{}{}'", System.getProperty("user.dir"), File.separator, PROPERTIES_FILE_NAME);
		FileOutputStream out = new FileOutputStream(PROPERTIES_FILE_NAME);
		this.store(out, "Workfront Last Sync");
		out.close();
	}
	
	public Date getLastSyncDate() {
		String lastSyncString = this.getProperty(PROP_LAST_SYNC_DATE, DEFAULT_LAST_SYNC_DATE);
		try {
			return formatter.parse(lastSyncString);
		} catch (ParseException e) {
			logger.error("Something went wrong parsing the date string '{}'", lastSyncString);
			return null;
		}
	}
	
	public void setLastSyncDate(Date date) {
		if (date == null) {
			logger.error("Attempted to save a null as the last sync date in the properties file");
			return;
		}
		this.setProperty(PROP_LAST_SYNC_DATE, formatter.format(date));
	}
}
