package org.opensha.oaf.oetas.fit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.opensha.oaf.util.MarshalReader;
import org.opensha.oaf.util.MarshalWriter;
import org.opensha.oaf.util.MarshalException;
import org.opensha.oaf.util.InvariantViolationException;
import org.opensha.oaf.util.SimpleUtils;
import static org.opensha.oaf.util.SimpleUtils.rndd;

import org.opensha.oaf.oetas.OEStatsCalc;
import org.opensha.oaf.oetas.OEConstants;
import org.opensha.oaf.oetas.OECatalogParams;
import org.opensha.oaf.oetas.OECatalogSeedComm;
import org.opensha.oaf.oetas.OECatalogBuilder;
import org.opensha.oaf.oetas.OERupture;
import org.opensha.oaf.oetas.OESeedParams;
import org.opensha.oaf.oetas.OERandomGenerator;
import org.opensha.oaf.oetas.OECatalogStorage;
import org.opensha.oaf.oetas.util.OEValueElement;
import org.opensha.oaf.oetas.OEEnsembleInitializer;
import org.opensha.oaf.oetas.OECatalogSeeder;
import org.opensha.oaf.oetas.OECatalogRange;
import org.opensha.oaf.oetas.OEGenerationInfo;
import org.opensha.oaf.oetas.util.OEArraysCalc;


// Operational ETAS catalog initializer for fitted parameters.
// Author: Michael Barall 03/09/2023.

public class OEDisc2InitVoxSet implements OEEnsembleInitializer, OEDisc2InitVoxConsumer {


	//----- Implementation of OEDisc2InitVoxConsumer -----



	// The fitting information.

	private OEDisc2InitFitInfo fit_info;

	// The b-vallue to use for scaling catalog size, as supplied thru the consumer interface, or OEConstants.UNKNOWN_B_VALUE == -1.0 if unknown.

	private double b_scaling;

	// Number of voxels (guaranteed to be at least 1).

	private int voxel_count;

	// The statistics voxels, as an array; length = voxel_count.

	private OEDisc2InitStatVox[] a_voxel_list;

	// Temporary list, used for accumulating voxels.

	private ArrayList<OEDisc2InitStatVox> temp_voxel_list;




	// Begin consuming voxels.
	// Parameters:
	//  fit_info = Information about parameter fitting.
	//  b_scaling = The b-value to use for scaling catalog size, or OEConstants.UNKNOWN_B_VALUE == -1.0 if unknown.
	// Note: This function retains the fit_info object, so the caller must
	// not modify it after the function returns.
	// Threading: This function should be called by a single thread,
	// before any calls to add_voxel.

	@Override
	public void begin_voxel_consume (OEDisc2InitFitInfo fit_info, double b_scaling) {
		//if (b_scaling < OEConstants.UNKNOWN_B_VALUE_CHECK) {
		//	throw new IllegalArgumentException ("OEDisc2InitVoxSet.begin_voxel_consume: Negative b_scaling not supported: b_scaling = " + b_scaling);
		//}
		synchronized (this) {
			this.fit_info = fit_info;
			this.b_scaling = b_scaling;
			this.a_voxel_list = null;
			this.temp_voxel_list = new ArrayList<OEDisc2InitStatVox>();
		}
		return;
	}




	// End consuming voxels.
	// Threading: This function should be called by a single thread,
	// after all calls to add_voxel have returned.

	@Override
	public void end_voxel_consume () {
		synchronized (this) {
			voxel_count = temp_voxel_list.size();
			if (voxel_count == 0) {
				throw new IllegalArgumentException ("OEDisc2InitVoxSet.end_voxel_consume: No voxels supplied");
			}
			if (voxel_count > OEConstants.MAX_STATISTICS_GRID) {
				throw new IllegalArgumentException ("OEDisc2InitVoxSet.end_voxel_consume: Too many voxels supplied - voxel_count = " + voxel_count);
			}
			a_voxel_list = temp_voxel_list.toArray (new OEDisc2InitStatVox[0]);
			temp_voxel_list = null;
		}

		// Sort voxels into canonical order

		Arrays.sort (a_voxel_list, new OEDisc2InitStatVoxComparator());

		// Check the sort, specifically to check for duplicate voxels

		for (int index = 1; index < voxel_count; ++index) {
			int cmp = a_voxel_list[index - 1].compareTo (a_voxel_list[index]);
			if (!( cmp < 0 )) {
				throw new InvariantViolationException (
					"OEDisc2InitVoxSet.end_voxel_consume: Duplicate voxel or sort error: cmp = "
					+ cmp
					+ ", index = "
					+ index
					+ "\n"
					+ "voxel1 = "
					+ a_voxel_list[index - 1].summary_string()
					+ "voxel2 = "
					+ a_voxel_list[index].summary_string()
				);
			}
		}

		return;
	}




