package org.opensha.oaf.comcat;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Date;
import java.math.BigDecimal;

import java.net.URL;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.net.HttpURLConnection;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.StringReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.opensha.commons.data.comcat.ComcatRegion;
import org.opensha.commons.data.comcat.ComcatException;
import org.opensha.commons.data.comcat.ComcatAccessor;
import org.opensha.commons.data.comcat.ComcatEventWebService;
import org.opensha.commons.data.comcat.ComcatVisitor;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

//import gov.usgs.earthquake.event.EventQuery;
//import gov.usgs.earthquake.event.EventWebService;
//import gov.usgs.earthquake.event.Format;
//import gov.usgs.earthquake.event.JsonEvent;
//import gov.usgs.earthquake.event.JsonUtil;

import org.opensha.sha.earthquake.observedEarthquake.ObsEqkRupture;
import org.opensha.commons.geo.Location;
import org.opensha.commons.geo.LocationList;
import org.opensha.commons.geo.GeoTools;

import org.opensha.oaf.util.SimpleUtils;
import org.opensha.oaf.util.SphRegionWorld;

import org.opensha.oaf.aafs.ServerConfig;
import org.opensha.oaf.aafs.ServerConfigFile;

import org.opensha.oaf.pdl.PDLProductFile;

import gov.usgs.earthquake.event.JsonEvent;


/**
 * A generic Comcat product, as retrieved from Comcat.
 * Author: Michael Barall 02/25/2020.
 *
 * This class holds information that is common to all Comcat products.
 * It may be used as a superclass for a class that handles a specific product type.
 * Or it may be used by itself in cases where only generic product info is needed.
 */
public class ComcatProduct {

	// Known Comcat product types.

	public static final String PRODTYPE_DYFI = "dyfi";
	public static final String PRODTYPE_FINITE_FAULT = "finite-fault";
	public static final String PRODTYPE_FOCAL_MECHANISM = "focal-mechanism";
	public static final String PRODTYPE_GENERAL_LINK = "general-link";
	public static final String PRODTYPE_GENERAL_TEXT = "general-text";
	public static final String PRODTYPE_GEOSERVE = "geoserve";
	public static final String PRODTYPE_GROUND_FAILURE = "ground-failure";
	public static final String PRODTYPE_IMPACT_LINK = "impact-link";
	public static final String PRODTYPE_IMPACT_TEXT = "impact-text";
	public static final String PRODTYPE_LOSSPAGER = "losspager";
	public static final String PRODTYPE_MOMENT_TENSOR = "moment-tensor";
	public static final String PRODTYPE_NEARBY_CITIES = "nearby-cities";
	public static final String PRODTYPE_OAF = "oaf";
	public static final String PRODTYPE_ORIGIN = "origin";
	public static final String PRODTYPE_PHASE_DATA = "phase-data";
	public static final String PRODTYPE_POSTER = "poster";
	public static final String PRODTYPE_SCITECH_LINK = "scitech-link";
	public static final String PRODTYPE_SHAKEMAP = "shakemap";
	public static final String PRODTYPE_EVENT_SEQUENCE = "event-sequence";
	public static final String PRODTYPE_EVENT_SEQUENCE_TEXT = "event-sequence-text";


	// Return true if the given product type is valid for this class.

	public static boolean is_valid_product_type (String product_type) {
		switch (product_type) {
			case PRODTYPE_DYFI:
			case PRODTYPE_FINITE_FAULT:
			case PRODTYPE_FOCAL_MECHANISM:
			case PRODTYPE_GENERAL_LINK:
			case PRODTYPE_GENERAL_TEXT:
			case PRODTYPE_GEOSERVE:
			case PRODTYPE_GROUND_FAILURE:
			case PRODTYPE_IMPACT_LINK:
			case PRODTYPE_IMPACT_TEXT:
			case PRODTYPE_LOSSPAGER:
			case PRODTYPE_MOMENT_TENSOR:
			case PRODTYPE_NEARBY_CITIES:
			case PRODTYPE_OAF:
			case PRODTYPE_ORIGIN:
			case PRODTYPE_PHASE_DATA:
			case PRODTYPE_POSTER:
			case PRODTYPE_SCITECH_LINK:
			case PRODTYPE_SHAKEMAP:
			case PRODTYPE_EVENT_SEQUENCE:
			case PRODTYPE_EVENT_SEQUENCE_TEXT:
				return true;
		}
		return false;
	}


	// Default product type.
	// (In subclasses this should be the product type of the subclass)

	private static final String default_product_type = PRODTYPE_OAF;




	// Product file connection timeout, in milliseconds.
	// Use 0 for no timeout, or -1 for the system default.
	// Note: See java.net.URLConnection for information on timeouts.

	public static final int PRODFILE_CONNECT_TIMEOUT = 15000;		// 15 seconds

	// Product file read timeout, in milliseconds.
	// Use 0 for no timeout, or -1 for the system default.
	// Note: See java.net.URLConnection for information on timeouts.
	// Note: The system default is apparently 0, meaing no timeout, but on rare
	// occasions the use of 0 can cause the program to hang in an infinite wait.

	public static final int PRODFILE_READ_TIMEOUT = 30000;			// 30 seconds

	// Maximum length we accept for a product file.

	public static final long PRODFILE_MAX_LENGTH = 134217728L;		// 128 MB




	// Flag indicating if this is a delete product (derived from "status" in the product).

	public boolean isDelete;

	// Event ID, used as the "code" for the product (for example, "us10006jv5") ("code" in the product).

	public String eventID;

	// Source ID, used as the "source" for the product (for example, "us") ("source" in the product).

	public String sourceID;
	
	// Network identifier for the event (for example, "us") ("properties/eventsource" in the product).
	
	public String eventNetwork;
	
	// Network code for the event (for example, "10006jv5") ("properties/eventsourcecode" in the product).
	
	public String eventCode;

	// True if this product has been reviewed ("properties/review-status" in the product).

	public boolean isReviewed;
	
	// Time the product was submitted to PDL ("updateTime" in the product).
	
	public long updateTime;
	
	// In-line text, or null if none (null if this is a delete product) ("contents//bytes" in the product).
	// Note: The double slash indicates a JSON key which is the empty string "".
	
	public String inlineText;

	// Information about a product file.

	public static class ProductFile {
	
		// MIME type ("contentType" in the product).
		// Note: See PDLProductFile for common MIME types.

		public String contentType;

		// URL to file ("url" in the product).

		public String url;

		// Last modified time ("lastModified" in the product), or -1L if not available.

		public long lastModified;

		// Length of file ("length" in the product), or -1L if not available.

		public long length;

		// SHA256 for the file ("sha256" in the product), or null if not available.

		public String sha256;
	}

	// Map of product files (empty if this is a delete product) ("contents" in the product).

	public HashMap<String, ProductFile> productFiles;

	// Map of property strings (empty if this is a delete product) ("properties" in the product).

	public HashMap<String, String> propertyStrings;

	// Information for in-line text, null if no in-line text.
	// The url is null.  The contentType may be null or an invalid MIME type.

	public ProductFile inlineFile;




	// Return true if the product contains the given file.

