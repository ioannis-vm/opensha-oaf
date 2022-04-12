package org.opensha.oaf.oetas;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalImpJsonReader;
import org.opensha.oaf.util.MarshalImpJsonWriter;
import org.opensha.oaf.util.MarshalException;

import org.opensha.oaf.comcat.GeoJsonUtils;


// Class to store parameters for simulating operational ETAS catalog.
// Author: Michael Barall 03/14/2022.
//
// Holds the parameters for overall top-level control of simulating a
// suite of realizations of ETAS catalogs.

public class OESimulationParams {

	//----- Parameters -----

	//--- Simulation

	// The number of catalogs to generate, for simulations.

	public int sim_num_catalogs;

	// The minimum acceptable number of catalogs, for simulations.

	public int sim_min_num_catalogs;

	// The time limit for generating catalogs, for simulations, in milliseconds; or -1L for no limit.

	public long sim_max_runtime;

	// The time interval for progress messages, for simulations, in milliseconds; of -1L for none.

	public long sim_progress_time;

	// The accumulator to use, for simulations.  (See OEConstants.SEL_ACCUM_XXXX.)

	public int sim_accum_selection;

	// Accumulator options, for simulations.

	public int sim_accum_option;

	// Accumulator additional parameter 1, for simulations.
	// For OEAccumRateTimeMag, it is the proportional reduction (0.0 to 1.0) to apply to secondary productivity when computing upfill.

	public double sim_accum_param_1;

	//--- Ranging

	// The number of catalogs to generate, for ranging.

	public int range_num_catalogs;

	// The minimum acceptable number of catalogs, for ranging.

	public int range_min_num_catalogs;

	// The time limit for generating catalogs, for ranging, in milliseconds; or -1L for no limit.

	public long range_max_runtime;

	// The time interval for progress messages, for ranging, in milliseconds; of -1L for none.

	public long range_progress_time;

	// The accumulator to use, for ranging.  (See OEConstants.SEL_ACCUM_XXXX.)

	public int range_accum_selection;

	// Accumulator options, for ranging.

	public int range_accum_option;

	// Minimum relative magnitude for ranging, relative to mainshock or scaling magnitude.

	public double range_min_rel_mag;

	// Maximum relative magnitude for ranging, relative to mainshock or scaling magnitude.

	public double range_max_rel_mag;

	// Allowable fraction of catalogs to exceed max mag, to determine duration, for ranging.  Must be > 0.0.

	public double range_exceed_fraction;

	// Target catalog size, for ranging.

	public int range_target_size;

	// Target catalog fractile (the fractile that is targeted to be range_target_size), for ranging.

	public double range_target_fractile;

	// Minimum catalog duration, in days, for ranging.

	public double range_min_duration;

	// Maximum number of ranging attempts.  Must be > 0.

	public int range_max_attempts;

	// Fraction of catalogs to exceed top mag, to determine simulation max mag, for ranging.
	// Can be zero to indicate max mag should not be adjusted.

	public double range_mag_lim_fraction;

	// Time at which top mag is checked, in days, to determine simulation max mag, for ranging.

	public double range_mag_lim_time;




	//----- Construction -----




	// Clear to default values.

	public final void clear () {
		sim_num_catalogs       = 0;
		sim_min_num_catalogs   = 0;
		sim_max_runtime        = 0L;
		sim_progress_time      = 0L;
		sim_accum_selection    = 0;
		sim_accum_option       = 0;
		sim_accum_param_1      = 0.0;
		range_num_catalogs     = 0;
		range_min_num_catalogs = 0;
		range_max_runtime      = 0L;
		range_progress_time    = 0L;
		range_accum_selection  = 0;
		range_accum_option     = 0;
		range_min_rel_mag      = 0.0;
		range_max_rel_mag      = 0.0;
		range_exceed_fraction  = 0.0;
		range_target_size      = 0;
		range_target_fractile  = 0.0;
		range_min_duration     = 0.0;
		range_max_attempts     = 0;
		range_mag_lim_fraction = 0.0;
		range_mag_lim_time     = 0.0;
		return;
	}




	// Default constructor.

	public OESimulationParams () {
		clear();
	}




	// Set all values.