	// Add a list of voxels to the set of voxels.
	// Parameters:
	//  voxels = List of voxels to add to the set.
	// Threading: Can be called simulataneously by multiple threads, so the
	// implementation must synchronize appropriately.  To minimize synchronization
	// cost, instead of supplying voxels one at a time, the caller should supply
	// them in groups, perhaps as large as all voxels created by the caller's thread.

	@Override
	public void add_voxels (Collection<OEDisc2InitStatVox> voxels){
		synchronized (this) {
			temp_voxel_list.addAll (voxels);
		}
		return;
	}




	//----- Post-fitting -----




	//--- Input parameters

	// Original parameters for the catalog, from when the initializer was set up.

	private OECatalogParams original_cat_params;

	// Bayesian prior weight (1 = Bayesian, 0 = Sequence-specific, see OEConstants.BAY_WT_XXX).

	private double bay_weight;

	// The number of selected sub-voxels for seeding.

	private int seed_subvox_count;


	//--- Derived data

	// Prototype parameters for the catalog.
	// This contains the current range, but not the statistical parameters (which are done locally in each thread).

	private OECatalogParams proto_cat_params;

	// The total number of sub-voxels.

	private int total_subvox_count;

	// Cumulative number of sub-voxels for each voxel; length = voxel_count + 1.
	// cum_subvox_count[i] is the total number of sub-voxels in all voxels prior to i.

	private int[] cum_subvox_count;

	// The maximum log-density over all sub-voxels.

	private double max_log_density;

	// The sub-voxels for seeding; length = seed_subvox_count.

	private int[] a_seed_subvox;


	//--- Output statistics

	// Dithering mismatch, ideally should be zero.

	private int dither_mismatch;

	// Probability and tally of sub-voxels passing first clip, removing small log-density.

	private double clip_log_density_prob;
	private int clip_log_density_tally;

	// Probability and tally of sub-voxels passing second clip, removing tail of probability distribution.

	private double clip_tail_prob;
	private int clip_tail_tally;

	// Probability and tally of sub-voxels used for seeding.

	private double clip_seed_prob;
	private int clip_seed_tally;

	// Average b-value for sub-voxels used for seeding.

	private double seed_b_value;

	// The b-value to use for ranging.

	private double ranging_b_value;




	// Find the voxel index for a given global sub-voxel index.
	// Parameters:
	//  global_subvox_index = Global sub-voxel index, 0 thru total_subvox_count-1.

	private int get_voxel_for_subvox (int global_subvox_index) {
		return OEArraysCalc.bsearch_array (cum_subvox_count, global_subvox_index) - 1;
	}




	// Find the local sub-voxel index (index within the voxel) for a given global sub-voxel index.
	// Parameters:
	//  global_subvox_index = Global sub-voxel index, 0 thru total_subvox_count-1.
	//  voxel_index = Voxel index, 0 thru voxel_count-1, can be obtained from get_voxel_for_subvox().

	private int get_local_for_global_subvox (int global_subvox_index, int voxel_index) {
		return global_subvox_index - cum_subvox_count[voxel_index];
	}




	// Get the fitting information.
	// Note: Can be called any time after voxels are constructed, even before setup_post_fitting.
	// The caller should not modify the returned object.

	public final OEDisc2InitFitInfo get_fit_info () {
		return fit_info;
	}




	// Set up for initialization, post fitting.
	// Parameters:
	//  the_cat_params = Catalog parameters to use.
	//  the_bay_weight = Bayesian prior weight (1 = Bayesian, 0 = Sequence-specific, see OEConstants.BAY_WT_XXX).
	//  density_bin_size_lnu = Size of each bin for binning sub-voxels according to log-density, in natural log units.
	//  density_bin_count = Number of bins for binning sub-voxels according to log-density; must be >= 2.
	//  prob_tail_trim = Fraction of the probability distribution to trim.
	//  the_seed_subvox_count = Number of sub-voxels to use for seeding, must be a power of 2.
	// Note: The i-th density bin contains sub-voxels whose negative normalized log-density lies between
	// i*density_bin_size_lnu and (i+1)*density_bin_size_lnu.  The last bin contains all sub-voxels whose negative
	// normalized log-density is greater than (density_bin_count-1)*density_bin_size_lnu.  The density bins are
	// used for trimming the tail of the probability distribution.

