
package dataModel;

import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import dataModel.Rating;
import tools.Utilities101;

/**
 * This class holds all the data required for the recommendation process.
 *
 */
@SuppressWarnings("serial")
public class DataModel implements Serializable {
	
	// The list of users
	Set<Integer> users = new IntOpenHashSet();
	
	// The item list
	Set<Integer> items = new IntOpenHashSet();
	
	// All the ratings / use sparse matrix later on 
	protected Set<Rating> ratings = new ObjectOpenHashSet<Rating>();
	
	// HashMap ratings per user
	Map<Integer, Set<Rating>> ratingsPerUser = new Int2ObjectOpenHashMap<Set<Rating>>();
	
	// boolean indicating if averages are "dirty"
	boolean averagesDirty = false;

	/**
	 * A map that contains the user averages. We initialize it on first use and 
	 * a call to getUserAverageRating() or to getUserAverageRatings();
	 */
	public Map<Integer, Float> userAverageRatings;
	
	// the minimum rating value
	float minRatingValue;
	
	// the maximum rating value
	float maxRatingValue;
	
	// remember the split number. Can be used to store models
	int splitNumber = -1;
	
	// =====================================================================================

	/**
	 * Stores a rating in the data model
	 * @param user the user
	 * @param item the rated item
	 * @param value the value. No decimals allowed.
	 * @return the newly added rating
	 */
	public Rating addRating(int user, int item, float value) {
		Rating r = new Rating(user,item,value);
		ratings.add(r);
		Set<Rating> userRatings = ratingsPerUser.get(user);
		if (userRatings == null) {
			userRatings = new ObjectOpenHashSet<Rating>();
		}
		ratingsPerUser.put(user, userRatings);
		userRatings.add(r);
		users.add(user);
		items.add(item);
		averagesDirty = true;
		return r;
		
	}
	
	// =====================================================================================

	/**
	 * Stores a rating in the data model
	 * @param user the user
	 * @param item the rated item
	 * @param value the value. No decimals allowed.
	 * @return the newly added rating
	 */
	public Rating addRating( Rating r ) {
		ratings.add(r);
		Set<Rating> userRatings = ratingsPerUser.get(r.user);
		if (userRatings == null) {
			userRatings = new ObjectOpenHashSet<Rating>();
		}
		ratingsPerUser.put(r.user, userRatings);
		userRatings.add(r);
		users.add(r.user);
		items.add(r.item);
		averagesDirty = true;
		return r;
		
	}
	
	// =====================================================================================
	/**
	 * A method that returns the user's average rating in this data model
	 * @param user the user id
	 * @return the average or -1 in case we have no ratings.
	 */
	public float getUserAverageRating(Integer user) {
		if (this.userAverageRatings == null) {
			userAverageRatings = new Int2FloatOpenHashMap();
			userAverageRatings = Utilities101.getUserAverageRatings(this.ratings);
		}
		Float avg = userAverageRatings.get(user);
		if (avg != null) {
			return avg;
		}
		return -1;
	}
	
	// Get the ratings per user from the outside.
	public Map<Integer, Set<Rating>> getRatingsPerUser() {
		return ratingsPerUser;
	}
	
	/**
	 * Returns the set of ratins of a given user
	 * @param user the user id
	 * @return the ratings or null if the user has no ratings
	 */
	public Set<Rating> getRatingsOfUser(Integer user) {
		return this.ratingsPerUser.get(user);
	}
	
	
	// =====================================================================================


	
	/**
	 * The method returns the average ratings of a user of a map of user ids to floats. 
	 * @return the map of averages
	 */
	public Map<Integer, Float> getUserAverageRatings() {
		if (this.userAverageRatings == null) {
			userAverageRatings = new Int2FloatOpenHashMap();
			userAverageRatings = Utilities101.getUserAverageRatings(this.ratings);
		}
		// Check if someone has removed something
		if (averagesDirty) {
			recalculateUserAverages();
		}
		return  userAverageRatings;
	}


	// =====================================================================================

	/**
	 * Retrieve a rating for a given user-item pair. 
	 * @param user the user ID
	 * @param item the item ID
	 * @return the rating value or -1 in case there is no rating 
	 */
	public float getRating(int user, int item) {
		Set<Rating> userRatings = ratingsPerUser.get(user);
		float result = -1;
		if (userRatings != null) {
			// implementation to be improved
			for (Rating r : userRatings) {
				if (r.user == user && r.item == item) {
					return r.rating;
				}
			}
		}
		return result;
	}

	// =====================================================================================

	/**
	 * Returns a list of users
	 * @return
	 */
	public Set<Integer> getUsers() {
		return users;
	}

	// =====================================================================================