	public final OESimulationParams set (
		int    sim_num_catalogs      ,
		int    sim_min_num_catalogs  ,
		long   sim_max_runtime       ,
		long   sim_progress_time     ,
		int    sim_accum_selection   ,
		int    sim_accum_option      ,
		double sim_accum_param_1     ,
		int    range_num_catalogs    ,
		int    range_min_num_catalogs,
		long   range_max_runtime     ,
		long   range_progress_time   ,
		int    range_accum_selection ,
		int    range_accum_option    ,
		double range_min_rel_mag     ,
		double range_max_rel_mag     ,
		double range_exceed_fraction ,
		int    range_target_size     ,  
		double range_target_fractile ,
		double range_min_duration    ,
		int    range_max_attempts    ,  
		double range_mag_lim_fraction,
		double range_mag_lim_time
	) {
		this.sim_num_catalogs       = sim_num_catalogs      ;
		this.sim_min_num_catalogs   = sim_min_num_catalogs  ;
		this.sim_max_runtime        = sim_max_runtime       ;
		this.sim_progress_time      = sim_progress_time     ;
		this.sim_accum_selection    = sim_accum_selection   ;
		this.sim_accum_option       = sim_accum_option      ;
		this.sim_accum_param_1      = sim_accum_param_1     ;
		this.range_num_catalogs     = range_num_catalogs    ;
		this.range_min_num_catalogs = range_min_num_catalogs;
		this.range_max_runtime      = range_max_runtime     ;
		this.range_progress_time    = range_progress_time   ;
		this.range_accum_selection  = range_accum_selection ;
		this.range_accum_option     = range_accum_option    ;
		this.range_min_rel_mag      = range_min_rel_mag     ;
		this.range_max_rel_mag      = range_max_rel_mag     ;
		this.range_exceed_fraction  = range_exceed_fraction ;
		this.range_target_size      = range_target_size     ;
		this.range_target_fractile  = range_target_fractile ;
		this.range_min_duration     = range_min_duration    ;
		this.range_max_attempts     = range_max_attempts    ;
		this.range_mag_lim_fraction = range_mag_lim_fraction;
		this.range_mag_lim_time     = range_mag_lim_time    ;
		return this;
	}




	// Copy all values from the other object.

	public final OESimulationParams copy_from (OESimulationParams other) {
		this.sim_num_catalogs       = other.sim_num_catalogs      ;
		this.sim_min_num_catalogs   = other.sim_min_num_catalogs  ;
		this.sim_max_runtime        = other.sim_max_runtime       ;
		this.sim_progress_time      = other.sim_progress_time     ;
		this.sim_accum_selection    = other.sim_accum_selection   ;
		this.sim_accum_option       = other.sim_accum_option      ;
		this.sim_accum_param_1      = other.sim_accum_param_1     ;
		this.range_num_catalogs     = other.range_num_catalogs    ;
		this.range_min_num_catalogs = other.range_min_num_catalogs;
		this.range_max_runtime      = other.range_max_runtime     ;
		this.range_progress_time    = other.range_progress_time   ;
		this.range_accum_selection  = other.range_accum_selection ;
		this.range_accum_option     = other.range_accum_option    ;
		this.range_min_rel_mag      = other.range_min_rel_mag     ;
		this.range_max_rel_mag      = other.range_max_rel_mag     ;
		this.range_exceed_fraction  = other.range_exceed_fraction ;
		this.range_target_size      = other.range_target_size     ;
		this.range_target_fractile  = other.range_target_fractile ;
		this.range_min_duration     = other.range_min_duration    ;
		this.range_max_attempts     = other.range_max_attempts    ;
		this.range_mag_lim_fraction = other.range_mag_lim_fraction;
		this.range_mag_lim_time     = other.range_mag_lim_time    ;
		return this;
	}




	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("OESimulationParams:" + "\n");

