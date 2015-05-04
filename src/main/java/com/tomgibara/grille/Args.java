package com.tomgibara.grille;

import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

public class Args {

	@Parameter(names = { "--help", "-h"}, help = true, description = "Display this help")
	public boolean help;

	@Parameter(names = { "--settings" }, description = "Path to settings file")
	public String settingsPath;

	@Parameter(names = {"--order", "-o"}, description = "Size of grille", required = true, validateValueWith = OrderValidator.class)
	public Integer order;
	
	@Parameter(names = {"--seed", "-s" }, description = "Seed for grille generation", validateValueWith = SeedValidator.class)
	public Long seed;

	@Parameter(names = {"--filename", "-f" }, description = "Filename of generated image")
	public String filename;

	public static class OrderValidator implements IValueValidator<Integer> {

		@Override
		public void validate(String name, Integer order) throws ParameterException {
			if (order < 1) throw new ParameterException("Order must be greater than zero");
			if (order > 32) throw new ParameterException("Order must not exceed 32");
		}

	}

	public static class SeedValidator implements IValueValidator<Long> {

		@Override
		public void validate(String name, Long seed) throws ParameterException {
			if (seed < 0) throw new ParameterException("Seed must be positive");
			if (seed > Integer.MAX_VALUE) throw new ParameterException("Seed must not exceed " + Integer.MAX_VALUE);
		}

	}

}