	/**
	 * Returns a list of items
	 * @return
	 */
	public Set<Integer> getItems() {
		return items;
	}

	// =====================================================================================

	/**
	 * Returns a handle to the list of ratings
	 * @return the ratings
	 */
	public Set<Rating> getRatings() {
		return ratings;
	}

	// =====================================================================================
	
	/**
	 * Removes a rating from the data model
	 * @param r
	 */
	public void removeRating(Rating r) {
		// Remove from my ratings
		ratings.remove(r);
		// Remove from the map
		Set<Rating> userRatings = ratingsPerUser.get(r.user);
		if (userRatings != null) {
			for (Rating rating : userRatings) {
				if (rating.item == r.item) {
					userRatings.remove(r);
					averagesDirty = true;
					return;
				}
			}
		}
	}

	// =====================================================================================

	/**
	 * Removes the user and his ratings from the data model
	 * @param user the user id
	 */
	public void removeUserWithRatings(Integer user) {
		Set<Rating> ratingsOfUser = this.ratingsPerUser.get(user);
		if (ratingsOfUser != null) {
			for (Rating r : ratingsOfUser) {
				ratings.remove(r);
			}
		}
		this.ratingsPerUser.remove(user);
		this.users.remove(user);
	}
	
	
	// =====================================================================================

	/**
	 * A method to recalculate the average ratings.
	 * This method additionally checks, if users without ratings are present.
	 */
	public void recalculateUserAverages() {
		this.userAverageRatings = Utilities101.getUserAverageRatings(this.ratings);
		averagesDirty = false;
	}
	
	// =====================================================================================
	
	/**
	 * Calculates the minimum number of ratings that any users has.
	 * @return min. number of ratings/user
	 */
	public int getMinUserRatings(){
		int min = Integer.MAX_VALUE;
		for (Map.Entry<Integer, Set<Rating>> entry : ratingsPerUser.entrySet()) {
		    Set<Rating> value = entry.getValue();
		    int size = value.size();
		    if (size < min) min = size;
		}
		return min;
	}
	
	/**
	 * Calculates the maximum number of ratings that any users has.
	 * @return max. number of ratings/user
	 */
	public int getMaxUserRatings(){
		int max = 0;
		for (Map.Entry<Integer, Set<Rating>> entry : ratingsPerUser.entrySet()) {
		    Set<Rating> value = entry.getValue();
		    int size = value.size();
		    if (size > max) max = size;
		}
		return max;
	}
	
	/**
	 * Calculates the minimum number of ratings that any item has.
	 * @return min. number of ratings/item
	 */
	public int getMinItemRatings(){
		Map<Integer, Integer> ratingsPerItem = new Int2IntOpenHashMap();
		for (Rating r : ratings){
			Utilities101.incrementMapValue(ratingsPerItem, r.item);
		}
		if(ratingsPerItem.isEmpty())return 0;
		else return Collections.min(ratingsPerItem.values());
	}
	
	/**
	 * Calculates the maximum number of ratings that any item has.
	 * @return max. number of ratings/item
	 */
	public int getMaxItemRatings(){
		Map<Integer, Integer> ratingsPerItem = new Int2IntOpenHashMap();
		for (Rating r : ratings){
			Utilities101.incrementMapValue(ratingsPerItem, r.item);
		}
		if(ratingsPerItem.isEmpty())return 0;
		return Collections.max(ratingsPerItem.values());
	}
	
	// =====================================================================================

	/**
	 * A simple string representation returning basic stats
	 */
	public String toString() {
		String result = "Datamodel:\n------------\n";
		result +="Users:  \t" + this.users.size() + "\n";
		result +="Items:  \t" + this.items.size() + "\n";
		result +="Ratings:\t" + this.ratings.size() + "\n";
				
		return result;
	}

	/**
	 * A getter for the maximum rating value
	 * @return the minimum value
	 */
	public float getMaxRatingValue() {
		return maxRatingValue;
	}

	/**
	 * A setter for the maximum rating value
	 * @param maxRatingValue
	 */
	public void setMaxRatingValue(float maxRatingValue) {
		this.maxRatingValue = maxRatingValue;
	}

	/**
	 * Getter for the minimum rating
	 * @return
	 */
	public float getMinRatingValue() {
		return minRatingValue;
	}

	/**
	 * Sets the maximum rating value
	 * @param minRatingValue
	 */
	public void setMinRatingValue(float minRatingValue) {
		this.minRatingValue = minRatingValue;
	}

	// Set the number of the split
	public int getSplitNumber() {
		return splitNumber;
	}

	// get the split number
	public void setSplitNumber(int splitNumber) {
		this.splitNumber = splitNumber;
	}	
}
