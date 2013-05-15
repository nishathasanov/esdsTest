package com.openkm.util;

import java.io.File;
import java.util.TimerTask;
import java.util.logging.Logger;

import org.apache.log4j.LogManager;
import org.apache.log4j.PropertyConfigurator;

/**
 * Automatically refresh log4j configuration.
 * Based on http://janvanbesien.blogspot.com.es/2010/02/reload-log4j-configuration-in-tomcat.html
 * 
 * @author pavila
 */
public class LoggerWatchdog extends TimerTask {
	private static Logger log = Logger.getLogger(LoggerWatchdog.class.getName());
	private static volatile boolean running = false;
	private long lastModified = 0;
	private File file = null;
	
	public LoggerWatchdog(String file) {
		this.file = new File(file);
	}
	
	public void run() {
		if (running) {
			log.warning("*** LoggerWatchdog already running ***");
		} else {
			running = true;
			log.fine("*** LoggerWatchdog activated ***");
			
			try {
				if (file.exists() && file.canRead()) {
					long l = file.lastModified();
					
					if (l > lastModified) {
						lastModified = l;
						doOnChange();
					}
				}
			} finally {
				running = false;
			}
		}
	}
	
	private void doOnChange() {
		log.info("*** Log4j configuration file changed ***");
		new PropertyConfigurator().doConfigure(file.getPath(), LogManager.getLoggerRepository());
	}
}
