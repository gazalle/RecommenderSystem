
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;



import dataModel.DataModel;
import dataModel.DefaultDataLoader;
import tools.Utilities101;
import dataModel.Rating;


public class FilteringComponent{
	
	ProfileLearner pf= new ProfileLearner();
	
	
	public static String dataDirectory = "data/shopProducts/";
	public String wordListFile = "wordlist.txt";
	public String featureWeightFile = "tf-idf-vectors.txt";
	public String cosineSimilaritiesFile = "cos-sim-vectors.txt";
	
	// The list of words
	List<String> wordlist;
    // The weights per word of a game: maps product IDs to a map of word-id and weights
	Map<Integer, Map<Integer, Double>> featureWeights;
	// The cosine similarities of each item pair
	Map<Integer, Map<Integer, Double>> cosineSimilarities;
	// The average profile vector per user
	Map<Integer, Map<Integer, Double>> userProfiles;
    // A map where we store what a user has liked in the past
	
	// Remember the user averages for the prediction task
	Map<Integer, Float> userAverages;
	// Remember the item averages for the prediction task
	Map<Integer, Float> itemAverages;

	// How many neighbors should be used for the rating prediction
	int nbNeighborsForPrediction = 3;
	double simThresholdForPrediction = 0.0;
	//default is true for legacy reasons
	 boolean hideKnownItems = true;	
	// Prepare a list for the results
	Map<Integer, Double> similarities = new HashMap<Integer, Double>();
	
				
	// =====================================================================================
	/**
	 * @throws Exception 
	 * The algorithm searches for the n most similar items of the given item and combines the ratings
	 * the prediction is a weighted combination of the neighbor ratings.
	 */
	public float predictRating(int user, int item) throws Exception {
		
		DataModel dataModel=new DataModel();
		DefaultDataLoader dl=new DefaultDataLoader();
		dl.loadData(dataModel);
		
		// Remember the item averages
		itemAverages = Utilities101.getItemAverageRatings(dataModel.getRatings());
		// Remember the user averages as default for the prediction
		userAverages = dataModel.getUserAverageRatings();
		// load the word list
		wordlist = Utilities101.loadwordlist(dataDirectory + "/" + wordListFile);
		// load feature vectors
		featureWeights = Utilities101.loadFeatureWeights(dataDirectory + "/" + featureWeightFile);
		//create cosine similarities
		Utilities101.createCosineSimilaritiesFile(dataDirectory, featureWeights, wordlist, 1);
		//load cosine similarities
		cosineSimilarities = Utilities101.loadCosineSimilarities(dataDirectory + "/" + cosineSimilaritiesFile);
		
		
		// We can do nothing about this user
		// The item default might be a fallback
		Float userAvg = this.userAverages.get(user);
	
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
		
		// go through all the items for which we have ratings
		Set<Rating> ratedItems = dataModel.getRatingsPerUser().get(user);
		// Default, if we have no data
		if (ratedItems.size() == 0) {
			return userAvg;
		}
		
		// Store the similarities
		double similarity = 0;
		int keySmaller;
		int keyGreater;
		for (Rating r : ratedItems) {
			
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
	    //System.out.println("Similarities: " + similarities);
		
		double existingRatingsSum = 0;
		int counter = 0;
		double weightSum = 0;
		double similarityWeight = 0;
		double ratingSum = 0;
		for (Integer otherItem : similarities.keySet()) {
        //System.out.println("Using for prediction " + otherItem + ", user rating was: " + dataModel.getRating(user, otherItem));
		// remember the weight
		similarityWeight = similarities.get(otherItem);
		if (similarityWeight > this.simThresholdForPrediction) {
		weightSum += similarityWeight;
		// add up the differences of the user average
		ratingSum = (dataModel.getRating(user, otherItem) - userAvg) * similarityWeight;
		//System.out.println("existing rating was: " + dataModel.getRating(user, otherItem));
		existingRatingsSum += ratingSum;
		counter++;}
		if (nbNeighborsForPrediction > 0 && counter >= nbNeighborsForPrediction)  {
			break;
		}
		}
		
		int nbToDivide = Math.min(counter, similarities.keySet().size());
		if (nbToDivide == 0) {
		System.out.println("No neighbors found with ratings ..");}
		
		float result = (float) existingRatingsSum / (float) weightSum;
		result += userAvg;
        return result;
	}
	
	// =====================================================================================
		/**
		 * Take the user's profile and return the non-seen items ranked according to the
		 * cosine similarity of the vectors of the profile and the item
		 * @throws Exception 
		 */
		
		public List<Integer> recommendItems(int user) throws Exception {
			
			DataModel dataModel=new DataModel();
			DefaultDataLoader dl=new DefaultDataLoader();
			dl.loadData(dataModel);
			
			// load the word list
			wordlist = Utilities101.loadwordlist(dataDirectory + "/" + wordListFile);
			// calculate user profiles from transactions
			userProfiles = pf.loadUserProfiles();
			// load feature vectors
			featureWeights = Utilities101.loadFeatureWeights(dataDirectory + "/" + featureWeightFile);
	
			List<Integer> result = new ArrayList<Integer>();
			
			// Get the profile
			Map<Integer, Double> userProfile = this.userProfiles.get(user);
			if (userProfile == null) {
			System.out.println("No PROFILE for user " + user + " - returning empty list");
			return result; }
			
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
					continue;}
				// Get the feature vector for the item
				Map<Integer,Double> featureVector = this.featureWeights.get(item);
		
				// Check if we have a feature vector for it
				if (featureVector != null) {
					
					similarity = Utilities101.cosineSimilarity(userProfile, featureVector, this.wordlist);
					if (Double.isNaN(similarity)) {
						similarity = 0.0;}
					similarities.put(item, similarity);} }
			
			// Once we have the similarities, we sort them in descending order and return them
			Map<Integer, Double> sortedMap = Utilities101.sortByValueDescending(similarities);
		    result.addAll(sortedMap.keySet());
			return result;
		}

}