		result.append ("sim_num_catalogs = "       + sim_num_catalogs       + "\n");
		result.append ("sim_min_num_catalogs = "   + sim_min_num_catalogs   + "\n");
		result.append ("sim_max_runtime = "        + sim_max_runtime        + "\n");
		result.append ("sim_progress_time = "      + sim_progress_time      + "\n");
		result.append ("sim_accum_selection = "    + sim_accum_selection    + "\n");
		result.append ("sim_accum_option = "       + sim_accum_option       + "\n");
		result.append ("sim_accum_param_1 = "      + sim_accum_param_1      + "\n");
		result.append ("range_num_catalogs = "     + range_num_catalogs     + "\n");
		result.append ("range_min_num_catalogs = " + range_min_num_catalogs + "\n");
		result.append ("range_max_runtime = "      + range_max_runtime      + "\n");
		result.append ("range_progress_time = "    + range_progress_time    + "\n");
		result.append ("range_accum_selection = "  + range_accum_selection  + "\n");
		result.append ("range_accum_option = "     + range_accum_option     + "\n");
		result.append ("range_min_rel_mag = "      + range_min_rel_mag      + "\n");
		result.append ("range_max_rel_mag = "      + range_max_rel_mag      + "\n");
		result.append ("range_exceed_fraction = "  + range_exceed_fraction  + "\n");
		result.append ("range_target_size = "      + range_target_size      + "\n");
		result.append ("range_target_fractile = "  + range_target_fractile  + "\n");
		result.append ("range_min_duration = "     + range_min_duration     + "\n");
		result.append ("range_max_attempts = "     + range_max_attempts     + "\n");
		result.append ("range_mag_lim_fraction = " + range_mag_lim_fraction + "\n");
		result.append ("range_mag_lim_time = "     + range_mag_lim_time     + "\n");