	public boolean contains_file (String filename) {
		return productFiles.containsKey (filename);
	}




	// Return true if the product contains the given property.

	public boolean contains_property (String property_name) {
		return propertyStrings.containsKey (property_name);
	}




	//----- Functions for reading products -----




	// Read contents from a GeoJson product.
	// Parameters:
	//  gj_product = GeoJson containing the product.
	//  delete_ok = True if delete products are OK, false if not.
	// Returns true if success, false if data missing or mis-formatted.
	// Returns false if it is a delete product and delete_ok is false.
	//
	// Note: A subclass may override this method to add additional fields
	// or additional error checks.  The subclass should first pass thru
	// to this class.  If this function returns false then the subclass
	// should immediately return false; otherwise, the subclass should
	// perform its own processing.

	protected boolean read_from_gj (JSONObject gj_product, boolean delete_ok) {

		// Delete flag

		isDelete = false;
		String product_status = GeoJsonUtils.getString (gj_product, "status");
		if (product_status != null) {
			if (product_status.equalsIgnoreCase ("DELETE")) {
				isDelete = true;
				if (!( delete_ok )) {
					return false;
				}
			}
		}
	
		// Our code

		eventID = GeoJsonUtils.getString (gj_product, "code");
		if (eventID == null || eventID.isEmpty()) {
			return false;
		}

		// Our source

		sourceID = GeoJsonUtils.getString (gj_product, "source");
		if (sourceID == null || sourceID.isEmpty()) {
			return false;
		}

		// Event network

		eventNetwork = GeoJsonUtils.getString (gj_product, "properties", "eventsource");
		if (eventNetwork == null || eventNetwork.isEmpty()) {
			return false;
		}

		// Event network code

		eventCode = GeoJsonUtils.getString (gj_product, "properties", "eventsourcecode");
		if (eventCode == null || eventCode.isEmpty()) {
			return false;
		}

		// Review status, note that a missing item means not-reviewed

		isReviewed = false;
		String review_status = GeoJsonUtils.getString (gj_product, "properties", "review-status");
		if (review_status != null) {
			if (review_status.equalsIgnoreCase ("reviewed")) {
				isReviewed = true;
			}
		}

		// Update time

		Long the_update_time = GeoJsonUtils.getTimeMillis (gj_product, "updateTime");
		if (the_update_time == null) {
			return false;
		}
		updateTime = the_update_time;

		// In-line text

		inlineText = null;
		inlineFile = null;

		if (!( isDelete )) {

			inlineText = GeoJsonUtils.getString (gj_product, "contents", "", "bytes");

			if (inlineText != null) {
				inlineFile = new ProductFile();

				// MIME type, null if not available

				inlineFile.contentType = GeoJsonUtils.getString (gj_product, "contents", "", "contentType");

				// File URL

				inlineFile.url = null;

				// Modified time, -1L if not available

				Long ftime = GeoJsonUtils.getLong (gj_product, "contents", "", "lastModified");
				if (ftime == null) {
					inlineFile.lastModified = -1L;
				} else {
					inlineFile.lastModified = ftime;
				}

				// Length, -1L if not available

				Long flen = GeoJsonUtils.getLong (gj_product, "contents", "", "length");
				if (flen == null) {
					inlineFile.length = -1L;
				} else {
					inlineFile.length = flen;
					if (inlineFile.length < 0L) {
						inlineFile.length = -1L;
					}
				}

				// SHA256, null if not available

				inlineFile.sha256 = GeoJsonUtils.getString (gj_product, "contents", "", "sha256");
			}
		}

		// Contents

		productFiles = new HashMap<String, ProductFile>();

		if (!( isDelete )) {

			JSONObject gj_contents = GeoJsonUtils.getJsonObject (gj_product, "contents");

			if (gj_contents != null) {

				for (Object key : gj_contents.keySet()) {

					// The filename is the key, an empty filename is the inline data

					String filename = key.toString();
					if (!( filename.isEmpty() )) {

						ProductFile the_file = new ProductFile();

						// MIME type

						the_file.contentType = GeoJsonUtils.getString (gj_contents, filename, "contentType");

						if (!( the_file.contentType == null || the_file.contentType.isEmpty() )) {

							// File URL

							the_file.url = GeoJsonUtils.getString (gj_contents, filename, "url");

							if (!( the_file.url == null || the_file.url.isEmpty() )) {

								// Modified time, -1L if not available

								Long ftime = GeoJsonUtils.getLong (gj_contents, filename, "lastModified");
								if (ftime == null) {
									the_file.lastModified = -1L;
								} else {
									the_file.lastModified = ftime;
								}

								// Length, -1L if not available

								Long flen = GeoJsonUtils.getLong (gj_contents, filename, "length");
								if (flen == null) {
									the_file.length = -1L;
								} else {
									the_file.length = flen;
									if (the_file.length < 0L) {
										the_file.length = -1L;
									}
								}

								// SHA256, null if not available

								the_file.sha256 = GeoJsonUtils.getString (gj_contents, filename, "sha256");

								// Add to the map

								productFiles.put (filename, the_file);
							}
						}
					}
				}
			}

		}

		// Properties

		propertyStrings = new HashMap<String, String>();

		if (!( isDelete )) {

			JSONObject gj_properties = GeoJsonUtils.getJsonObject (gj_product, "properties");

			if (gj_properties != null) {

				for (Object key : gj_properties.keySet()) {

					// The property name is the key

					String property_name = key.toString();

					// Property string

					String property_string = GeoJsonUtils.getString (gj_properties, property_name);

					if (!( property_string == null )) {

						// Add to the map

						propertyStrings.put (property_name, property_string);
					}
				}
			}

		}

		return true;
	}




	// Make a product from a GeoJson product.
	// Parameters:
	//  gj_product = GeoJson containing the product.
	//  delete_ok = True if delete products are OK, false if not (defaults to false if omitted).
	// Returns the constructed product, or null if product is missing or mis-formatted.
	// Returns null if it is a delete product and delete_ok is false or omitted.
	// Note: A subclass may wish to provide versions that return the type of the subclass.

	public static ComcatProduct make_from_gj (JSONObject gj_product) {
		return make_from_gj (gj_product, false);
	}

	public static ComcatProduct make_from_gj (JSONObject gj_product, boolean delete_ok) {
		ComcatProduct the_product = new ComcatProduct();
		if (the_product.read_from_gj (gj_product, delete_ok)) {
			return the_product;
		}
		return null;
	}




	// Make a preferred product from a GeoJson event.
	// Parameters:
	//  product_type = Type of product.
	//  event = GeoJson containing the event.
	//  delete_ok = True if delete products are OK, false if not (defaults to false if omitted).
	// Returns the constructed product, or null if product is missing or mis-formatted.
	// Returns null if it is a delete product and delete_ok is false or omitted.
	// Note: The preferred product is the first entry in the array of products.
	// Note: A subclass may wish to provide versions that return the type of the subclass.

	public static ComcatProduct make_preferred_from_gj (String product_type, JSONObject event) {
		return make_preferred_from_gj (product_type, event, false);
	}

