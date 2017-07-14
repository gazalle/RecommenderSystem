//import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dataModel.DataModel;
import tools.Utilities101;
//import dataModel.Rating;
//import tools.Debug;
import tools.Instantiable;


public class ProfileLearner extends Instantiable{
	
	protected DataModel dataModel;
	/**
	 * Where do we expect the files in the default case
	 */
	public static String dataDirectory = "data/movielens/";
	
	/**
	 * The file where the word list is stored 
	 */
	public String wordListFile = "wordlist.txt";
	
	/**
	 * Where we expect the feature (TF-IDF or lsa weight) files
	 */
	public String featureWeightFile = "tf-idf-vectors.txt";
	
	/**
	 * Where we expect the cosine similarities file
	 */
	public String cosineSimilaritiesFile = "cos-sim-vectors.zip";
	
	// The list of words
	List<String> wordlist;

	// The weights per word of a game: maps product IDs to a map of word-id and weights
	Map<Integer, Map<Integer, Double>> featureWeights;
	
	// The cosine similarities of each item pair
	Map<Integer, Map<Integer, Double>> cosineSimilarities;
	
	// The average profile vector per user
	Map<Integer, Map<Integer, Double>> userProfiles;

	// A map where we store what a user has liked in the past
	// Liked items are those that the user has rated above his own average in the past
	Map<Integer, Set<Integer>> likedItemsPerUser;
	
	// Remember the user averages for the prediction task
	Map<Integer, Float> userAverages;
	

	// Remember the item averages for the prediction task
	Map<Integer, Float> itemAverages;

	// How many neighbors should be used for the rating prediction
	int nbNeighborsForPrediction = 10;
	
	// Abstract Recommender as a fallback
	//AbstractRecommender fallBackRec = null;

	// The fallback string
	String fallBack = null;
	
	
	/**
	 * The min similarity for predictions
	 */
	double simThresholdForPrediction = 0.0;
	
	//default is true for legacy reasons
	private boolean hideKnownItems = true;
	
	/**
	 * Sets the data model and do all initializations here
	 * @param dm
	 */
	public void setDataModel(DataModel dm) {
		dataModel = dm;
	};
	
	/**
	 * Returns the data model
	 * @return
	 */
	public DataModel getDataModel() {
		return dataModel;
	}	
	
	@Override
	public String toString() {
		String result = this.getConfigurationFileString();
		if (result != null) {
			result = Utilities101.removePackageQualifiers(result);
			return result;
		}
		else {
			return "No algorithm configuration provided";
		}
	} 
	

	/**
	 * Determines if this instance of the recommender algorithm shall return items that the user already knows.
	 * E.g. in the movie domain, if this parameter is set to true, the recommender will not recommend movies 
	 * that the user has explicitly liked or rated in the training set. If the parameter is set to false, the recommender 
	 * can potentially recommend all items from the item space including items that the user already knows.
	 * @param value
	 */         
	public void setHideKnownItems(String value){
		hideKnownItems(Boolean.parseBoolean(value));
	}

	/**
	 * See {@link AbstractRecommender#setRecommendKnownItems(String)}. A recommender has to override this method,
	 * if the parameter is configurable in the respective algorithm. If not, and if the users tries to set the parameter,
	 * they will get an exception telling them that the algorithm cannot be configured to either recommend or not recommend
	 * known items and the they shall refrain from setting the parameter because the behavior will be indeterminate either way.
	 * @param parseBoolean
	 */
	protected void hideKnownItems(boolean value) {
		throw new UnsupportedOperationException("This algorithm does not support the 'recommend known items' parameter. "
				+ "If you want to be sure of the algorithm's behavior, you need to check its implementation");
	}
	
	// =====================================================================================
	/**
	 * We load the content-information into memory and calculate the profile vectors 
	 */
	/**
	 * We load the content-information into memory and calculate the profile vectors 
	 */
	
