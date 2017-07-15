
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dataModel.DataModel;
import dataModel.DefaultDataLoader;
import tools.Utilities101;


public class ProfileLearner{
	
	
	
	public static String dataDirectory = "data/shopProducts/";
	public String featureWeightFile = "tf-idf-vectors.txt";
	public String wordListFile = "wordlist.txt";

	// A map where stores what a user has liked in the past
	// Liked items are those that the user has rated above his own average in the past
	Map<Integer, Set<Integer>> likedItemsPerUser;
	// The weights per word of a game: maps product IDs to a map of word-id and weights
		Map<Integer, Map<Integer, Double>> featureWeights;
		// The list of words
		List<String> wordlist;
		// create the profile vector entries from the wordlist. We need all the values here.
		Map<Integer, Double> profileVectorMap = new HashMap<Integer, Double>();
	
		List<Map<Integer, Double>> vectorsOfLikedItems = new ArrayList<Map<Integer, Double>>(); 
		Map<Integer, Double> weightvector=new HashMap<Integer, Double>();
	
	// =====================================================================================			
			
/**
 * Calculate the profile vectors for each user based on his past liked items
 * The calculation is based on the average profile vector
 * @return
 * @throws Exception 
 */
@SuppressWarnings("unused")
Map<Integer, Map<Integer, Double>> loadUserProfiles() throws Exception {
	
	DataModel dm=new DataModel();
	DefaultDataLoader dl=new DefaultDataLoader();
	dl.loadData(dm);
	
	Map<Integer,Map<Integer, Double>> result = new HashMap<Integer, Map<Integer, Double>>();
	
	// Get the liked items of the past of the user
	likedItemsPerUser = Utilities101.getPastLikedItemsOfUsers(dm);
	// load the word list
	wordlist = Utilities101.loadwordlist(dataDirectory + "/" + wordListFile);
	// load feature vectors
	featureWeights = Utilities101.loadFeatureWeights(dataDirectory + "/" + featureWeightFile);
	
	int cnt = 1;
	for (Integer user : dm.getUsers()) {
		// Get the positively rated items of the user
		Set<Integer> likedItems = likedItemsPerUser.get(user);
		if (likedItems == null) {
			// If we have no likedItems, we have to go to the next. 
			continue;}
		for (Integer item : likedItems) {
			// Get the features weights
			weightvector = featureWeights.get(item);
			vectorsOfLikedItems.add(weightvector);
			if (weightvector != null) {
				vectorsOfLikedItems.add(weightvector);}}
		
		// get the word list, sum up everything and divide by number of liked items
		if (vectorsOfLikedItems.size() > 0) {
			// go through all the item vectors
			for (Map<Integer, Double> weightvector : vectorsOfLikedItems) {
				// and copy things to the global vector
				for (Integer wordidx : weightvector.keySet()) {
					Double oldvalue = profileVectorMap.get(wordidx);
					if (oldvalue == null) {
						profileVectorMap.put(wordidx, 0.0);
					}
					profileVectorMap.put(wordidx, profileVectorMap.get(wordidx) + weightvector.get(wordidx));}}

			for (Integer wordidx : profileVectorMap.keySet()) {
				// get the value and divide it by the number of liked items
				profileVectorMap.put(wordidx, profileVectorMap.get(wordidx) / (double) likedItems.size());
			}
			result.put(user,profileVectorMap);}//if
		cnt++;
}//for 1
	return result;
}//end of method
}//end of class
	