	public static ComcatProduct make_preferred_from_gj (String product_type, JSONObject event, boolean delete_ok) {
		int index = 0;
		JSONObject gj_product = GeoJsonUtils.getJsonObject (event, "properties", "products", product_type, index);
		if (gj_product != null) {
			return make_from_gj (gj_product, delete_ok);
		}
		return null;
	}




	// Make a list of products from a GeoJson event.
	//  product_type = Type of product.
	//  event = GeoJson containing the event.
	//  delete_ok = True if delete products are OK, false if not (defaults to false if omitted).
	// Returns empty list if products do not exist, or if data missing or mis-formatted.
	// Omits delete products if delete_ok is false or omitted.
	// Note: A subclass may wish to provide versions that return the type of the subclass.

	public static List<ComcatProduct> make_list_from_gj (String product_type, JSONObject event) {
		return make_list_from_gj (product_type, event, false);
	}

	public static List<ComcatProduct> make_list_from_gj (String product_type, JSONObject event, boolean delete_ok) {
		ArrayList<ComcatProduct> the_product_list = new ArrayList<ComcatProduct>();

		// Get the array of products

		JSONArray gj_product_array = GeoJsonUtils.getJsonArray (event, "properties", "products", product_type);
		if (gj_product_array != null) {

			// Loop over products in the array

			for (int k = 0; k < gj_product_array.size(); ++k) {

				// Make the product

				JSONObject gj_product = GeoJsonUtils.getJsonObject (gj_product_array, k);
				if (gj_product != null) {
					ComcatProduct the_product = make_from_gj (gj_product, delete_ok);

					// Add product to the List

					if (the_product != null) {
						the_product_list.add (the_product);
					}
				}
			}
		}

		return the_product_list;
	}




	// Return the given string, or "<null>" if it is null.

	protected final String str_or_null (String s) {
		if (s == null) {
			return "<null>";
		}
		return s;
	}




	//----- Functions for displaying the product -----




	// Make a string representation.

	@Override
	public String toString () {
		StringBuilder sb = new StringBuilder();
		sb.append ("status = " + (isDelete ? "DELETE" : "UPDATE") + "\n");
		sb.append ("eventID = " + eventID + "\n");
		sb.append ("sourceID = " + sourceID + "\n");
		sb.append ("eventNetwork = " + eventNetwork + "\n");
		sb.append ("eventCode = " + eventCode + "\n");
		sb.append ("isReviewed = " + isReviewed + "\n");
		sb.append ("updateTime = " + updateTime + "\n");
		if (inlineText != null) {
			sb.append ("inlineText = " + inlineText + "\n");
		}
		if (inlineFile != null) {
			sb.append ("inlineFile" + ":" + "\n");
			sb.append ("  contentType = " + str_or_null(inlineFile.contentType) + "\n");
			sb.append ("  lastModified = " + inlineFile.lastModified + "\n");
			sb.append ("  length = " + inlineFile.length + "\n");
			sb.append ("  sha256 = " + str_or_null(inlineFile.sha256) + "\n");
		}
		for (String propname : propertyStrings.keySet()) {
			sb.append ("prop/" + propname + " = " + propertyStrings.get (propname) + "\n");
		}
		for (String filename : productFiles.keySet()) {
			sb.append ("File " + filename + ":" + "\n");
			ProductFile the_file = productFiles.get (filename);
			sb.append ("  contentType = " + the_file.contentType + "\n");
			sb.append ("  url = " + the_file.url + "\n");
			sb.append ("  lastModified = " + the_file.lastModified + "\n");
			sb.append ("  length = " + the_file.length + "\n");
			sb.append ("  sha256 = " + str_or_null(the_file.sha256) + "\n");
		}
		return sb.toString();
	}




	// Make a one-line summary string.

	public String summary_string () {
		StringBuilder sb = new StringBuilder();
		if (isDelete) {
			sb.append ("DELETE, ");
		}
		sb.append ("code = " + eventID);
		if (!( sourceID.equalsIgnoreCase ("us") )) {
			sb.append (", source = " + sourceID);
		}
		sb.append (", reviewed = " + isReviewed);
		sb.append (", time = " + SimpleUtils.time_raw_and_string (updateTime));
		return sb.toString();
	}




	//----- Functions for finding products in Comcat -----




	// Scan Comcat and build a list of events that have the given type of product.
	// Parameters:
	//  product_type = Type of product.
	//  f_prod = True to read from prod-Comcat, false to read from dev-Comcat,
	//           null to read from the configured PDL destination.
	//  startTime = Start of time interval, in milliseconds after the epoch.
	//  endTime = End of time interval, in milliseconds after the epoch.
	//  minDepth = Minimum depth, in km.
	//  maxDepth = Maximum depth, in km.
	//  region = Region to search.
	//  minMag = Minimum magnitude.
	//  includeDeleted = True to include deleted events and events where all products were deleted.
	// Returns a list of event IDs that contain an OAF product.

	public static List<String> findProductEvents (String product_type, Boolean f_prod,
			long startTime, long endTime, double minDepth, double maxDepth, ComcatRegion region,
			double minMag, boolean includeDeleted) {

		// Server configuration

		ServerConfig server_config = new ServerConfig();

		// Get flag indicating if we should read products from production

		boolean f_use_prod = server_config.get_is_pdl_readback_prod();

		if (f_prod != null) {
			f_use_prod = f_prod.booleanValue();
		}

		// Get our source code (typically "us")

		//final String our_source = server_config.get_pdl_oaf_source();
		
		// Get the accessor

		ComcatOAFAccessor accessor = new ComcatOAFAccessor (true, f_use_prod);

		// Make an empty list

		final List<String> eventList = new ArrayList<String>();

		// Visitor for building the list

		ComcatVisitor visitor = new ComcatVisitor() {
			@Override
			public int visit (ObsEqkRupture rup, JsonEvent geojson) {
				eventList.add (rup.getEventId());
				return 0;
			}
		};

		// Call Comcat

		String rup_event_id = null;
		boolean wrapLon = false;
		boolean extendedInfo = true;

		String productType = product_type;

		int visit_result = accessor.visitEventList (visitor, rup_event_id, startTime, endTime,
				minDepth, maxDepth, region, wrapLon, extendedInfo,
				minMag, productType, includeDeleted);

		System.out.println ("Count of events with '" + product_type + "' products = " + eventList.size());

		// Return the list

		return eventList;
	}




	//----- Functions for access to content files -----




	// Open an input stream from a URL.
	// Parameters:
	//  url_spec = String to parse as a URL.
	// Returns a list of strings containing the lines of the file.
	// Throws ComcatException if any error.
	// Note: This should only be used for URLs obtained from Comcat
	// (e.g., in the contents section of a product).  Any error that
	// occurs is properly considered to be a Comcat error.
	// Note: ComcatContentFileException indicates an error that is
	// likely to be permanent, and therefore should not be retried.
	// Important: The caller must close the stream!  The use of
	// try-with-resources is highly recommended.