	public final void setup_post_fitting (
		OECatalogParams the_cat_params,
		double the_bay_weight,
		double density_bin_size_lnu,
		int density_bin_count,
		double prob_tail_trim,
		int the_seed_subvox_count
	) {

		// Save the parameters

		proto_cat_params = (new OECatalogParams()).copy_from (the_cat_params);
		original_cat_params = (new OECatalogParams()).copy_from (the_cat_params);
		bay_weight = the_bay_weight;
		seed_subvox_count = the_seed_subvox_count;

		// First scan, find total sub-voxel count and maximum log-density

		cum_subvox_count = new int[voxel_count + 1];

		cum_subvox_count[0] = 0;
		total_subvox_count = a_voxel_list[0].get_subvox_count();
		max_log_density = a_voxel_list[0].get_max_subvox_log_density (bay_weight);

		for (int j = 1; j < voxel_count; ++j) {
			final OEDisc2InitStatVox voxel = a_voxel_list[j];
			cum_subvox_count[j] = total_subvox_count;
			total_subvox_count += voxel.get_subvox_count();
			final double x = voxel.get_max_subvox_log_density (bay_weight);
			if (max_log_density < x) {
				max_log_density = x;
			}
		}

		cum_subvox_count[voxel_count] = total_subvox_count;

		// Array to hold the probability of each sub-voxel

		double[] a_subvox_prob = new double[total_subvox_count];

		// Array to hold the density bin index of each sub-voxel

		int[] a_density_bin = new int[total_subvox_count];

		// Array to hold total probability for the sub-voxels in each bin, later cumulated, initialized to zero

		double[] a_prob_accum = new double[density_bin_count];
		OEArraysCalc.zero_array (a_prob_accum);

		// Array to hold the number of sub-voxels in each bin, later cumulated, initialized to zero

		int[] a_tally_accum = new int[density_bin_count];
		OEArraysCalc.zero_array (a_tally_accum);

		// Second scan, get the probabilities and density bins

		for (int j = 0; j < voxel_count; ++j) {
			final OEDisc2InitStatVox voxel = a_voxel_list[j];
			voxel.get_probabilities_and_bin (
				bay_weight,
				max_log_density,
				a_subvox_prob,
				a_density_bin,
				cum_subvox_count[j],	// dest_index
				density_bin_size_lnu,	// bin_size_lnu
				a_prob_accum,
				a_tally_accum
			);
		}

		// Cumulate the probabilities and tallies in the bins

		OEArraysCalc.cumulate_array (a_prob_accum, true);
		OEArraysCalc.cumulate_array (a_tally_accum, true);

		if (!( a_tally_accum[density_bin_count - 1] == total_subvox_count )) {
			throw new InvariantViolationException ("OEDisc2InitVoxSet.setup_post_fitting: Sub-voxel tally mismatch: total_subvox_count = " + total_subvox_count + ", a_tally_accum[density_bin_count - 1] = " + a_tally_accum[density_bin_count - 1]);
		}

		// Clip the tail as a fraction of the cumulative probability in the next-to-last bin (always clip the last bin)
		// (clip_bin_index is the first bin excluded, and is guaranteed to be >= 1)

		final double clip_prob = a_prob_accum[density_bin_count - 2] * (1.0 - prob_tail_trim);
		final int clip_bin_index = OEArraysCalc.bsearch_array (a_prob_accum, clip_prob, 1, density_bin_count - 1);

		// Initialize the clipping statistics

		clip_log_density_prob = a_prob_accum[density_bin_count - 2] / a_prob_accum[density_bin_count - 1];
		clip_log_density_tally = a_tally_accum[density_bin_count - 2];

		clip_tail_prob = a_prob_accum[clip_bin_index - 1] / a_prob_accum[density_bin_count - 1];
		clip_tail_tally = a_tally_accum[clip_bin_index - 1];

		clip_seed_prob = 0.0;
		clip_seed_tally = 0;

		seed_b_value = 0.0;
		ranging_b_value = 0.0;

		// Array of scrambled indexes for seed sub-voxels

		final int[] a_scramble = OEArraysCalc.make_bit_rev_array (seed_subvox_count);

		// Allocate array for seed sub-voxels

		a_seed_subvox = new int[seed_subvox_count];
		OEArraysCalc.fill_array (a_seed_subvox, -1);	// allows detection of unfilled elements

		// Dither probabilities to select the seed sub-voxels

		final double dither_step = a_prob_accum[clip_bin_index - 1] / ((double)seed_subvox_count);	// (total probability) / (nummber of seeds)
		double dither_accum = -0.5 * dither_step;

		int ix_seed = 0;

		int ix_voxel = 0;
		double b_voxel = a_voxel_list[ix_voxel].get_b_value();

		int tol = seed_subvox_count / 32;		// tolerance for dither mismatch

		for (int ix_subvox = 0; ix_subvox < total_subvox_count; ++ix_subvox) {

			// If end of voxel, advance to the next Voxel

			if (ix_subvox == cum_subvox_count[ix_voxel + 1]) {
				++ix_voxel;
				b_voxel = a_voxel_list[ix_voxel].get_b_value();
			}

			// If this sub-voxel passes the probability tail clip...

			if (a_density_bin[ix_subvox] < clip_bin_index) {

				// Add the sub-voxel probability to the dither

				dither_accum += a_subvox_prob[ix_subvox];

				// If this sub-voxel is contributing to the seeding...

				if (dither_accum >= 0.0) {

					// Accumulate it for statistics

					clip_seed_prob += a_subvox_prob[ix_subvox];
					++clip_seed_tally;

					// Add it to seeds as many times as needed

					do {

						// Use this sub-voxel

						if (ix_seed < seed_subvox_count) {
							if (a_seed_subvox[a_scramble[ix_seed]] != -1) {
								throw new InvariantViolationException ("OEDisc2InitVoxSet.setup_post_fitting: Seeding array collision: ix_seed = " + ix_seed + ", ix_subvox = " + ix_subvox + ", a_scramble[ix_seed] = " + a_scramble[ix_seed] + ", a_seed_subvox[a_scramble[ix_seed]] = " + a_seed_subvox[a_scramble[ix_seed]]);
							}

							a_seed_subvox[a_scramble[ix_seed]] = ix_subvox;
							seed_b_value += b_voxel;
						}
						else if (ix_seed > seed_subvox_count + tol) {
							throw new InvariantViolationException ("OEDisc2InitVoxSet.setup_post_fitting: Dither mismatch: seed_subvox_count = " + seed_subvox_count + ", ix_seed = " + ix_seed);
						}
						++ix_seed;

						// Advance the dither

						dither_accum -= dither_step;

					} while (dither_accum >= 0.0);
				}
			}
		}

		// Finish the clipping statistics

		clip_seed_prob /= a_prob_accum[density_bin_count - 1];

		dither_mismatch = ix_seed - seed_subvox_count;

		// The dither mismatch should be zero, but we permit some sloppiness

		if (dither_mismatch != 0) {

			// Check for mismatch exceeding tolerance

			if (dither_mismatch > tol || dither_mismatch < -tol) {
				throw new InvariantViolationException ("OEDisc2InitVoxSet.setup_post_fitting: Dither mismatch: seed_subvox_count = " + seed_subvox_count + ", final ix_seed = " + ix_seed);
			}

			// If we didn't fill all the seeds, pad using elements from the middle

			int mid = seed_subvox_count / 2;
			while (ix_seed < seed_subvox_count) {

				if (a_seed_subvox[a_scramble[ix_seed]] != -1) {
					throw new InvariantViolationException ("OEDisc2InitVoxSet.setup_post_fitting: Seeding array collision: ix_seed = " + ix_seed + ", a_scramble[ix_seed] = " + a_scramble[ix_seed] + ", a_seed_subvox[a_scramble[ix_seed]] = " + a_seed_subvox[a_scramble[ix_seed]]);
				}

				final int ix_subvox = a_seed_subvox[a_scramble[ix_seed - mid]];
				if (ix_subvox == -1) {
					throw new InvariantViolationException ("OEDisc2InitVoxSet.setup_post_fitting: Seeding array entry empty: ix_seed - mid = " + (ix_seed - mid) + ", a_scramble[ix_seed - mid] = " + a_scramble[ix_seed - mid] + ", a_seed_subvox[a_scramble[ix_seed - mid]] = " + a_seed_subvox[a_scramble[ix_seed - mid]]);
				}

				a_seed_subvox[a_scramble[ix_seed]] = ix_subvox;
				++ix_seed;
				seed_b_value += a_voxel_list[get_voxel_for_subvox (ix_subvox)].get_b_value();
			}
		}

		// Finish the b-value computation, and use it for scaling if needed

		seed_b_value /= ((double)seed_subvox_count);

		if (b_scaling < OEConstants.UNKNOWN_B_VALUE_CHECK) {
			ranging_b_value = seed_b_value;
		} else {
			ranging_b_value = b_scaling;
		}

		// Here there could be computation of parameter statistics...

		return;
	}




