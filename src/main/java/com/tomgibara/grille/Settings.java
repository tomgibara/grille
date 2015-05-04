package com.tomgibara.grille;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Properties;

public class Settings {

	private static Properties load(String path) throws IOException {
		File file = new File(path);
		if (!file.exists()) throw new IOException("Cannot read settings file: " + path);
		Properties properties = new Properties();
		properties.load(new FileInputStream(file));
		return properties;
	}

	/* Pixel density of generated image */
	public int dpi = 300;
	/* Size of individual grille squares */
	public int mmPerSquare = 8;
	/* Directory to which images should be written */
	public File outputDir = new File(".");
	/* Filename assigned to grille image */
	public String filename = "grille";
	/* Design used to mark grille positions */
	public Design design = Design.CIRCLE;
	/* Whether grille positions should be shaded */
	public boolean shaded = false;
	/* Number of attempts made to find a 'good' grille */
	public int attempts = 100000;

	public Settings() {
	}
	
	public Settings(String path) throws IOException {
		this(load(path));
	}
	
	public Settings(Properties properties) {
		if (properties == null) return;
		for (Field field : Settings.class.getFields()) {
			try {
				String name = field.getName();
				String value = properties.getProperty(name);
				if (value == null || value.isEmpty()) continue;

				Class<?> type = field.getType();
				if (type == int.class) try {
					int i = Integer.parseInt(value);
					field.set(this, i);
				} catch (NumberFormatException e) {
					System.err.println("Invalid value for " + name + ": " + value);
				} else if (type == String.class) {
					field.set(this, value);
				} else if (type == File.class) {
					File file = new File(value);
					field.set(this, file);
				} else if (type.isEnum()) try {
					Enum<?> enm = Enum.valueOf((Class) type, value.toUpperCase());
					field.set(this, enm);
				} catch (IllegalArgumentException e) {
					System.err.println("Invalid value for " + name + ": " + value);
				} else if (type == boolean.class) {
					field.set(this, Boolean.valueOf(value));
				}

			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}
	
}