	public static InputStream open_stream_for_url (String url_spec) throws IOException {

		// Construct the URL
		// Throws MalformedURLException (subclass of IOException) if error in forming URL.

		URL url = null;
		try {
			url = new URL (url_spec);
		}
		catch (Exception e) {
			throw new ComcatContentFileException ("Malformed URL for access to Comcat content file (this is a permanent failure): URL = " + url_spec, e);
		}

		// Create a connection.
		// This is a local operation that does not call out to the network.
		// Throws IOException if there is a problem.
		// The object returned should be an HttpURLConnection (subclass of URLConnection).

		URLConnection conn = url.openConnection();

		// Here we could call conn.addRequestProperty() to add properties.

		// Set the timeouts, if desired.

		if (PRODFILE_CONNECT_TIMEOUT >= 0) {
			conn.setConnectTimeout (PRODFILE_CONNECT_TIMEOUT);
		}

		if (PRODFILE_READ_TIMEOUT >= 0) {
			conn.setReadTimeout (PRODFILE_READ_TIMEOUT);
		}

		// Connect to Comcat.
		// Throws SocketTimeoutException (subclass of IOException) if timeout trying to establish connection.
		// Throws IOException if there is some other problem.

		conn.connect();

		// Here is where we can examine the HTTP status code.
		// The connection should be an HTTP connection.

		if (conn instanceof HttpURLConnection) {
			HttpURLConnection http_conn = (HttpURLConnection)conn;

			// This is the HTTP status code

			int http_status_code = http_conn.getResponseCode();

			switch (http_status_code) {
			
			case HttpURLConnection.HTTP_OK:				// 200 = OK
			case HttpURLConnection.HTTP_PARTIAL:		// 206 = Partial content
			case -1:									// -1 = unknown status (getResponseCode returns -1 if it can't determine the status code)
				break;									// continue
			
			case HttpURLConnection.HTTP_BAD_REQUEST:	// 400 = Bad request
			case HttpURLConnection.HTTP_FORBIDDEN:		// 403 = Forbidden
			case HttpURLConnection.HTTP_NOT_FOUND:		// 404 = Not found
			case HttpURLConnection.HTTP_GONE:			// 410 = Gone
			case HttpURLConnection.HTTP_REQ_TOO_LONG:	// 414 = Request-URI too long
				throw new ComcatContentFileException ("Access to Comcat content file failed with HTTP status code " + http_status_code + " (this is a permanent failure), URL = " + url_spec);

			default:									// any other status code is an error
				throw new IOException ("Access to Comcat content file failed with HTTP status code " + http_status_code + ", URL = " + url_spec);
			}
		}

		// Open the stream

		return conn.getInputStream();
	}




	// Read all the bytes from a URL into a buffer.
	// Parameters:
	//  url_spec = String to parse as a URL.
	//  expected_length = Expected number of bytes, or -1L if not specified.
	//  maximum_length = Maximum allowed number of bytes, or -1L if not specified.
	// Returns a buffer containing all the bytes of the URL.
	// Note: ComcatContentFileException indicates an error that is
	// likely to be permanent, and therefore should not be retried.
	// Note: Throws ComcatContentFileException if number of bytes received
	// is greater than maximum, or not equal to expected.

	public static ByteArrayOutputStream read_byte_buffer_from_url (String url_spec, long expected_length, long maximum_length) throws IOException {
		ByteArrayOutputStream result = new ByteArrayOutputStream();

		// Check for expected length longer than maximum length

		if (expected_length >= 0L && maximum_length >= 0L && expected_length > maximum_length) {
			throw new ComcatContentFileException ("Comcat content file expected length (" + expected_length + ") exceeds maximum length (" + maximum_length + "), URL = " + url_spec);
		}

		// Open the input stream

		try (
			InputStream stream =  open_stream_for_url (url_spec);
		){
			long total_length = 0L;
			byte[] buffer = new byte[1048576];		// 1 MB

			// Loop, reading data one buffer at a time

			for (int length = stream.read(buffer); length > 0; length = stream.read(buffer)) {

				// Check total length against limits

				total_length += ((long)length);
				if (expected_length >= 0L && total_length > expected_length) {
					throw new ComcatContentFileException ("Comcat content file length exceeds expected length (" + expected_length + "), URL = " + url_spec);
				}
				if (maximum_length >= 0L && total_length > maximum_length) {
					throw new ComcatContentFileException ("Comcat content file length exceeds maximum length (" + maximum_length + "), URL = " + url_spec);
				}

				// Write the data into the result buffer

				result.write(buffer, 0, length);
			}

			// Check final length against expected length

			if (expected_length >= 0L && total_length != expected_length) {
				throw new ComcatContentFileException ("Comcat content file length (" + total_length + ") differs from expected length (" + expected_length + "), URL = " + url_spec);
			}
		}

		return result;
	}




	// Read all the bytes from a URL into a byte array.
	// Parameters:
	//  url_spec = String to parse as a URL.
	//  expected_length = Expected number of bytes, or -1L if not specified.
	//  maximum_length = Maximum allowed number of bytes, or -1L if not specified.
	// Returns a byte array containing all the bytes of the URL.
	// Note: ComcatContentFileException indicates an error that is
	// likely to be permanent, and therefore should not be retried.
	// Note: Throws ComcatContentFileException if number of bytes received
	// is greater than maximum, or not equal to expected.

	public static byte[] read_all_bytes_from_url (String url_spec, long expected_length, long maximum_length) throws IOException {
		return read_byte_buffer_from_url (url_spec, expected_length, maximum_length).toByteArray();
	}




	// Read all the bytes from a URL into a string.
	// Parameters:
	//  url_spec = String to parse as a URL.
	//  expected_length = Expected number of bytes, or -1L if not specified.
	//  maximum_length = Maximum allowed number of bytes, or -1L if not specified.
	// Returns a string containing the entire contents of the URL.
	// Note: ComcatContentFileException indicates an error that is
	// likely to be permanent, and therefore should not be retried.
	// Note: Throws ComcatContentFileException if number of bytes received
	// is greater than maximum, or not equal to expected.
	// Note: Counts refer to the number of bytes, which is not necessarity equal
	// to the number of characters.

	public static String read_string_from_url (String url_spec, long expected_length, long maximum_length) throws IOException {
		return read_byte_buffer_from_url (url_spec, expected_length, maximum_length).toString(StandardCharsets.UTF_8);
	}




	// Read all the bytes from a content file into a byte array.
	// Parameters:
	//  filename = Filename to read (from contents).
	// Returns a byte array containing all the bytes of the URL.
	// Note: ComcatUnrecoverableException indicates an error that is
	// likely to be permanent, and therefore should not be retried.

	public byte[] read_all_bytes_from_contents (String filename) {
	
		// Get the URL from the contents list.

		ProductFile product_file = productFiles.get (filename);
		if (product_file == null) {
			throw new ComcatUnrecoverableException ("ComcatProduct.read_all_bytes_from_contents: Content file is not present in the product, filename = " + filename);
		}

		// Read from the URL

		byte[] result;

		try {
			result = read_all_bytes_from_url (product_file.url, product_file.length, PRODFILE_MAX_LENGTH);
		}
		catch (ComcatContentFileException e) {
			throw new ComcatUnrecoverableException ("ComcatProduct.read_all_bytes_from_contents: Unrecoverable error reading from content file, filename = " + filename, e);
		}
		catch (IOException e) {
			throw new ComcatException ("ComcatProduct.read_all_bytes_from_contents: I/O error reading from content file, filename = " + filename, e);
		}
		catch (Exception e) {
			throw new ComcatException ("ComcatProduct.read_all_bytes_from_contents: Error reading from content file, filename = " + filename, e);
		}

		return result;
	}




