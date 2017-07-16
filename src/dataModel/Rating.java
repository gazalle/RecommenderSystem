package dataModel;

import java.io.Serializable;

/**
 * A class to store ratings
 *
 */
@SuppressWarnings("serial")
public class Rating implements Serializable {
	public int user;
	public int item;
	public float rating;

	/**
	 * A simple constructor
	 * @param u
	 * @param i
	 * @param r
	 */
	public Rating(int u, int i, float r) {
		user = u; item = i; rating = r;
	}
	
	
	/** String representation of rating */
	public String toString() {
		return ""+this.item+ "";  
	}
	
	/**
	 * Two ratings are equal if they have the same user and item id.
	 * (Rating values are not important as we assume to have only one rating per user and item)
	 */
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof Rating))
            return false;
        if (other == this)
            return true;
		
		Rating otherRating = (Rating) other;
		if (this.item == otherRating.item && this.user == otherRating.user) {
			return true;
		}
		else {
			return false;
		}
	}
	
}