	//----- Construction -----




	// Erase the consume data.

	private final void clear_consumer () {
		fit_info = null;
		b_scaling = 0.0;
		voxel_count = 0;
		a_voxel_list = null;
		temp_voxel_list = null;
		return;
	}




	// Erse the post-fitting data.

	public final void clear_post_fitting () {
		original_cat_params = null;
		bay_weight = 0.0;
		seed_subvox_count = 0;

		proto_cat_params = null;
		total_subvox_count = 0;
		cum_subvox_count = null;
		max_log_density = 0.0;
		a_seed_subvox = null;

		dither_mismatch = 0;
		clip_log_density_prob = 0.0;
		clip_log_density_tally = 0;
		clip_tail_prob = 0.0;
		clip_tail_tally = 0;
		clip_seed_prob = 0.0;
		clip_seed_tally = 0;
		seed_b_value = 0.0;
		ranging_b_value = 0.0;
		return;
	}




	// Erase the contents.

	public final void clear () {
		clear_consumer();
		clear_post_fitting();
		return;
	}




	// Default constructor.

	public OEDisc2InitVoxSet () {
		clear();
	}




	// Display our contents.

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append ("OEDisc2InitVoxSet:" + "\n");

		if (fit_info != null) {
			result.append ("fit_info = {" + fit_info.toString() + "}\n");
		}
		result.append ("b_scaling = " + b_scaling + "\n");
		result.append ("voxel_count = " + voxel_count + "\n");
		if (a_voxel_list != null) {
			result.append ("a_voxel_list.length = " + a_voxel_list.length + "\n");
		}
		if (original_cat_params != null) {
			result.append ("original_cat_params = {" + original_cat_params.toString() + "}\n");
		}
		result.append ("bay_weight = " + bay_weight + "\n");
		result.append ("seed_subvox_count = " + seed_subvox_count + "\n");
		if (proto_cat_params != null) {
			result.append ("proto_cat_params = {" + proto_cat_params.toString() + "}\n");
		}
		result.append ("total_subvox_count = " + total_subvox_count + "\n");
		if (cum_subvox_count != null) {
			result.append ("cum_subvox_count.length = " + cum_subvox_count.length + "\n");
			if (cum_subvox_count.length >= 2) {
				result.append ("cum_subvox_count[0] = " + cum_subvox_count[0] + "\n");
				result.append ("cum_subvox_count[1] = " + cum_subvox_count[1] + "\n");
				result.append ("cum_subvox_count[" + (cum_subvox_count.length - 1) + "] = " + cum_subvox_count[cum_subvox_count.length - 1] + "\n");
			}
		}
		result.append ("max_log_density = " + max_log_density + "\n");
		if (a_seed_subvox != null) {
			result.append ("a_seed_subvox.length = " + a_seed_subvox.length + "\n");
			if (a_seed_subvox.length >= 4) {
				result.append ("a_seed_subvox[0] = " + a_seed_subvox[0] + "\n");
				result.append ("a_seed_subvox[1] = " + a_seed_subvox[1] + "\n");
				result.append ("a_seed_subvox[2] = " + a_seed_subvox[2] + "\n");
				result.append ("a_seed_subvox[3] = " + a_seed_subvox[3] + "\n");
			}
		}
		result.append ("dither_mismatch = " + dither_mismatch + "\n");
		result.append ("clip_log_density_prob = " + clip_log_density_prob + "\n");
		result.append ("clip_log_density_tally = " + clip_log_density_tally + "\n");
		result.append ("clip_tail_prob = " + clip_tail_prob + "\n");
		result.append ("clip_tail_tally = " + clip_tail_tally + "\n");
		result.append ("clip_seed_prob = " + clip_seed_prob + "\n");
		result.append ("clip_seed_tally = " + clip_seed_tally + "\n");
		result.append ("seed_b_value = " + seed_b_value + "\n");
		result.append ("ranging_b_value = " + ranging_b_value + "\n");