	// Read all the bytes from a content file into a string.
	// Parameters:
	//  filename = Filename to read (from contents).
	// Returns a string containing the entire contents of the URL.
	// Note: ComcatUnrecoverableException indicates an error that is
	// likely to be permanent, and therefore should not be retried.

	public String read_string_from_contents (String filename) {
	
		// Get the URL from the contents list.

		ProductFile product_file = productFiles.get (filename);
		if (product_file == null) {
			throw new ComcatUnrecoverableException ("ComcatProduct.read_string_from_contents: Content file is not present in the product, filename = " + filename);
		}

		// Read from the URL

		String result;

		try {
			result = read_string_from_url (product_file.url, product_file.length, PRODFILE_MAX_LENGTH);
		}
		catch (ComcatContentFileException e) {
			throw new ComcatUnrecoverableException ("ComcatProduct.read_string_from_contents: Unrecoverable error reading from content file, filename = " + filename, e);
		}
		catch (IOException e) {
			throw new ComcatException ("ComcatProduct.read_string_from_contents: I/O error reading from content file, filename = " + filename, e);
		}
		catch (Exception e) {
			throw new ComcatException ("ComcatProduct.read_string_from_contents: Error reading from content file, filename = " + filename, e);
		}

		return result;
	}




	// Read all the lines of text from a content file.
	// Parameters:
	//  filename = Filename to read (from contents).
	// Returns a list of strings containing the lines of the file.
	// Throws ComcatException if any error.
	// Note: ComcatUnrecoverableException indicates an error that is
	// likely to be permanent, and therefore should not be retried.
	//
	// Implementation note: We read the entire file into a string and
	// then break it into lines, rather than break the file into lines
	// on-the-fly as it is read, to enable error checking that depends
	// on having the entire file in memory.

	public List<String> read_all_lines_from_contents (String filename) {

		// Get the contents as a string

		String s = read_string_from_contents (filename);

		// Break the contents into lines

		ArrayList<String> lines = new ArrayList<String>();

		try (
			BufferedReader br = new BufferedReader (new StringReader (s));
		){
			for (String line = br.readLine(); line != null; line = br.readLine()) {
				lines.add (line);
			}
		}
		catch (IOException e) {
			throw new ComcatUnrecoverableException ("ComcatProduct.read_all_lines_from_contents: I/O error splitting file contents into lines, filename = " + filename, e);
		}
		catch (Exception e) {
			throw new ComcatUnrecoverableException ("ComcatProduct.read_all_lines_from_contents: Error splitting file contents into lines, filename = " + filename, e);
		}

		return lines;
	}




	// Read a JSON object from a content file.
	// Parameters:
	//  filename = Filename to read (from contents).
	// Returns a JSONObject containing the contents of the file.
	// Returns null if the file is read successfully, but does not parse as a JSON object.
	// Throws ComcatException if any error.
	// Note: ComcatUnrecoverableException indicates an error that is
	// likely to be permanent, and therefore should not be retried.
	//
	// Implementation note: We read the entire file into a string and
	// then parse it as JSON, rather than parse the file as JSON
	// on-the-fly as it is read, to enable error checking that depends
	// on having the entire file in memory.

	public JSONObject read_json_obj_from_contents (String filename) {

		// Get the contents as a string

		String s = read_string_from_contents (filename);

		// Scan the string to form a JSON object

		JSONObject result = null;

		try {
			Object o = JSONValue.parseWithException (s);
			if (o != null) {
				if (o instanceof JSONObject) {
					result = (JSONObject) o;
				}
			}
		}
		catch (ParseException e) {
			result = null;
		}
		catch (Exception e) {
			result = null;
		}

		return result;
	}




	//----- Utility functions -----




	// Make a Location object for the given latitude, longitude, depth(km).
	// Returns the Location object.
	// Throws exception if the arguments are invalid.
	// This function is permissive, in that:
	//  * Latitude slightly out-of-range -90 to +90 is coerced into range.
	//  * Longitude can range from -360 to +360 (even through Location only accepts
	//    -180 to +360); longitude slightly out-of-range is coerced into range;
	//    and all longitudes are coerced into range -180 to +180.
	//  * Depth is coerced into range 0 to 700 (with no limit).
	// Note: The purpose of permissiveness is to avoid spurious errors when coordinates are
	// sligtly out-of-range due to imprecise conversions (degrees/radians, or double/string).

	public static Location permissive_make_loc (double lat, double lon, double depth) {

		if (lat < -90.001 || lat > 90.001) {
			throw new IllegalArgumentException ("ComcatProduct.permissive_make_loc: Invalid latitude: lat lon depth = " + lat + " " + lon + " " + depth);
		}
		if (lat > 90.0) {
			lat = 90.0;
		}
		if (lat < -90.0) {
			lat = -90.0;
		}

		if (lon < -360.001 || lon > 360.001) {
			throw new IllegalArgumentException ("ComcatProduct.permissive_make_loc: Invalid longitude: lat lon depth = " + lat + " " + lon + " " + depth);
		}
		if (lon > 180.0) {
			lon -= 360.0;
		}
		if (lon < -180.0) {
			lon += 360.0;
		}

		if (depth < 0.0) {
			depth = 0.0;
		}
		if (depth > GeoTools.DEPTH_MAX) {
			depth = GeoTools.DEPTH_MAX;
		}

		return new Location (lat, lon, depth);
	}




	// Make a Location object for the given latitude, longitude, depth(km), given as Double.
	// Returns the Location object.
	// Throws exception if the arguments are invalid.
	// This function is permissive, in that:
	//  * Latitude slightly out-of-range -90 to +90 is coerced into range.
	//  * Longitude can range from -360 to +360 (even through Location only accepts
	//    -180 to +360); longitude slightly out-of-range is coerced into range;
	//    and all longitudes are coerced into range -180 to +180.
	//  * Depth is coerced into range 0 to 700 (with no limit).
	// Note: The purpose of permissiveness is to avoid spurious errors when coordinates are
	// sligtly out-of-range due to imprecise conversions (degrees/radians, or double/string).

	public static Location permissive_make_loc_from_obj (Double lat, Double lon, Double depth) {

		if (lat == null || lon == null || depth == null) {
			throw new IllegalArgumentException ("ComcatProduct.permissive_make_loc: Missing coordinates: lat lon depth = "
				+ ((lat == null) ? "null" : lat.toString()) + " "
				+ ((lon == null) ? "null" : lon.toString()) + " "
				+ ((depth == null) ? "null" : depth.toString()) );
		}

		return permissive_make_loc (lat.doubleValue(), lon.doubleValue(), depth.doubleValue());
	}




