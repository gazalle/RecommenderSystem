
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;




import dataModel.DataModel;
import dataModel.DefaultDataLoader;
import tools.Utilities101;



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
