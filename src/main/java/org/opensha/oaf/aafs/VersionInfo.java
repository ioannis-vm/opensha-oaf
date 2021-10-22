package org.opensha.oaf.aafs;


/**
 * Program version information.
 * Author: Michael Barall 08/16/2018.
 */
public class VersionInfo  {

	// Program name.

	public static final String program_name = "USGS Aftershock Forecasting System";

	// Program version.

	public static final String program_version = "Version 1.00.1363 (10/21/2021)";

	// Program sponsor.

	public static final String program_sponsor = "U.S. Geological Survey, Earthquake Science Center";

	// Major version.

	public static final int major_version = 1;

	// Minor version.

	public static final int minor_version = 0;

	// Build.

	public static final int build = 1363;




	// Get the title, as multiple lines but no final newline.

	public static String get_title () {
		return program_name + "\n"
				+ program_version + "\n"
				+ program_sponsor;
	}


	// Get a one-line name and version

	public static String get_one_line_version () {
		return program_name + ", " + program_version;
	}

}