	// Make a Location object for the given latitude, longitude, depth(km), given as String.
	// Returns the Location object.
	// Throws exception if the arguments are invalid.
	// This function is permissive, in that:
	//  * Latitude slightly out-of-range -90 to +90 is coerced into range.
	//  * Longitude can range from -360 to +360 (even through Location only accepts
	//    -180 to +360); longitude slightly out-of-range is coerced into range;
	//    and all longitudes are coerced into range -180 to +180.
	//  * Depth is coerced into range 0 to 700 (with no limit).
	// Note: The purpose of permissiveness is to avoid spurious errors when coordinates are
	// sligtly out-of-range due to imprecise conversions (degrees/radians, or double/string).

	public static Location permissive_make_loc_from_string (String lat, String lon, String depth) {

		if (lat == null || lon == null || depth == null) {
			throw new IllegalArgumentException ("ComcatProduct.permissive_make_loc_from_string: Missing coordinates: lat lon depth = '"
				+ ((lat == null) ? "null" : lat) + "' '"
				+ ((lon == null) ? "null" : lon) + "' '"
				+ ((depth == null) ? "null" : depth) + "'" );
		}

		double r_lat;
		double r_lon;
		double r_depth;

		try {
			r_lat = Double.parseDouble (lat);
			r_lon = Double.parseDouble (lon);
			r_depth = Double.parseDouble (depth);
		}
		catch (Exception e) {
			throw new IllegalArgumentException ("ComcatProduct.permissive_make_loc_from_string: Malformed coordinates: lat lon depth = '"
				+ ((lat == null) ? "null" : lat) + "' '"
				+ ((lon == null) ? "null" : lon) + "' '"
				+ ((depth == null) ? "null" : depth) + "'", e);
		}

		return permissive_make_loc (r_lat, r_lon, r_depth);
	}




	//----- Testing -----




