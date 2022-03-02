package org.opensha.oaf.oetas;


// Holds constants used by Operational ETAS.
// Author: Michael Barall 11/27/2019.

public class OEConstants {

	// The number of milliseconds in a day.

	public static final double C_MILLIS_PER_DAY = 86400000.0;
	public static final long L_MILLIS_PER_DAY = 86400000L;

	// The number of milliseconds in a week.
	// Note: A week is defined to be 7 days.

	public static final double C_MILLIS_PER_WEEK = 604800000.0;
	public static final long L_MILLIS_PER_WEEK = 604800000L;

	// The number of milliseconds in a month.
	// Note: A month is defined to be 30 days.

	public static final double C_MILLIS_PER_MONTH = 2592000000.0;
	public static final long L_MILLIS_PER_MONTH = 2592000000L;

	// The number of milliseconds in a year.
	// Note: A year is defined to be 365 days.

	public static final double C_MILLIS_PER_YEAR = 31536000000.0;
	public static final long L_MILLIS_PER_YEAR = 31536000000L;

	// The natural logarithm of 10.

	public static final double C_LOG_10 = 2.3025850929940457;

	// A postive magnitude larger than any possible magnitude.
	// Note: Chosen to be exactly representable in typical floating point
	// implementations, and in the encoding of CompactEqkRupList.

	public static final double NO_MAG_POS = 11.875;

	// Use x >= NO_MAG_POS_CHECK to check if x contains NO_MAG_POS.

	public static final double NO_MAG_POS_CHECK = 11.75;

	// A negative magnitude smaller than any possible magnitude.
	// Note: Chosen to be exactly representable in typical floating point
	// implementations, and in the encoding of CompactEqkRupList.

	public static final double NO_MAG_NEG = -11.875;

	// Use x <= NO_MAG_NEG_CHECK to check if x contains NO_MAG_NEG.

	public static final double NO_MAG_NEG_CHECK = -11.75;

	// A time value or difference much larger than any actual value, in days.
	// (But still much smaller than the range of double.)

	public static final double HUGE_TIME_DAYS = 1.0e+20;

	// Use x >= HUGE_TIME_DAYS_CHECK to check if x contains HUGE_TIME_DAYS.

	public static final double HUGE_TIME_DAYS_CHECK = 0.99e+20;

	// The logarithm base 10 of HUGE_TIME_DAYS.

	public static final double LOG10_HUGE_TIME_DAYS = 20;

	// A duration so extremely small it is considered to be zero.
	// Note: This is used to avoid divide-by-zero in formulas that have a
	// duration in the denominator; it does not mean that durations larger
	// than this are considered significant.
	
	public static final double TINY_DURATION_DAYS = 1.0e-150;

	// An Omori rate so extremely small it is considered to be zero.
	// Note: This is used to avoid divide-by-zero in formulas that have an
	// Omori rate in the denominator; it does not mean that rates larger
	// than this are considered significant.
	
	public static final double TINY_OMORI_RATE = 1.0e-150;

	// An expected count of earthquakes small enough to treat as zero.

	public static final double SMALL_EXPECTED_COUNT = 0.001;

	// Methods for infilling event counts below the simulated magnitude range.

	public static final int INFILL_METH_MIN			= 1;
	public static final int INFILL_METH_NONE		= 1;	// No infill, use exactly as simulated
	public static final int INFILL_METH_SCALE		= 2;	// Scale up, using G-R relation
	public static final int INFILL_METH_POISSON		= 3;	// Use Poisson random var applied to expected rate
	public static final int INFILL_METH_STERILE		= 4;	// Generate sterile ruptures
	public static final int INFILL_METH_MAX			= 4;

	// Return a string describing the infill method.

	public static String get_infill_method_as_string (int infill_meth) {
		switch (infill_meth) {
		case INFILL_METH_NONE: return "INFILL_METH_NONE";
		case INFILL_METH_SCALE: return "INFILL_METH_SCALE";
		case INFILL_METH_POISSON: return "INFILL_METH_POISSON";
		case INFILL_METH_STERILE: return "INFILL_METH_STERILE";
		}
		return "INFILL_METH_INVALID(" + infill_meth + ")";
	}