		return result.toString();
	}




	// Set to typical values.
	// Parameters:
	//  f_prod = True for production values, false for development values.

	public final OESimulationParams set_to_typical (boolean f_prod) {
		if (f_prod) {
			sim_num_catalogs       = 50000;
			sim_min_num_catalogs   = 25000;
			sim_max_runtime        = 75000L;
			sim_progress_time      = 10000L;
			sim_accum_selection    = OEConstants.SEL_ACCUM_RATE_TIME_MAG;
			sim_accum_option       = OEConstants.make_rate_acc_meth (
			                             OEConstants.CATLEN_METH_ENTIRE,
								         OEConstants.OUTFILL_METH_PDF_DIRECT,
								         OEConstants.MAGFILL_METH_PDF_HYBRID);	// 433
			sim_accum_param_1      = 0.5;
			range_num_catalogs     = 5000;
			range_min_num_catalogs = 2500;
			range_max_runtime      = 15000L;
			range_progress_time    = 10000L;
			range_accum_selection  = OEConstants.SEL_ACCUM_SIM_RANGING;
			range_accum_option     = 0;
			range_min_rel_mag      = -4.0;
			range_max_rel_mag      = 0.0;
			range_exceed_fraction  = 0.15;
			range_target_size      = 4000;
			range_target_fractile  = 0.60;
			range_min_duration     = 14.0;
			range_max_attempts     = 8;
			range_mag_lim_fraction = 0.02;
			range_mag_lim_time     = 10.0;
		} else {
			sim_num_catalogs       = 20000;
			sim_min_num_catalogs   = 10000;
			sim_max_runtime        = 180000L;
			sim_progress_time      = 10000L;
			sim_accum_selection    = OEConstants.SEL_ACCUM_RATE_TIME_MAG;
			sim_accum_option       = OEConstants.make_rate_acc_meth (
			                             OEConstants.CATLEN_METH_ENTIRE,
								         OEConstants.OUTFILL_METH_PDF_DIRECT,
								         OEConstants.MAGFILL_METH_PDF_HYBRID);	// 433
			sim_accum_param_1      = 0.5;
			range_num_catalogs     = 2000;
			range_min_num_catalogs = 1000;
			range_max_runtime      = 30000L;
			range_progress_time    = 10000L;
			range_accum_selection  = OEConstants.SEL_ACCUM_SIM_RANGING;
			range_accum_option     = 0;
			range_min_rel_mag      = -4.0;
			range_max_rel_mag      = 0.0;
			range_exceed_fraction  = 0.15;
			range_target_size      = 4000;
			range_target_fractile  = 0.60;
			range_min_duration     = 14.0;
			range_max_attempts     = 8;
			range_mag_lim_fraction = 0.02;
			range_mag_lim_time     = 10.0;
		}
		return this;
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 102001;

	private static final String M_VERSION_NAME = "OESimulationParams";

	// Marshal object, internal.

	private void do_marshal (MarshalWriter writer) {

		// Version

		int ver = MARSHAL_VER_1;

		writer.marshalInt (M_VERSION_NAME, ver);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			writer.marshalInt    ("sim_num_catalogs"       , sim_num_catalogs      );
			writer.marshalInt    ("sim_min_num_catalogs"   , sim_min_num_catalogs  );
			writer.marshalLong   ("sim_max_runtime"        , sim_max_runtime       );
			writer.marshalLong   ("sim_progress_time"      , sim_progress_time     );
			writer.marshalInt    ("sim_accum_selection"    , sim_accum_selection   );
			writer.marshalInt    ("sim_accum_option"       , sim_accum_option      );
			writer.marshalDouble ("sim_accum_param_1"      , sim_accum_param_1     );
			writer.marshalInt    ("range_num_catalogs"     , range_num_catalogs    );
			writer.marshalInt    ("range_min_num_catalogs" , range_min_num_catalogs);
			writer.marshalLong   ("range_max_runtime"      , range_max_runtime     );
			writer.marshalLong   ("range_progress_time"    , range_progress_time   );
			writer.marshalInt    ("range_accum_selection"  , range_accum_selection );
			writer.marshalInt    ("range_accum_option"     , range_accum_option    );
			writer.marshalDouble ("range_min_rel_mag"      , range_min_rel_mag     );
			writer.marshalDouble ("range_max_rel_mag"      , range_max_rel_mag     );
			writer.marshalDouble ("range_exceed_fraction"  , range_exceed_fraction );
			writer.marshalInt    ("range_target_size"      , range_target_size     );
			writer.marshalDouble ("range_target_fractile"  , range_target_fractile );
			writer.marshalDouble ("range_min_duration"     , range_min_duration    );
			writer.marshalInt    ("range_max_attempts"     , range_max_attempts    );
			writer.marshalDouble ("range_mag_lim_fraction" , range_mag_lim_fraction);
			writer.marshalDouble ("range_mag_lim_time"     , range_mag_lim_time    );

		}
		break;

		}

		return;
	}

	// Unmarshal object, internal.

	private void do_umarshal (MarshalReader reader) {
	
		// Version

		int ver = reader.unmarshalInt (M_VERSION_NAME, MARSHAL_VER_1, MARSHAL_VER_1);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			sim_num_catalogs       = reader.unmarshalInt    ("sim_num_catalogs"      );
			sim_min_num_catalogs   = reader.unmarshalInt    ("sim_min_num_catalogs"  );
			sim_max_runtime        = reader.unmarshalLong   ("sim_max_runtime"       );
			sim_progress_time      = reader.unmarshalLong   ("sim_progress_time"     );
			sim_accum_selection    = reader.unmarshalInt    ("sim_accum_selection"   );
			sim_accum_option       = reader.unmarshalInt    ("sim_accum_option"      );
			sim_accum_param_1      = reader.unmarshalDouble ("sim_accum_param_1"     );
			range_num_catalogs     = reader.unmarshalInt    ("range_num_catalogs"    );
			range_min_num_catalogs = reader.unmarshalInt    ("range_min_num_catalogs");
			range_max_runtime      = reader.unmarshalLong   ("range_max_runtime"     );
			range_progress_time    = reader.unmarshalLong   ("range_progress_time"   );
			range_accum_selection  = reader.unmarshalInt    ("range_accum_selection" );
			range_accum_option     = reader.unmarshalInt    ("range_accum_option"    );
			range_min_rel_mag      = reader.unmarshalDouble ("range_min_rel_mag"     );
			range_max_rel_mag      = reader.unmarshalDouble ("range_max_rel_mag"     );
			range_exceed_fraction  = reader.unmarshalDouble ("range_exceed_fraction" );
			range_target_size      = reader.unmarshalInt    ("range_target_size"     );
			range_target_fractile  = reader.unmarshalDouble ("range_target_fractile" );
			range_min_duration     = reader.unmarshalDouble ("range_min_duration"    );
			range_max_attempts     = reader.unmarshalInt    ("range_max_attempts"    );
			range_mag_lim_fraction = reader.unmarshalDouble ("range_mag_lim_fraction");
			range_mag_lim_time     = reader.unmarshalDouble ("range_mag_lim_time"    );

		}
		break;

		}

		return;
	}

	// Marshal object.

	public void marshal (MarshalWriter writer, String name) {
		writer.marshalMapBegin (name);
		do_marshal (writer);
		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal object.

	public OESimulationParams unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object.

	public static void static_marshal (MarshalWriter writer, String name, OESimulationParams catalog) {
		writer.marshalMapBegin (name);
		catalog.do_marshal (writer);
		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal object.

	public static OESimulationParams static_unmarshal (MarshalReader reader, String name) {
		OESimulationParams catalog = new OESimulationParams();
		reader.unmarshalMapBegin (name);
		catalog.do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return catalog;
	}




	//----- Testing -----




	// Check if two catalog parameter structures are identical.
	// Note: This is primarily for testing.

	public final boolean check_param_equal (OESimulationParams other) {
		if (
			   this.sim_num_catalogs       == other.sim_num_catalogs
			&& this.sim_min_num_catalogs   == other.sim_min_num_catalogs
			&& this.sim_max_runtime        == other.sim_max_runtime
			&& this.sim_progress_time      == other.sim_progress_time
			&& this.sim_accum_selection    == other.sim_accum_selection
			&& this.sim_accum_option       == other.sim_accum_option
			&& this.sim_accum_param_1      == other.sim_accum_param_1
			&& this.range_num_catalogs     == other.range_num_catalogs
			&& this.range_min_num_catalogs == other.range_min_num_catalogs
			&& this.range_max_runtime      == other.range_max_runtime
			&& this.range_progress_time    == other.range_progress_time
			&& this.range_accum_selection  == other.range_accum_selection
			&& this.range_accum_option     == other.range_accum_option
			&& this.range_min_rel_mag      == other.range_min_rel_mag
			&& this.range_max_rel_mag      == other.range_max_rel_mag
			&& this.range_exceed_fraction  == other.range_exceed_fraction
			&& this.range_target_size      == other.range_target_size
			&& this.range_target_fractile  == other.range_target_fractile
			&& this.range_min_duration     == other.range_min_duration
			&& this.range_max_attempts     == other.range_max_attempts
			&& this.range_mag_lim_fraction == other.range_mag_lim_fraction
			&& this.range_mag_lim_time     == other.range_mag_lim_time
		) {
			return true;
		}
		return false;
	}




	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("OESimulationParams : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1  f_prod
		// Construct typical parameters for either production or development, and display it.
		// Marshal to JSON and display JSON text, then unmarshal and display the results.

		if (args[0].equalsIgnoreCase ("test1")) {

			// 1 additional arguments

			if (args.length != 2) {
				System.err.println ("OESimulationParams : Invalid 'test1' subcommand");
				return;
			}

			try {

				boolean f_prod = Boolean.parseBoolean (args[1]);

				// Say hello

				System.out.println ("Constructing and displaying simulation parameters");
				System.out.println ("f_prod: " + f_prod);
				System.out.println ();

				// Create the parameters

				OESimulationParams sim_params = new OESimulationParams();
				sim_params.set_to_typical (f_prod);

				// Display the contents

				System.out.println ();
				System.out.println ("********** Simulation Parameters Display **********");
				System.out.println ();

				System.out.println (sim_params.toString());

				// Marshal to JSON

				System.out.println ();
				System.out.println ("********** Marshal to JSON **********");
				System.out.println ();

				MarshalImpJsonWriter store = new MarshalImpJsonWriter();
				OESimulationParams.static_marshal (store, null, sim_params);
				store.check_write_complete ();

				String json_string = store.get_json_string();
				//System.out.println (json_string);

				Object json_container = store.get_json_container();
				System.out.println (GeoJsonUtils.jsonObjectToString (json_container));

				// Unmarshal from JSON

				System.out.println ();
				System.out.println ("********** Unmarshal from JSON **********");
				System.out.println ();
			
				OESimulationParams sim_params2 = null;

				MarshalImpJsonReader retrieve = new MarshalImpJsonReader (json_string);
				sim_params2 = OESimulationParams.static_unmarshal (retrieve, null);
				retrieve.check_read_complete ();

				// Display the contents

				System.out.println (sim_params2.toString());

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("OESimulationParams : Unrecognized subcommand : " + args[0]);
		return;

	}




}