	public void createUserProfile() throws Exception {

		// load the word list
		wordlist = Utilities101.loadwordlist(dataDirectory + "/" + wordListFile);
		// load feature vectors
		featureWeights = Utilities101.loadFeatureWeights(dataDirectory + "/" + featureWeightFile);
		// Get the liked items of the past of the user
		likedItemsPerUser = Utilities101.getPastLikedItemsOfUsers(dataModel);
		
		// load cosine similarities once
		//if (cosineSimilarities == null) {
			
			// if the cosine similarities file does not exist, create it
			//File filecheck = new File(dataDirectory + "/" + cosineSimilaritiesFile);
			//if(!filecheck.exists()){
				Utilities101.createCosineSimilaritiesFile(dataDirectory, featureWeights, wordlist, 1);
			//}
			
			cosineSimilarities = Utilities101.loadCosineSimilarities(dataDirectory + "/" + cosineSimilaritiesFile);
		//}
		 
		
		
		// Remember the user averages as default for the prediction
		userAverages = dataModel.getUserAverageRatings();
		
		// Remember the item averages
		itemAverages = Utilities101.getItemAverageRatings(dataModel.getRatings());

	}
	
	
	// =====================================================================================			
			
/**
 * Calculate the profile vectors for each user based on his past liked items
 * The calculation is based on the average profile vector
 * @return
 */
@SuppressWarnings("unused")
Map<Integer, Map<Integer, Double>> loadUserProfiles() {
	Map<Integer,Map<Integer, Double>> result = new HashMap<Integer, Map<Integer, Double>>();
	int cnt = 1;
	for (Integer user : dataModel.getUsers()) {
		// Get the positively rated items of the user
		Set<Integer> likedItems = likedItemsPerUser.get(user);
		// If we have no likedItems, we have to go to the next. 
		if (likedItems == null) {
			// cannot build a profile
			continue;
		}
		
//		System.out.println("Liked by user: " + user + " " + likedItems);

		List<Map<Integer, Double>> vectorsOfLikedItems = new ArrayList<Map<Integer, Double>>(); 
		for (Integer item : likedItems) {
//			System.out.println("User " + user.getID() + " liked: " + aritem);
			// Get the features weights
			Map<Integer, Double> weightvector = featureWeights.get(item);
			if (weightvector != null) {
				vectorsOfLikedItems.add(weightvector);
			}
			else {
				// Do nothing. There are not vectors for every product available
//			System.out.println("---> Missing vector for " + itemID);
			}
		}
		// Ok, we have the vectors. get the word list, sum up everything and divide by number of liked items
		if (vectorsOfLikedItems.size() > 0) {
			// create the profile vector entries from the wordlist. We need all the values here.
			Map<Integer, Double> profileVectorMap = new HashMap<Integer, Double>();
			// go through the word list and initialize the weights with zero
//			int i = 0;
//			for (String word : this.wordlist) {
//				profileVectorMap.put(i, 0.0);
//				i++;
//			}
			// go through all the item vectors
			for (Map<Integer, Double> weightvector : vectorsOfLikedItems) {
				// and copy things to the global vector. at the end, we will divide things by the number of items
				for (Integer wordidx : weightvector.keySet()) {
					Double oldvalue = profileVectorMap.get(wordidx);
					if (oldvalue == null) {
						profileVectorMap.put(wordidx, 0.0);
					}
					profileVectorMap.put(wordidx, profileVectorMap.get(wordidx) + weightvector.get(wordidx));
				}
			}
			// now we divide things..
			
			for (Integer wordidx : profileVectorMap.keySet()) {
				// get the value and divide it by the number of liked items
				profileVectorMap.put(wordidx, profileVectorMap.get(wordidx) / (double) likedItems.size());
			}
			result.put(user,profileVectorMap);
		}
		else {
			// set null (i.e. do nothing) 
		}
		
		// debug
		cnt++;
//		if (cnt >= 20) { break; }
		
	}
	return result;
}
}
	