	// Methods for magfilling event counts outside the simulated magnitude range.

	public static final int MAGFILL_METH_MIN			= 1;
	public static final int MAGFILL_METH_NONE			= 1;	// No magfill, use exactly as simulated
	public static final int MAGFILL_METH_PDF_ONLY		= 2;	// Use probability distribution function with expected rate over entire mag range
	public static final int MAGFILL_METH_PDF_HYBRID		= 3;	// Combine counts with a pdf derived from expected rate outside mag range
	public static final int MAGFILL_METH_PDF_STERILE	= 4;	// Combine counts, sterile ruptures, and a pdf derived from expected rate above mag range
	public static final int MAGFILL_METH_MAX			= 4;

	// Return a string describing the magfill method.

	public static String get_magfill_method_as_string (int magfill_meth) {
		switch (magfill_meth) {
		case MAGFILL_METH_NONE: return "MAGFILL_METH_NONE";
		case MAGFILL_METH_PDF_ONLY: return "MAGFILL_METH_PDF_ONLY";
		case MAGFILL_METH_PDF_HYBRID: return "MAGFILL_METH_PDF_HYBRID";
		case MAGFILL_METH_PDF_STERILE: return "MAGFILL_METH_PDF_STERILE";
		}
		return "MAGFILL_METH_INVALID(" + magfill_meth + ")";
	}

	// Methods for outfilling event counts outside the simulated time range.

	public static final int OUTFILL_METH_MIN		= 1;
	public static final int OUTFILL_METH_NONE		= 1;	// No outfill, use exactly as simulated, zero-filling after end of simulation
	public static final int OUTFILL_METH_OMIT		= 2;	// Do not participate in probability distributions after end of simulation
	public static final int OUTFILL_METH_PDF_DIRECT	= 3;	// Use pdf derived from expected rate of direct aftershocks
	public static final int OUTFILL_METH_MAX		= 3;

	// Return a string describing the outfill method.

	public static String get_outfill_method_as_string (int outfill_meth) {
		switch (outfill_meth) {
		case OUTFILL_METH_NONE: return "OUTFILL_METH_NONE";
		case OUTFILL_METH_OMIT: return "OUTFILL_METH_OMIT";
		case OUTFILL_METH_PDF_DIRECT: return "OUTFILL_METH_PDF_DIRECT";
		}
		return "OUTFILL_METH_INVALID(" + outfill_meth + ")";
	}

	// Methods for selecting which catalogs are sufficiently long and determining their time range..

	public static final int CATLEN_METH_MIN			= 1;
	public static final int CATLEN_METH_ANY			= 1;	// Any catalog length is acceptable
	public static final int CATLEN_METH_ANY_CLIP	= 2;	// Any catalog length is acceptable, and clip to a full time bin
	public static final int CATLEN_METH_RANGE		= 3;	// Catalog must cover all active time bins
	public static final int CATLEN_METH_ENTIRE		= 4;	// Catalog must have reached simulation end time
	public static final int CATLEN_METH_ENTIRE_CLIP	= 5;	// Catalog must have reached simulation end time, and clip to a full time bin
	public static final int CATLEN_METH_MAX			= 5;

	// Return a string describing the catlen method.

	public static String get_catlen_method_as_string (int catlen_meth) {
		switch (catlen_meth) {
		case CATLEN_METH_ANY: return "CATLEN_METH_ANY";
		case CATLEN_METH_ANY_CLIP: return "CATLEN_METH_ANY_CLIP";
		case CATLEN_METH_RANGE: return "CATLEN_METH_RANGE";
		case CATLEN_METH_ENTIRE: return "CATLEN_METH_ENTIRE";
		case CATLEN_METH_ENTIRE_CLIP: return "CATLEN_METH_ENTIRE_CLIP";
		}
		return "CATLEN_METH_INVALID(" + catlen_meth + ")";
	}

	// Combine catalog length, outfill, and magfill methods into a single rate accumulation method.
	// The combined form is three digits: catlen, outfill, magfill.

