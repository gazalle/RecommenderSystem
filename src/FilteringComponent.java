import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;



import dataModel.DataModel;
import tools.Utilities101;
import dataModel.Rating;
import tools.Instantiable;

public class FilteringComponent  extends Instantiable{
	protected ProfileLearner pf;
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
	
	public void start() throws Exception {

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
		 
		// calculate user profiles from transactions
		userProfiles = pf.loadUserProfiles();
		
		// Remember the user averages as default for the prediction
		userAverages = dataModel.getUserAverageRatings();
		
		// Remember the item averages
		itemAverages = Utilities101.getItemAverageRatings(dataModel.getRatings());
		
		

	}
	
	
	// =====================================================================================

	public float predictRating(int user, int item) {
		// The default
		Float userAvg = this.userAverages.get(user);
		
		// We can do nothing about this user
		// The item default might be a fallback
		if (userAvg == null) {
			Float itemAVG = this.itemAverages.get(item);
			if (itemAVG != null) {
				System.out.println("Returning item average");
				return itemAVG;
			}
			else {
				System.out.println("No user and item average available");
				return Float.NaN;
			}
		}
		
		
		// The algorithm searches for the n most similar items of the given item and combines the ratings
		// the prediction is a weighted combination of the neighbor ratings.
		// Get the feature vector of the target item
		//Map<Integer, Double> targetItemVector = this.featureWeights.get(item);
		//Map<Integer, Double> otherItemVector;
		// go through all the items for which we have ratings
		Set<Rating> ratedItems = dataModel.getRatingsPerUser().get(user);
		
		// Default, if we have no data
		if (ratedItems.size() == 0) {
			return userAvg;
		}
		
		// Here's where we will store the similarities
		Map<Integer, Double> similarities = new HashMap<Integer, Double>();
		double similarity = 0;
		int keySmaller;
		int keyGreater;
		for (Rating r : ratedItems) {
			// Get the feature vector
			//otherItemVector = this.featureWeights.get(r.item);
			//similarity = Utilities101.cosineSimilarity(targetItemVector, otherItemVector, this.wordlist);
			
			// get the cosine similarity of the current pair if possible
			keySmaller = Math.min(item, r.item);
			keyGreater = Math.max(item, r.item);
			if ((cosineSimilarities.get(keySmaller) != null) && (cosineSimilarities.get(keySmaller).get(keyGreater) != null)) {
				similarity = cosineSimilarities.get(keySmaller).get(keyGreater);
			} else {
				similarity = Double.NaN;
			}
			
			if (Double.isNaN(similarity)) {
				similarity = 0.0;
			}
			similarities.put(r.item, similarity);
		}
		
		similarities = Utilities101.sortByValueDescending(similarities);
//		System.out.println("Similarities: " + similarities);
		
		double existingRatingsSum = 0;
		int counter = 0;
		double weightSum = 0;
		double similarityWeight = 0;
		double ratingSum = 0;
		for (Integer otherItem : similarities.keySet()) {
//			System.out.println("Using for prediction " + otherItem + ", user rating was: " + dataModel.getRating(user, otherItem));
			
			// remember the weight
			similarityWeight = similarities.get(otherItem);
			if (similarityWeight > this.simThresholdForPrediction) {
				weightSum += similarityWeight;
				// add up the differences of the user average
				ratingSum = (dataModel.getRating(user, otherItem) - userAvg) * similarityWeight;
//				System.out.println("existing rating was: " + dataModel.getRating(user, otherItem));
				existingRatingsSum += ratingSum;
				counter++;
			}
			if (nbNeighborsForPrediction > 0 && counter >= nbNeighborsForPrediction)  {
				break;
			}
		}
		int nbToDivide = Math.min(counter, similarities.keySet().size());
		
		//if (nbToDivide == 0) {
//			System.out.println("No neighbors found with ratings ..");
			//if (this.fallBackRec != null) {
				//return this.fallBackRec.predictRating(user, item);
			//}
			//return Float.NaN;
		
		//}
		
//		result = (float) existingRatingsSum / nbToDivide;
		float result = (float) existingRatingsSum / (float) weightSum;
		result += userAvg;
//        System.out.println("predicting " + result + " based on " + similarities.keySet().size() + " ratings (max : " + nbNeighborsForPrediction + "), nbTodivide:[ " + nbToDivide + "]");
		return result;
	}
	
	// =====================================================================================
		/**
		 * We take the user's profile and return the non-seen items ranked according to the
		 * cosine similarity of the vectors of the profile and the item
		 */
		
		public List<Integer> recommendItems(int user) {
			List<Integer> result = new ArrayList<Integer>();
			
			// Get the profile
			Map<Integer, Double> userProfile = this.userProfiles.get(user);
			if (userProfile == null) {
				if (fallBack == null) {
					//Debug.log("ContentBasedRecommender:recommend : NO PROFILE for user " + user + " - returning empty list");
					return result;
				}
				else {
					//Debug.log("No profile - using fallback: "  + this.fallBack);
					return this.recommendItems(user);
				}
//				return result;
			}
			
			// Prepare a list for the results
			Map<Integer, Double> similarities = new HashMap<Integer, Double>();
			// go through the items
			float rating = -1;
			double similarity = 0;
			for (Integer item : dataModel.getItems()) {
				similarity = 0.0;
				// check if we have a rating for it
				rating = dataModel.getRating(user, item);
				
				if (hideKnownItems && rating != -1) {
					//if hideKnownItems is true, we only want to recommend items which the user does not know
					//thus, if hideKnownItems is and the item is known (rating != -1), we make no prediction
					continue;
				}
				
				// Get the feature vector for the item
				Map<Integer,Double> featureVector = this.featureWeights.get(item);
				// Check if we have a feature vector for it
				if (featureVector != null) {
					similarity = Utilities101.cosineSimilarity(userProfile, featureVector, this.wordlist);
					if (Double.isNaN(similarity)) {
						similarity = 0.0;
					}
					similarities.put(item, similarity);
				}
				
			}
			// Once we have the similarities, we sort them in descending order and return them
			Map<Integer, Double> sortedMap = Utilities101.sortByValueDescending(similarities);
			
			result.addAll(sortedMap.keySet());
			
			
			// DEBUG:
//			System.out.println("User liked");
//			for (Integer item : likedItemsPerUser.get(user)) {
//				System.out.println(featureVectorAsString(featureWeights.get(item), this.wordlist));
//			}
	//
//			System.out.println("User profile");
//			System.out.println(featureVectorAsString(this.userProfiles.get(user), wordlist));
//			System.out.println("Recommending: ");
//			for (int i=0;i<5;i++) {
//				System.out.println(result.get(i) + " " + similarities.get(result.get(i)));
//				System.out.println(featureVectorAsString(featureWeights.get(result.get(i)), this.wordlist));
//			}
			
			return result;
		}

		// =====================================================================================

}
