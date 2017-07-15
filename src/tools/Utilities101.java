
package tools;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.ZipFile;
import dataModel.DataModel;
import dataModel.Rating;

/**
 * Various helpers 
 */
public class Utilities101 {

	public static float PREDICTION_RELEVANCE_MIN_RATING_FOR_RELEVANCE = -1;
	public static int PREDICTION_RELEVANCE_MIN_PERCENTAGE_ABOVE_AVERAGE = 0;
	public static float MIN_RATING = 1;
	public static float MAX_RATING = 5;
	
	public static Map<Integer, Integer> calculateRatingsPerItem(DataModel dm) {
		Map<Integer, Integer> result = new HashMap<Integer, Integer>();
		for (Rating r : dm.getRatings()) {
			Utilities101.incrementMapValue(result, r.item);
		}
		return result;
	}
	// =====================================================================================

	/**
	 * Sort a Map by value in descending order
	 ** 
	 * @param map
	 * @return a sorted map
	 */
	public static <K, V extends Comparable<? super V>> LinkedHashMap<K, V> sortByValueDescending(
			Map<K, V> map) {
		List<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>(
				map.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
			public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
				return (o2.getValue()).compareTo(o1.getValue());
			}
		});

		LinkedHashMap<K, V> result = new LinkedHashMap<K, V>();
		for (Map.Entry<K, V> entry : list) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}
	
	/**
	 * Sort a Map by key in ascending order
	 ** 
	 * @param map
	 * @return a sorted map
	 */
	public static <K extends Comparable<? super K>, V> Map<K, V> sortByKeyAscending(
			Map<K, V> map) {
		List<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>(
				map.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
			public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
				return (o1.getKey()).compareTo(o2.getKey());
			}
		});

		Map<K, V> result = new LinkedHashMap<K, V>();
		for (Map.Entry<K, V> entry : list) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}
	
	

	// =====================================================================================

	/**
	 * Returns a map with user averages given a set of ratings
	 */
	public static Map<Integer, Float> getUserAverageRatings(Set<Rating> ratings) {
		Map<Integer, Float> result = new HashMap<Integer, Float>();
		Map<Integer, Integer> counters = new HashMap<Integer, Integer>();

		for (Rating r : ratings) {
			Float userAvg = result.get(r.user);
			if (userAvg == null) {
				userAvg = new Float(r.rating);
				result.put(r.user, (float) r.rating);
				counters.put(r.user, 1);
			} else {
				counters.put(r.user, counters.get(r.user) + 1);
				result.put(r.user, result.get(r.user) + r.rating);
			}
		}
		// Divide by number of ratings
		for (Integer user : result.keySet()) {
			result.put(user, result.get(user) / (float) counters.get(user));
		}
		return result;
	}

	// =====================================================================================

	/**
	 * Returns a map with user averages given a set of ratings
	 */
	public static Map<Integer, Float> getItemAverageRatings(Set<Rating> ratings) {
		Map<Integer, Float> result = new HashMap<Integer, Float>();
		Map<Integer, Integer> counters = new HashMap<Integer, Integer>();

		for (Rating r : ratings) {
			Float itemAvg = result.get(r.item);
			if (itemAvg == null) {
				itemAvg = new Float(r.rating);
				result.put(r.item, (float) r.rating);
				counters.put(r.item, 1);
			} else {
				counters.put(r.item, counters.get(r.item) + 1);
				result.put(r.item, result.get(r.item) + r.rating);
			}
		}
		// Divide by number of ratings
		for (Integer item : result.keySet()) {
			result.put(item, result.get(item) / (float) counters.get(item));
		}
		return result;
	}

	// =====================================================================================

	/**
	 * Calculates the past like statements of a user. Liked items are those
	 * which were rated above (or exactly as) the user's average
	 * 
	 * @return
	 */
	public static Map<Integer, Set<Integer>> getPastLikedItemsOfUsers(
			DataModel dm) {
		Map<Integer, Set<Integer>> result = new HashMap<Integer, Set<Integer>>();

		// Get the user averages first
		Map<Integer, Float> userAverages = dm.getUserAverageRatings();
		// go through the ratings and store things in the map
		for (Rating r : dm.getRatings()) {
			if (r.rating >= userAverages.get(r.user)) {
				Set<Integer> likedItems = result.get(r.user);
				if (likedItems == null) {
					likedItems = new HashSet<Integer>();
					result.put(r.user, likedItems);
				}
				likedItems.add(r.item);
			}
		}
		return result;
	}

	// =====================================================================================

	/**
	 * A method that increments the counter value in a map. If no value exists,
	 * it adds 1. Otherwise we increment the value
	 * 
	 * @param map
	 * @param key
	 */
	public static <K> void incrementMapValue(Map<K, Integer> map, K key) {
		Integer existingValue = map.get(key);
		if (existingValue == null) {
			map.put(key, 1);
		} else {
			map.put(key, existingValue + 1);
		}
	}

	// =====================================================================================
	
	/**
	 * Calculates the cosine similarity between two weight vectors. 
	 * Lists are generated first and empty fields filled with zeros
	 * @param map1
	 * @param map2
	 * @param wordlist used for better debugging
	 * @return the similarity or double.NaN if the value could not be calculated (because of missing content data)
	 */
	@SuppressWarnings("unused")
	public static double cosineSimilarity(Map<Integer, Double> map1, Map<Integer, Double> map2, List<String> wordlist) {
//		System.out.println("map1: " + map1 + " \nmap 2: " + map2);
		if (map1 == null || map2 == null) {
			return Double.NaN;
		}
		
		// create the lists
		List<Double> L1 = new ArrayList<Double>();
		List<Double> L2 = new ArrayList<Double>();
		// initialize the lists
		for (String word : wordlist)  {
			L1.add(0.0);
			L2.add(0.0);
		}
		// copy list one
		for (Integer pos : map1.keySet()) {
			L1.set(pos, map1.get(pos));
		}
		// copy list two
		for (Integer pos : map2.keySet()) {
		
			L2.set(pos, map2.get(pos));
		}
		Double[] arr1 = L1.toArray(new Double[L1.size()]);
		Double[] arr2 = L2.toArray(new Double[L2.size()]);
		double result = dot( arr1, arr2 ) / ( Math.sqrt( dot( arr1, arr1 ) ) * Math.sqrt( dot( arr2, arr2 ) ) );
		
		
		return result;
	}

	// =====================================================================================

	/**
	 * Returns the dot product of two arrays
	 * @param arr1
	 * @param arr2
	 * @return the dot product
	 */
	public static double dot(Double[] arr1, Double[] arr2) {
		double result = 0.0;
    for ( int j = 0, n = arr1.length; j < n; j++ ) {
      result += arr1[j] * arr2[j];
    }
    return result;
	}
	
	
	// =====================================================================================

	/**
	 * Loads the word list from the file system into memory
	 * @return a list of words
	 * @throws Exception
	 */
	public static List<String> loadwordlist(String filename) throws Exception {
		ArrayList<String> result = new ArrayList<String>();
		BufferedReader reader = new BufferedReader(new FileReader(filename));
		String line = reader.readLine();
		while (line != null) {
			result.add(line);
			line = reader.readLine();
		}
		reader.close();
		return result;
	}
	
	// =====================================================================================
	
	/**
	 * {@link #createCosineSimilaritiesFile(String, String, Map, List, int)}
	 * Uses the default file name 'cos-sim-vectors.txt'.
	 * @param dataDirectory
	 * @param featureWeights
	 * @param wordlist
	 * @param nbOfThreads
	 * @throws Exception
	 */
	public static void createCosineSimilaritiesFile(String dataDirectory, Map<Integer, Map<Integer, Double>> featureWeights, List<String> wordlist, int nbOfThreads) throws Exception {
		createCosineSimilaritiesFile(dataDirectory, "cos-sim-vectors.txt", featureWeights, wordlist, nbOfThreads);
	}
	
	/**
	 * Helper method that calculates cosine similarities for item pairs in an TF-IDF vector file and stores them in a separate file
	 * As a convention, in this file each item ID has a list of items with greater ID that holds the corresponding cosine similarity for the item pair 
	 * @param dataDirectory directory in which all files for this calculation are located
	 * @param featureWeights a nestled map of feature weights
	 * @param wordlist a list of terms for the feature weight content
	 * @param nbOfThreads how many thread shall be used for the creation of the file
	 */
	public static void createCosineSimilaritiesFile(String dataDirectory, String fileName, Map<Integer, Map<Integer, Double>> featureWeights, List<String> wordlist, int nbOfThreads) throws Exception {
	
		
		//keep track of appropriate line number
		int lineNo = 0;
		
		//Get ExecutorService from Executors utility class, thread pool size is n
        ExecutorService executor = Executors.newFixedThreadPool(nbOfThreads);
        //create a list to hold the Future object associated with Callable
        List<Future<SimpleEntry<Integer,String>>> futures = new ArrayList<Future<SimpleEntry<Integer,String>>>();
		
		// compare each entry with all other entries
		for (Map.Entry<Integer, Map<Integer, Double>> entry1 : featureWeights.entrySet()) {
			
			//add the assignment to the thread pool
			Callable<SimpleEntry<Integer,String>> callable = new CosineCallable(lineNo, entry1, featureWeights, wordlist);
			futures.add(executor.submit(callable));
			
			lineNo++;
		}
		
		//keep track of count
		long count = 0;
		long lastPercent = 0;
			
		//Retrieve the results
		Map<Integer, String> lines = new HashMap<Integer, String>();
		for(Future<SimpleEntry<Integer,String>> f : futures){
			//The next line waits, so we will only be done with this for loop when all results are in
			SimpleEntry<Integer,String> result = f.get();
			
			//Log the progress
			count++;
			long percent; //our granularity is xxx.xx%
			if((percent=count*10000/featureWeights.size())!=lastPercent){
				lastPercent = percent;
			
			}
			
			lines.put(result.getKey(), result.getValue());
		}
		
		//shut down the executor service now
        executor.shutdown();
        
    
        
        // prepare file for output; will be overwritten if it already exists
     	PrintWriter pWriter = new PrintWriter(new FileWriter(dataDirectory + "/" + fileName));
     	
     	//write the lines
     	Map<Integer, String> sortedLines = sortByKeyAscending(lines);
        for (String line : sortedLines.values()) {
        	pWriter.println(line);
		}
        
		// close output file
		pWriter.close();
		
		

	}
	
	/**
	 * Encapsulates the cosine creation process to make it multi threaded
	 * @author Jugovac
	 *
	 */
	public static class CosineCallable implements Callable<SimpleEntry<Integer, String>>{

		private int lineNumber;
		private Entry<Integer, Map<Integer, Double>> primaryElement;
		private Map<Integer, Map<Integer, Double>> otherFeatureWeights;
		private List<String> wordlist;

		public CosineCallable(int lineNumber, Map.Entry<Integer, Map<Integer, Double>> primaryElement, 
				Map<Integer, Map<Integer, Double>> otherFeatureWeights, List<String> wordlist){
					this.lineNumber = lineNumber;
					this.primaryElement = primaryElement;
					this.otherFeatureWeights = otherFeatureWeights;
					this.wordlist = wordlist;
			
		}
		
		/**
		 * Does the actual work of creating exactly one line for the cos-sim file
		 */
		@Override
		public SimpleEntry<Integer, String> call() throws Exception {
			StringBuilder b = new StringBuilder();
			// write first (smaller) key for this line
			b.append(primaryElement.getKey() + ";");
				
			for (Map.Entry<Integer, Map<Integer, Double>> entry2 : otherFeatureWeights.entrySet()) {
					
				// make sure we don't compare an entry with itself and that we only compare entries once, and that the second entry is always greater
				if (primaryElement.getKey() < entry2.getKey()) {
						

					// calculate cosine similarity for the values of the key pair
					double similarity = Utilities101.cosineSimilarity(primaryElement.getValue(), entry2.getValue(), wordlist);
					
					// double-check if calculation was successful
					if (!Double.isNaN(similarity)) {
	
						// write the second key and the similarity for the key pair
						b.append(" " + entry2.getKey() + ":" + similarity);
					}
				}
			}
			
			//return the line number and the corresponding line for the cosine file
			return new SimpleEntry<>(lineNumber, b.toString());
		}
		
	}
	
	// =====================================================================================
	
	/**
	 * Helper method that loads pair cosine similarities from file into memory
	 * @param filename
	 * @return a nestled map of cosine similarities associated with pairs of items; the item ID of outer map's key is always smaller than the inner map's key
	 */
	public static Map<Integer, Map<Integer, Double>> loadCosineSimilarities(String filename) {
		
		Map<Integer, Map<Integer, Double>> result = new HashMap<Integer, Map<Integer, Double>>();
		Map<Integer, Double> newEntry = new HashMap<Integer, Double>();
		Integer keyItem;
		String[] tokens;
		String[] entry;
		
		
		try {
			BufferedReader reader;
			ZipFile zipFile = null;
			// initialize buffered reader
			if(filename.endsWith(".zip")){
				//Debug.log("Assuming file is zipped. Trying to access zipped content.");
				zipFile = new ZipFile(filename);

				InputStream inputStream = zipFile.getInputStream(zipFile.entries().nextElement());

				reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
			}else{
				reader = new BufferedReader(new FileReader(filename));
			}
			
			
			String line = reader.readLine();
			
			// read and load file content
			while (line != null) {
				
				// extract data from the line
				// expects file line structure: itemA; itemB:cosineSimilarity with itemA < itemB
				keyItem = Integer.parseInt(line.substring(0, line.indexOf(';')));
				line = line.substring(line.indexOf(';') + 1,line.length());
				
				// create a new entry in the result map
				newEntry = new HashMap<Integer, Double>();
				tokens = line.split(" ");
				for (String token : tokens) {
					if (token.length() > 0) {
						entry = token.split(":");
						newEntry.put(Integer.parseInt(entry[0]), Double.parseDouble(entry[1]));
					}
				}
				result.put(keyItem, newEntry);
				
				// get next line
				line = reader.readLine();
			}
			
			// close reader and return result
			reader.close();
			if(zipFile!=null){
				zipFile.close();
			}
			return result;
			
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	/**
	 * Loads the feature weights into memory. Features weights are stored in a map from item-IDs to feature ID to weight
	 * Could be made more memory efficient
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unused")
	public static Map<Integer, Map<Integer, Double>> loadFeatureWeights(String filename) throws Exception {
		HashMap<Integer, Map<Integer, Double>> result = new HashMap<Integer, Map<Integer, Double>>();
		BufferedReader reader = new BufferedReader(new FileReader(filename));
		String line = reader.readLine();
		String[] tokens;
		String[] entry;
		String itemid;
		HashMap<Integer, Double> newEntry;
		int idx;
		int cnt = 0;
		while (line != null) {
			cnt++;
			// Expected File structure
			// item-id;feature-id:weight feature-id weight
			// get the filename
			idx = line.indexOf(';');
			itemid = line.substring(0,idx);
			line = line.substring(idx+1,line.length());
//			idx = filename.lastIndexOf('/');
//			filename = filename.substring(idx+1,filename.length());
			newEntry = new HashMap<Integer, Double>();
			tokens = line.split(" ");
			for (String token: tokens) {
				if (token.length() > 0) {
					entry = token.split(":");
//					System.out.println("Entry: " + entry[0] + " " + entry[1]);
					newEntry.put(Integer.parseInt(entry[0]), Double.parseDouble(entry[1]));
				}
			}
			// remove the prefix first.

			result.put(Integer.parseInt(itemid),newEntry);
			line = reader.readLine();
		
		}
		reader.close();
		return result;
	}
	

	
		
}