	public static int make_rate_acc_meth (int catlen_meth, int outfill_meth, int magfill_meth) {
		if (!( catlen_meth >= CATLEN_METH_MIN && catlen_meth <= CATLEN_METH_MAX )) {
			throw new IllegalArgumentException ("OEConstants.make_rate_acc_meth: Invalid catalog length method: catlen_meth = " + catlen_meth);
		}
		if (!( outfill_meth >= OUTFILL_METH_MIN && outfill_meth <= OUTFILL_METH_MAX )) {
			throw new IllegalArgumentException ("OEConstants.make_rate_acc_meth: Invalid outfill method: outfill_meth = " + outfill_meth);
		}
		if (!( magfill_meth >= MAGFILL_METH_MIN && magfill_meth <= MAGFILL_METH_MAX )) {
			throw new IllegalArgumentException ("OEConstants.make_rate_acc_meth: Invalid magnitude fill method: magfill_meth = " + magfill_meth);
		}
		int rate_acc_meth = (catlen_meth * 100) + (outfill_meth * 10) + magfill_meth;
		return rate_acc_meth;
	}

	// Extract catalog length, outfill, and magfill methods from a single rate accumulation method.

	public static int extract_catlen_from_rate_acc (int rate_acc_meth) {
		int catlen_meth = rate_acc_meth / 100;
		if (!( catlen_meth >= CATLEN_METH_MIN && catlen_meth <= CATLEN_METH_MAX )) {
			throw new IllegalArgumentException ("OEConstants.extract_catlen_from_rate_acc: Invalid catalog length method: rate_acc_meth = " + rate_acc_meth + ", catlen_meth = " + catlen_meth);
		}
		return catlen_meth;
	}

	public static int extract_outfill_from_rate_acc (int rate_acc_meth) {
		int outfill_meth = (rate_acc_meth / 10) % 10;
		if (!( outfill_meth >= OUTFILL_METH_MIN && outfill_meth <= OUTFILL_METH_MAX )) {
			throw new IllegalArgumentException ("OEConstants.extract_outfill_from_rate_acc: Invalid outfill method: rate_acc_meth = " + rate_acc_meth + ", outfill_meth = " + outfill_meth);
		}
		return outfill_meth;
	}

	public static int extract_magfill_from_rate_acc (int rate_acc_meth) {
		int magfill_meth = rate_acc_meth % 10;
		if (!( magfill_meth >= MAGFILL_METH_MIN && magfill_meth <= MAGFILL_METH_MAX )) {
			throw new IllegalArgumentException ("OEConstants.extract_magfill_from_rate_acc: Invalid magnitude fill method: rate_acc_meth = " + rate_acc_meth + ", magfill_meth = " + magfill_meth);
		}
		return magfill_meth;
	}

	// Validate rate accumulation method.
	// Return true if valid, false if invalid.

	public static boolean validate_rate_acc_meth (int rate_acc_meth) {
		int catlen_meth = rate_acc_meth / 100;
		if (!( catlen_meth >= CATLEN_METH_MIN && catlen_meth <= CATLEN_METH_MAX )) {
			return false;
		}
		int outfill_meth = (rate_acc_meth / 10) % 10;
		if (!( outfill_meth >= OUTFILL_METH_MIN && outfill_meth <= OUTFILL_METH_MAX )) {
			return false;
		}
		int magfill_meth = rate_acc_meth % 10;
		if (!( magfill_meth >= MAGFILL_METH_MIN && magfill_meth <= MAGFILL_METH_MAX )) {
			return false;
		}
		return true;
	}

	// Negligably small time interval, in days, when generating catalogs.

	public static final double GEN_TIME_EPS = 0.00001;

	// Negligably small magnitude interval, when generating catalogs.

	public static final double GEN_MAG_EPS = 0.0002;

	// Negligably small time interval, in days, when fitting parameters.

	public static final double FIT_TIME_EPS = 1.0e-7;

	// Negligably small magnitude interval, when fitting parameters.

	public static final double FIT_MAG_EPS = 0.001;