	public static void main(String[] args) {

		// There needs to be at least one argument, which is the subcommand

		if (args.length < 1) {
			System.err.println ("ComcatProduct : Missing subcommand");
			return;
		}




		// Subcommand : Test #1
		// Command format:
		//  test1  f_use_prod  f_use_feed  event_id  [product_type]
		// Fetch information for an event, and display it.
		// Then construct the preferred product for the event, and display it.

		if (args[0].equalsIgnoreCase ("test1")) {

			// Additional arguments

			if (args.length != 4 && args.length != 5) {
				System.err.println ("ComcatProduct : Invalid 'test1' subcommand");
				return;
			}

			boolean f_use_prod = Boolean.parseBoolean (args[1]);
			boolean f_use_feed = Boolean.parseBoolean (args[2]);
			String event_id = args[3];

			String product_type = default_product_type;
			if (args.length >= 5) {
				product_type = args[4];
				if (!( is_valid_product_type (product_type) )) {
					System.out.println ("Invalid product_type: " + product_type);
					System.out.println ("Continuing anyway ...");
					System.out.println ("");
				}
			}

			try {

				// Say hello

				System.out.println ("Fetching event: " + event_id);
				System.out.println ("f_use_prod: " + f_use_prod);
				System.out.println ("f_use_feed: " + f_use_feed);
				System.out.println ("product_type: " + product_type);
				System.out.println ("");

				// Create the accessor

				ComcatOAFAccessor accessor = new ComcatOAFAccessor (true, f_use_prod, f_use_feed);

				// Get the rupture

				ObsEqkRupture rup = accessor.fetchEvent (event_id, false, true);

				// Display its information

				if (rup == null) {
					System.out.println ("Null return from fetchEvent");
					System.out.println ("http_status = " + accessor.get_http_status_code());
					System.out.println ("URL = " + accessor.get_last_url_as_string());
					return;
				}

				System.out.println (ComcatOAFAccessor.rupToString (rup));

				String rup_event_id = rup.getEventId();

				System.out.println ("http_status = " + accessor.get_http_status_code());

				Map<String, String> eimap = ComcatOAFAccessor.extendedInfoToMap (rup, ComcatOAFAccessor.EITMOPT_NULL_TO_EMPTY);

				for (String key : eimap.keySet()) {
					System.out.println ("EI Map: " + key + " = " + eimap.get(key));
				}

				List<String> idlist = ComcatOAFAccessor.idsToList (eimap.get (ComcatOAFAccessor.PARAM_NAME_IDLIST), rup_event_id);

				for (String id : idlist) {
					System.out.println ("ID List: " + id);
				}

				System.out.println ("URL = " + accessor.get_last_url_as_string());

				// Get the preferred product

				//  ComcatProduct preferred_product = make_preferred_from_gj (accessor.get_last_geojson());

				ComcatProduct preferred_product = make_preferred_from_gj (product_type, accessor.get_last_geojson());

				if (preferred_product == null) {
					System.out.println ();
					System.out.println ("Preferred product = None");
				}

				else {
					System.out.println ();
					System.out.println ("Preferred product:" );
					System.out.println (preferred_product.toString());
					System.out.println ("Summary: " + preferred_product.summary_string());
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #2
		// Command format:
		//  test2  f_use_prod  f_use_feed  event_id  superseded  delete_ok  [product_type]
		// Fetch information for an event, and display it.
		// Then construct the list of products for the event, and display it.

		if (args[0].equalsIgnoreCase ("test2")) {

			// Additional arguments

			if (args.length != 6 && args.length != 7) {
				System.err.println ("ComcatProduct : Invalid 'test2' subcommand");
				return;
			}

			boolean f_use_prod = Boolean.parseBoolean (args[1]);
			boolean f_use_feed = Boolean.parseBoolean (args[2]);
			String event_id = args[3];
			boolean superseded = Boolean.parseBoolean (args[4]);
			boolean delete_ok = Boolean.parseBoolean (args[5]);

			String product_type = default_product_type;
			if (args.length >= 7) {
				product_type = args[6];
				if (!( is_valid_product_type (product_type) )) {
					System.out.println ("Invalid product_type: " + product_type);
					System.out.println ("Continuing anyway ...");
					System.out.println ("");
				}
			}

			try {

				// Say hello

				System.out.println ("Fetching event: " + event_id);
				System.out.println ("f_use_prod: " + f_use_prod);
				System.out.println ("f_use_feed: " + f_use_feed);
				System.out.println ("superseded: " + superseded);
				System.out.println ("delete_ok: " + delete_ok);
				System.out.println ("product_type: " + product_type);
				System.out.println ("");

				// Create the accessor

				ComcatOAFAccessor accessor = new ComcatOAFAccessor (true, f_use_prod, f_use_feed);

				// Get the rupture

				ObsEqkRupture rup = accessor.fetchEvent (event_id, false, true, superseded);

				// Display its information

				if (rup == null) {
					System.out.println ("Null return from fetchEvent");
					System.out.println ("http_status = " + accessor.get_http_status_code());
					System.out.println ("URL = " + accessor.get_last_url_as_string());
					return;
				}

				System.out.println (ComcatOAFAccessor.rupToString (rup));

				String rup_event_id = rup.getEventId();

				System.out.println ("http_status = " + accessor.get_http_status_code());

				Map<String, String> eimap = ComcatOAFAccessor.extendedInfoToMap (rup, ComcatOAFAccessor.EITMOPT_NULL_TO_EMPTY);

				for (String key : eimap.keySet()) {
					System.out.println ("EI Map: " + key + " = " + eimap.get(key));
				}

				List<String> idlist = ComcatOAFAccessor.idsToList (eimap.get (ComcatOAFAccessor.PARAM_NAME_IDLIST), rup_event_id);

				for (String id : idlist) {
					System.out.println ("ID List: " + id);
				}

				System.out.println ("URL = " + accessor.get_last_url_as_string());

				// Get the product list

				//  List<ComcatProduct> product_list = make_list_from_gj (accessor.get_last_geojson(), delete_ok);

				List<ComcatProduct> product_list = make_list_from_gj (product_type, accessor.get_last_geojson(), delete_ok);

				for (int k = 0; k < product_list.size(); ++k) {
					System.out.println ();
					System.out.println ("Product " + k);
					System.out.println (product_list.get(k).toString());
					System.out.println ("Summary: " + product_list.get(k).summary_string());
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #3
		// Command format:
		//  test3  pdl_enable  start_time  end_time  min_mag  [product_type]
		// Set the PDL enable according to pdl_enable (see ServerConfigFile) (0 = none, 1 = dev, 2 = prod, ...).
		// Then call findProductEvents and display the result.
		// Times are ISO-8601 format, for example 2011-12-03T10:15:30Z.

		if (args[0].equalsIgnoreCase ("test3")) {

			// Additional arguments

			if (args.length != 5 && args.length != 6) {
				System.err.println ("ComcatProduct : Invalid 'test3' subcommand");
				return;
			}

			try {

				int pdl_enable = Integer.parseInt (args[1]);
				long startTime = SimpleUtils.string_to_time (args[2]);
				long endTime = SimpleUtils.string_to_time (args[3]);
				double minMag = Double.parseDouble (args[4]);

				String product_type = default_product_type;
				if (args.length >= 6) {
					product_type = args[5];
					if (!( is_valid_product_type (product_type) )) {
						System.out.println ("Invalid product_type: " + product_type);
						System.out.println ("Continuing anyway ...");
						System.out.println ("");
					}
				}

				// Set the PDL enable code

				if (pdl_enable < ServerConfigFile.PDLOPT_MIN || pdl_enable > ServerConfigFile.PDLOPT_MAX) {
					System.out.println ("Invalid pdl_enable = " + pdl_enable);
					return;
				}

				ServerConfig server_config = new ServerConfig();
				server_config.get_server_config_file().pdl_enable = pdl_enable;

				// Say hello

				System.out.println ("PDL enable: " + pdl_enable);
				System.out.println ("Start time: " + SimpleUtils.time_to_string(startTime));
				System.out.println ("End time: " + SimpleUtils.time_to_string(endTime));
				System.out.println ("Minimum magnitude: " + minMag);
				System.out.println ("product_type: " + product_type);
				System.out.println ("");

				// Make the call

				SphRegionWorld region = new SphRegionWorld ();
				double minDepth = ComcatOAFAccessor.DEFAULT_MIN_DEPTH;
				double maxDepth = ComcatOAFAccessor.DEFAULT_MAX_DEPTH;
		
				boolean includeDeleted = false;

				//  List<String> eventList = findProductEvents (null,
				//  	startTime, endTime, minDepth, maxDepth, region,
				//  	minMag, includeDeleted);

				List<String> eventList = findProductEvents (product_type, null,
					startTime, endTime, minDepth, maxDepth, region,
					minMag, includeDeleted);

				// Display the number of items returned

				System.out.println ("Number of items returned by findProductEvents: " + eventList.size());

				// Display the list, up to a maximum size

				int nmax = 100;
				int n = Math.min (nmax, eventList.size());

				for (int i = 0; i < n; ++i) {
					System.out.println (eventList.get(i));
				}

				if (n < eventList.size()) {
					System.out.println ("Plus " + (eventList.size() - n) + " more");
				}

			}

			catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #4
		// Command format:
		//  test4  f_use_prod  f_use_feed  event_id  [product_type]
		// Fetch information for an event, and display it.
		// Then construct the preferred product for the event, and display it.
		// Then display any text files in the contents.
		// Same as test #1 except with addition of text file contents.

		if (args[0].equalsIgnoreCase ("test4")) {

			// Additional arguments

			if (args.length != 4 && args.length != 5) {
				System.err.println ("ComcatProduct : Invalid 'test4' subcommand");
				return;
			}

			boolean f_use_prod = Boolean.parseBoolean (args[1]);
			boolean f_use_feed = Boolean.parseBoolean (args[2]);
			String event_id = args[3];

			String product_type = default_product_type;
			if (args.length >= 5) {
				product_type = args[4];
				if (!( is_valid_product_type (product_type) )) {
					System.out.println ("Invalid product_type: " + product_type);
					System.out.println ("Continuing anyway ...");
					System.out.println ("");
				}
			}

			try {

				// Say hello

				System.out.println ("Fetching event: " + event_id);
				System.out.println ("f_use_prod: " + f_use_prod);
				System.out.println ("f_use_feed: " + f_use_feed);
				System.out.println ("product_type: " + product_type);
				System.out.println ("");

				// Create the accessor

				ComcatOAFAccessor accessor = new ComcatOAFAccessor (true, f_use_prod, f_use_feed);

				// Get the rupture

				ObsEqkRupture rup = accessor.fetchEvent (event_id, false, true);

				// Display its information

				if (rup == null) {
					System.out.println ("Null return from fetchEvent");
					System.out.println ("http_status = " + accessor.get_http_status_code());
					System.out.println ("URL = " + accessor.get_last_url_as_string());
					return;
				}

				System.out.println (ComcatOAFAccessor.rupToString (rup));

				String rup_event_id = rup.getEventId();

				System.out.println ("http_status = " + accessor.get_http_status_code());

				Map<String, String> eimap = ComcatOAFAccessor.extendedInfoToMap (rup, ComcatOAFAccessor.EITMOPT_NULL_TO_EMPTY);

				for (String key : eimap.keySet()) {
					System.out.println ("EI Map: " + key + " = " + eimap.get(key));
				}

				List<String> idlist = ComcatOAFAccessor.idsToList (eimap.get (ComcatOAFAccessor.PARAM_NAME_IDLIST), rup_event_id);

				for (String id : idlist) {
					System.out.println ("ID List: " + id);
				}

				System.out.println ("URL = " + accessor.get_last_url_as_string());

				// Get the preferred product

				//  ComcatProduct preferred_product = make_preferred_from_gj (accessor.get_last_geojson());

				ComcatProduct preferred_product = make_preferred_from_gj (product_type, accessor.get_last_geojson());

				if (preferred_product == null) {
					System.out.println ();
					System.out.println ("Preferred product = None");
				}

				else {
					System.out.println ();
					System.out.println ("Preferred product:" );
					System.out.println (preferred_product.toString());
					System.out.println ("Summary: " + preferred_product.summary_string());
				}

				// Scan for text files

				if (preferred_product != null) {
					for (String fname : preferred_product.productFiles.keySet()) {
						if (preferred_product.productFiles.get(fname).contentType.equals (PDLProductFile.TEXT_PLAIN)) {
							List<String> lines = preferred_product.read_all_lines_from_contents (fname);
							System.out.println ();
							int nlines = lines.size();
							if (nlines <= 100) {
								System.out.println ("Text file: " + fname + " (" + nlines + " lines)");
							} else {
								System.out.println ("Text file: " + fname + " (showing 100 of " + nlines + " lines)");
								nlines = 100;
							}
							System.out.println ();
							for (int nline = 0; nline < nlines; ++nline) {
								System.out.println (lines.get(nline));
							}
						}
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #5
		// Skipped.




		// Subcommand : Test #6
		// Command format:
		//  test6  pdl_enable  start_time  end_time  min_mag  include_deleted  [product_type]
		// Set the PDL enable according to pdl_enable (see ServerConfigFile) (0 = none, 1 = dev, 2 = prod, ...).
		// Then call findProductEvents and display the result.
		// Times are ISO-8601 format, for example 2011-12-03T10:15:30Z.
		// Same as test #3 with the include_deleted option added.

		if (args[0].equalsIgnoreCase ("test6")) {

			// Additional arguments

			if (args.length != 6 && args.length != 7) {
				System.err.println ("ComcatProduct : Invalid 'test6' subcommand");
				return;
			}

			try {

				int pdl_enable = Integer.parseInt (args[1]);
				long startTime = SimpleUtils.string_to_time (args[2]);
				long endTime = SimpleUtils.string_to_time (args[3]);
				double minMag = Double.parseDouble (args[4]);
				boolean includeDeleted = Boolean.parseBoolean (args[5]);

				String product_type = default_product_type;
				if (args.length >= 7) {
					product_type = args[6];
					if (!( is_valid_product_type (product_type) )) {
						System.out.println ("Invalid product_type: " + product_type);
						System.out.println ("Continuing anyway ...");
						System.out.println ("");
					}
				}

				// Set the PDL enable code

				if (pdl_enable < ServerConfigFile.PDLOPT_MIN || pdl_enable > ServerConfigFile.PDLOPT_MAX) {
					System.out.println ("Invalid pdl_enable = " + pdl_enable);
					return;
				}

				ServerConfig server_config = new ServerConfig();
				server_config.get_server_config_file().pdl_enable = pdl_enable;

				// Say hello

				System.out.println ("PDL enable: " + pdl_enable);
				System.out.println ("Start time: " + SimpleUtils.time_to_string(startTime));
				System.out.println ("End time: " + SimpleUtils.time_to_string(endTime));
				System.out.println ("Minimum magnitude: " + minMag);
				System.out.println ("include_deleted: " + includeDeleted);
				System.out.println ("product_type: " + product_type);
				System.out.println ("");

				// Make the call

				SphRegionWorld region = new SphRegionWorld ();
				double minDepth = ComcatOAFAccessor.DEFAULT_MIN_DEPTH;
				double maxDepth = ComcatOAFAccessor.DEFAULT_MAX_DEPTH;

				//  List<String> eventList = findProductEvents (null,
				//  	startTime, endTime, minDepth, maxDepth, region,
				//  	minMag, includeDeleted);

				List<String> eventList = findProductEvents (product_type, null,
					startTime, endTime, minDepth, maxDepth, region,
					minMag, includeDeleted);

				// Display the number of items returned

				System.out.println ("Number of items returned by findProductEvents: " + eventList.size());

				// Display the list, up to a maximum size

				int nmax = 100;
				int n = Math.min (nmax, eventList.size());

				for (int i = 0; i < n; ++i) {
					System.out.println (eventList.get(i));
				}

				if (n < eventList.size()) {
					System.out.println ("Plus " + (eventList.size() - n) + " more");
				}

			}

			catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Subcommand : Test #7
		// Command format:
		//  test7  f_use_prod  f_use_feed  event_id  filename  [product_type]
		// Fetch information for an event, and display it.
		// Then construct the preferred product for the event, and display it.
		// Then display any text files in the contents.
		// Same as test #4 except user can select a file to display.

		if (args[0].equalsIgnoreCase ("test7")) {

			// Additional arguments

			if (args.length != 5 && args.length != 6) {
				System.err.println ("ComcatProduct : Invalid 'test7' subcommand");
				return;
			}

			try {

				boolean f_use_prod = Boolean.parseBoolean (args[1]);
				boolean f_use_feed = Boolean.parseBoolean (args[2]);
				String event_id = args[3];
				String filename = args[4];

				String product_type = default_product_type;
				if (args.length >= 6) {
					product_type = args[5];
					if (!( is_valid_product_type (product_type) )) {
						System.out.println ("Invalid product_type: " + product_type);
						System.out.println ("Continuing anyway ...");
						System.out.println ("");
					}
				}

				// Say hello

				System.out.println ("Fetching event: " + event_id);
				System.out.println ("f_use_prod: " + f_use_prod);
				System.out.println ("f_use_feed: " + f_use_feed);
				System.out.println ("filename: " + filename);
				System.out.println ("product_type: " + product_type);
				System.out.println ("");

				// Create the accessor

				ComcatOAFAccessor accessor = new ComcatOAFAccessor (true, f_use_prod, f_use_feed);

				// Get the rupture

				ObsEqkRupture rup = accessor.fetchEvent (event_id, false, true);

				// Display its information

				if (rup == null) {
					System.out.println ("Null return from fetchEvent");
					System.out.println ("http_status = " + accessor.get_http_status_code());
					System.out.println ("URL = " + accessor.get_last_url_as_string());
					return;
				}

				System.out.println (ComcatOAFAccessor.rupToString (rup));

				String rup_event_id = rup.getEventId();

				System.out.println ("http_status = " + accessor.get_http_status_code());

				Map<String, String> eimap = ComcatOAFAccessor.extendedInfoToMap (rup, ComcatOAFAccessor.EITMOPT_NULL_TO_EMPTY);

				for (String key : eimap.keySet()) {
					System.out.println ("EI Map: " + key + " = " + eimap.get(key));
				}

				List<String> idlist = ComcatOAFAccessor.idsToList (eimap.get (ComcatOAFAccessor.PARAM_NAME_IDLIST), rup_event_id);

				for (String id : idlist) {
					System.out.println ("ID List: " + id);
				}

				System.out.println ("URL = " + accessor.get_last_url_as_string());

				// Get the preferred product

				//  ComcatProduct preferred_product = make_preferred_from_gj (accessor.get_last_geojson());

				ComcatProduct preferred_product = make_preferred_from_gj (product_type, accessor.get_last_geojson());

				if (preferred_product == null) {
					System.out.println ();
					System.out.println ("Preferred product = None");
				}

				else {
					System.out.println ();
					System.out.println ("Preferred product:" );
					System.out.println (preferred_product.toString());
					System.out.println ("Summary: " + preferred_product.summary_string());
				}

				// See if product contains our filename
					
				boolean f_contains_file = preferred_product.contains_file (filename);
				System.out.println ();
				System.out.println ("Contains file: " + f_contains_file);

				// Read file contents as a string

				String file_contents = preferred_product.read_string_from_contents (filename);

				System.out.println ();
				System.out.println (file_contents);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}




		// Unrecognized subcommand.

		System.err.println ("ComcatProduct : Unrecognized subcommand : " + args[0]);
		return;

	}

}