		return result.toString();
	}




	//----- Seeders -----




	// The current index into the seeding array.

	private final AtomicInteger seeding_index = new AtomicInteger();




	// Seeder for voxel set.

	private class SeederVoxSet implements OECatalogSeeder {

		//----- Control -----

		// True if seeder is open.

		private boolean f_open;


		//----- Per-thread data structures -----

		private final OECatalogParams local_cat_params = new OECatalogParams();
		private final OERupture local_rup = new OERupture();


		//----- Construction -----

		// Default constructor.

		public SeederVoxSet () {
			f_open = false;
		}

		//----- Open/Close methods (Implementation of OECatalogSeeder) -----

		// Open the catalog seeder.
		// Perform any setup needed.

		@Override
		public void open () {

			// Clear the local data sttructures

			local_cat_params.clear();
			local_rup.clear();

			// Mark it open

			f_open = true;
			return;
		}

		// Close the catalog seeder.
		// Perform any final tasks needed.

		@Override
		public void close () {

			// If open ...

			if (f_open) {
			
				// Mark it closed

				f_open = false;
			}
			return;
		}


		//----- Data methods (Implementation of OECatalogSeeder) -----

		// Seed a catalog.
		// Parameters:
		//  comm = Communication area, with per-catalog values set up.
		// This function should perform the following steps:
		// 1. Construct a OECatalogParams object containing the catalog parameters.
		// 2. Call comm.cat_builder.begin_catalog() to begin constructing the catalog.
		// 3. Construct a OEGenerationInfo object containing info for the first (seed) generation.
		// 4. Call comm.cat_builder.begin_generation() to begin the first (seed) generation.
		// 5. Call comm.cat_builder.add_rup one or more times to add the seed ruptures.
		// 6. Call comm.cat_builder.end_generation() to end the first (seed) generation.

		@Override
		public void seed_catalog (OECatalogSeedComm comm) {

			// Get the voxel and sub-voxel indexes

			final int local_seed_index = seeding_index.getAndIncrement();		// a different index for each catalog
			final int global_subvox_index = a_seed_subvox[local_seed_index % seed_subvox_count];	// wrap if number of catalogs exceeds number of seeds
			final int voxel_index = get_voxel_for_subvox (global_subvox_index);
			final int local_subvox_index = get_local_for_global_subvox (global_subvox_index, voxel_index);

			// Let the voxel make the catalog parameters

			a_voxel_list[voxel_index].get_cat_params (
				fit_info,
				proto_cat_params,
				local_cat_params
			);

			// Let the voxel seed the catalog

			a_voxel_list[voxel_index].seed_catalog (
				fit_info,
				local_subvox_index,
				local_cat_params,
				comm,
				local_rup
			);
		
			return;
		}

	}




	//----- Implementation of OEEnsembleInitializer -----




	// Make a catalog seeder.
	// Returns a seeder which is able to seed the contents of one catalog
	// (or several catalogs in succession).
	// This function may be called repeatedly to create several seeders,
	// which can be used in multiple worker threads.
	// Threading: Can be called in multiple threads, before or after the call to
	// begin_initialization, and while there are existing open seeders, and so
	// must be properly synchronized.
	// Note: The returned seeder cannot be opened until after the call to
	// begin_initialization, and must be closed before the call to end_initialization.
	// Note: The returned seeder can be opened and closed repeatedly to seed
	// multiple catalogs.

	@Override
	public OECatalogSeeder make_seeder () {
		OECatalogSeeder seeder;
		seeder = new SeederVoxSet ();
		return seeder;
	}




	// Begin initializing catalogs.
	// This function should be called before any other control methods.
	// The initializer should allocate any resources it needs.
	// Threading: No other thread should be accessing this object,
	// and none of its seeders can be open.

	@Override
	public void begin_initialization () {

		// Reset the seeding index, so each ensemble repeats the same sequence of seedings

		seeding_index.set (0);
		return;
	}




	// End initializing catalogs.
	// This function should be called after all other control functions.
	// It provides an opportunity for the initializer to release any resources it holds.
	// Threading: No other thread should be accessing this object,
	// and none of its seeders can be open.

	@Override
	public void end_initialization () {
		return;
	}




	// Return true if there is a mainshock magnitude available.
	// Threading: No other thread should be accessing this object,
	// and be either before calling begin_initialization() or after
	// calling end_initialization().

	@Override
	public boolean has_mainshock_mag () {
		return fit_info.has_mag_main();
	}




	// Return the mainshock magnitude.
	// Check has_mainshock_mag() before calling this function.
	// Note: If has_mainshock_mag() returns false, then this function should
	// return a scaling magnitude (e.g., largest earthquake in a swarm), which
	// can be used for simulation ranging, but is not reported in the forecast.
	// Threading: No other thread should be accessing this object,
	// and be either before calling begin_initialization() or after
	// calling end_initialization().

	@Override
	public double get_mainshock_mag () {
		return fit_info.mag_main;
	}




	// Get the time and magnitude range of the catalog simulations.
	// The returned object is newly-allocated and not retained in this object.

	@Override
	public OECatalogRange get_range () {
		return proto_cat_params.get_range();
	}




	// Get the initial time and magnitude range of the catalog simulations.
	// The returned object is newly-allocated and not retained in this object.

	@Override
	public OECatalogRange get_initial_range () {
		return original_cat_params.get_range();
	}




	// Set the time and magnitude range to use for catalog simulations.
	// The supplied OECatalogRange object is not retained.
	// Note: This function allows adjusting time and magnitude ranges
	// without the need to construct an entirely new initializer.

	@Override
	public void set_range (OECatalogRange range) {
		proto_cat_params.set_range (range);
		return;
	}




	// Get the b-value used by the initializer.
	// The purpose of this function is to obtain a b-value that can be used
	// for adjusting the magnitude range in order to get a desired median
	// or expected catalog size.

	@Override
	public double get_b_value () {
		return ranging_b_value;
	}




	// Get parameters that can be displayed to the user.
	// Parameters:
	//  paramMap = Map of parameters, which this function adds to.
	// For consistency, it is recommended that parameter names use camelCase.
	// Each value should be one of: Integer, Long, Double, Float, Boolean, or String.

	@Override
	public void get_display_params (Map<String, Object> paramMap) {
		//paramMap.put ("a", SimpleUtils.round_double_via_string ("%.2f", cat_params.a));
		//paramMap.put ("p", SimpleUtils.round_double_via_string ("%.2f", cat_params.p));
		//paramMap.put ("c", SimpleUtils.round_double_via_string ("%.2e", cat_params.c));
		//paramMap.put ("b", SimpleUtils.round_double_via_string ("%.2f", cat_params.b));
		//paramMap.put ("alpha", SimpleUtils.round_double_via_string ("%.2f", cat_params.alpha));
		paramMap.put ("Mref", SimpleUtils.round_double_via_string ("%.2f", proto_cat_params.mref));
		paramMap.put ("Msup", SimpleUtils.round_double_via_string ("%.2f", proto_cat_params.msup));
		return;
	}




	//----- Marshaling -----

	// Marshal version number.

	private static final int MARSHAL_VER_1 = 116001;

	private static final String M_VERSION_NAME = "OEDisc2InitVoxSet";

	// Marshal object, internal.

	private void do_marshal (MarshalWriter writer) {

		// Version

		int ver = MARSHAL_VER_1;

		writer.marshalInt (M_VERSION_NAME, ver);

		// Contents

		switch (ver) {

		case MARSHAL_VER_1: {

			OEDisc2InitFitInfo.static_marshal (writer, "fit_info"              , fit_info              );
			writer.marshalDouble              (        "b_scaling"             , b_scaling             );
			writer.marshalInt                 (        "voxel_count"           , voxel_count           );
			OEDisc2InitStatVox.marshal_array  (writer, "a_voxel_list"          , a_voxel_list          );
			OECatalogParams.static_marshal    (writer, "original_cat_params"   , original_cat_params   );
			writer.marshalDouble              (        "bay_weight"            , bay_weight            );
			writer.marshalInt                 (        "seed_subvox_count"     , seed_subvox_count     );
			OECatalogParams.static_marshal    (writer, "proto_cat_params"      , proto_cat_params      );
			writer.marshalInt                 (        "total_subvox_count"    , total_subvox_count    );
			writer.marshalIntArray            (        "cum_subvox_count"      , cum_subvox_count      );
			writer.marshalDouble              (        "max_log_density"       , max_log_density       );
			writer.marshalIntArray            (        "a_seed_subvox"         , a_seed_subvox         );
			writer.marshalInt                 (        "dither_mismatch"       , dither_mismatch       );
			writer.marshalDouble              (        "clip_log_density_prob" , clip_log_density_prob );
			writer.marshalInt                 (        "clip_log_density_tally", clip_log_density_tally);
			writer.marshalDouble              (        "clip_tail_prob"        , clip_tail_prob        );
			writer.marshalInt                 (        "clip_tail_tally"       , clip_tail_tally       );
			writer.marshalDouble              (        "clip_seed_prob"        , clip_seed_prob        );
			writer.marshalInt                 (        "clip_seed_tally"       , clip_seed_tally       );
			writer.marshalDouble              (        "seed_b_value"          , seed_b_value          );
			writer.marshalDouble              (        "ranging_b_value"       , ranging_b_value       );

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

			fit_info               = OEDisc2InitFitInfo.static_unmarshal (reader, "fit_info"              );
			b_scaling              = reader.unmarshalDouble              (        "b_scaling"             );
			voxel_count            = reader.unmarshalInt                 (        "voxel_count"           );
			a_voxel_list           = OEDisc2InitStatVox.unmarshal_array  (reader, "a_voxel_list"          );
			original_cat_params    = OECatalogParams.static_unmarshal    (reader, "original_cat_params"   );
			bay_weight             = reader.unmarshalDouble              (        "bay_weight"            );
			seed_subvox_count      = reader.unmarshalInt                 (        "seed_subvox_count"     );
			proto_cat_params       = OECatalogParams.static_unmarshal    (reader, "proto_cat_params"      );
			total_subvox_count     = reader.unmarshalInt                 (        "total_subvox_count"    );
			cum_subvox_count       = reader.unmarshalIntArray            (        "cum_subvox_count"      );
			max_log_density        = reader.unmarshalDouble              (        "max_log_density"       );
			a_seed_subvox          = reader.unmarshalIntArray            (        "a_seed_subvox"         );
			dither_mismatch        = reader.unmarshalInt                 (        "dither_mismatch"       );
			clip_log_density_prob  = reader.unmarshalDouble              (        "clip_log_density_prob" );
			clip_log_density_tally = reader.unmarshalInt                 (        "clip_log_density_tally");
			clip_tail_prob         = reader.unmarshalDouble              (        "clip_tail_prob"        );
			clip_tail_tally        = reader.unmarshalInt                 (        "clip_tail_tally"       );
			clip_seed_prob         = reader.unmarshalDouble              (        "clip_seed_prob"        );
			clip_seed_tally        = reader.unmarshalInt                 (        "clip_seed_tally"       );
			seed_b_value           = reader.unmarshalDouble              (        "seed_b_value"          );
			ranging_b_value        = reader.unmarshalDouble              (        "ranging_b_value"       );

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

	public OEDisc2InitVoxSet unmarshal (MarshalReader reader, String name) {
		reader.unmarshalMapBegin (name);
		do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return this;
	}

	// Marshal object.

	public static void static_marshal (MarshalWriter writer, String name, OEDisc2InitVoxSet accumulator) {
		writer.marshalMapBegin (name);
		accumulator.do_marshal (writer);
		writer.marshalMapEnd ();
		return;
	}

	// Unmarshal object.

	public static OEDisc2InitVoxSet static_unmarshal (MarshalReader reader, String name) {
		OEDisc2InitVoxSet accumulator = new OEDisc2InitVoxSet();
		reader.unmarshalMapBegin (name);
		accumulator.do_umarshal (reader);
		reader.unmarshalMapEnd ();
		return accumulator;
	}




	//----- Testing -----




	// Get the number of voxels.
	// This function is for testing.

	public final int test_get_voxel_count () {
		return voxel_count;
	}




	// Get the voxel at the specified index.
	// This function is for testing.

	public final OEDisc2InitStatVox test_get_voxel (int index) {
		return a_voxel_list[index];
	}




	// Fill a grid with a c/p/a/ams likelihood grid.
	// Parameters:
	//  bay_weight = Bayesian prior weight, see OEConstants.BAY_WT_XXXX.
	//  grid = Grid to receive log-density values, indexed as grid[cix][pix][aix][amsix].
	// This function is for testing.
	// Note: This function assumes that b, alpha, and zmu are fixed, and that the
	// voxels represent a grid constructed from separate ranges of c, p, n, and zams.
	// The function depends on the fact that voxels are sorted into canoncial order,
	// which sorts first on b, then alpha, then c, then p, then n.  It also depends
	// on zams values appearing in order in the list of sub=voxels.

	public final void test_get_c_p_a_ams_log_density_grid (double bay_weight, double[][][][] grid) {
		final int c_length = grid.length;
		final int p_length = grid[0].length;
		final int a_length = grid[0][0].length;
		final int ams_length = grid[0][0][0].length;
		if (!( c_length * p_length * a_length == voxel_count )) {
			throw new InvariantViolationException ("OEDisc2InitVoxSet.test_get_log_density_grid: Grid size mismatch: c_length = " + c_length + ", p_length = " + p_length + ", a_length = " + a_length + ", voxel_count = " + voxel_count);
		}
		for (int cix = 0; cix < c_length; ++cix) {
			for (int pix = 0; pix < p_length; ++pix) {
				for (int aix = 0; aix < a_length; ++aix) {
					final OEDisc2InitStatVox stat_vox = a_voxel_list[(((cix * p_length) + pix) * a_length) + aix];
					final int subvox_count = stat_vox.get_subvox_count();
					if (!( ams_length == subvox_count )) {
						throw new InvariantViolationException ("OEDisc2InitVoxSet.test_get_log_density_grid: Grid size mismatch: ams_length = " + ams_length + ", subvox_count = " + subvox_count);
					}
					for (int amsix = 0; amsix < ams_length; ++amsix) {
						grid[cix][pix][aix][amsix] = stat_vox.get_subvox_log_density (amsix, bay_weight);
					}
				}
			}
		}
		return;
	}




	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("OEDisc2InitVoxSet : Missing subcommand");
			return;
		}








		// Unrecognized subcommand.

		System.err.println ("OEDisc2InitVoxSet : Unrecognized subcommand : " + args[0]);
		return;

	}

}