	// Default maximum number of generations, when generating catalogs.

	public static final int DEF_MAX_GEN_COUNT = 100;

	// Default maximum number of ruptures in a catalog, when generating catalogs; 0 if none.

	public static final int DEF_MAX_CAT_SIZE = 5000000;

	// Default magnitude excess for selecting stop time, when generating catalogs; 0.0 if none.

	public static final double DEF_MAG_EXCESS = 4.0;

	// Catalog result codes.

	public static final int CAT_RESULT_MIN = 0;				// Must be 0 so these can be used as array indexes
	public static final int CAT_RESULT_OK = 0;				// Success
	public static final int CAT_RESULT_EARLY_STOP = 1;		// Success, but catalog stopped before end time
	public static final int CAT_RESULT_CAT_TOO_LARGE = 2;	// Catalog is too large
	public static final int CAT_RESULT_TOO_MANY_GEN = 3;	// Catalog has too many generations
	public static final int CAT_RESULT_GEN_TOO_LARGE = 4;	// Generation is too large
	public static final int CAT_RESULT_MAX = 4;

	// Return true if the result code indicates success

	public static boolean is_cat_result_success (int cat_result) {
		switch (cat_result) {
		case CAT_RESULT_OK:
		case CAT_RESULT_EARLY_STOP:
			return true;
		}
		return false;
	}

	// Return a string identifying the result code

	public static String cat_result_to_string (int cat_result) {
		switch (cat_result) {
		case CAT_RESULT_OK: return "CAT_RESULT_OK";
		case CAT_RESULT_EARLY_STOP: return "CAT_RESULT_EARLY_STOP";
		case CAT_RESULT_CAT_TOO_LARGE: return "CAT_RESULT_CAT_TOO_LARGE";
		case CAT_RESULT_TOO_MANY_GEN: return "CAT_RESULT_TOO_MANY_GEN";
		case CAT_RESULT_GEN_TOO_LARGE: return "CAT_RESULT_GEN_TOO_LARGE";
		}
		return "CAT_RESULT_INVALID(" + cat_result + ")";
	}

	// Options for common Helmstetter parameters.

	public static final int HELM_PARAM_MIN   = 1;
	public static final int HELM_PARAM_NONE  = 1;	// No Helmstetter incompleteness
	public static final int HELM_PARAM_WORLD = 2;	// World values: F = 0.50, G = 0.25, H = 1.00
	public static final int HELM_PARAM_CAL   = 3;	// California values: F = 1.00, G = 4.50, H = 0.75
	public static final int HELM_PARAM_MAX   = 3;

	// Functions to obtain Helmstetter parameters.

	public static double helm_capF (int helm_param) {
		double capF;
		switch (helm_param) {
		default:
			throw new IllegalArgumentException ("OEConstants.helm_capF: Invalid Helmstetter option: helm_param = " + helm_param);
		case HELM_PARAM_NONE:
			capF = 0.50;
			break;
		case HELM_PARAM_WORLD:
			capF = 0.50;
			break;
		case HELM_PARAM_CAL:
			capF = 1.00;
			break;
		}
		return capF;
	}

	public static double helm_capG (int helm_param) {
		double capG;
		switch (helm_param) {
		default:
			throw new IllegalArgumentException ("OEConstants.helm_capG: Invalid Helmstetter option: helm_param = " + helm_param);
		case HELM_PARAM_NONE:
			capG = 100.0;
			break;
		case HELM_PARAM_WORLD:
			capG = 0.25;
			break;
		case HELM_PARAM_CAL:
			capG = 4.50;
			break;
		}
		return capG;
	}

	public static double helm_capH (int helm_param) {
		double capH;
		switch (helm_param) {
		default:
			throw new IllegalArgumentException ("OEConstants.helm_capH: Invalid Helmstetter option: helm_param = " + helm_param);
		case HELM_PARAM_NONE:
			capH = 1.00;
			break;
		case HELM_PARAM_WORLD:
			capH = 1.00;
			break;
		case HELM_PARAM_CAL:
			capH = 0.75;
			break;
		}
		return capH;
	}

}
