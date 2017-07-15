package dataModel;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Set;

/**
 * A default data loader capable of loading shop files
 * Format: user<tab>item<tab>order<tab>timestamp
 */
public class DefaultDataLoader  {

	// A default location
	protected String filename = "data/shopProducts/ProductOrders.txt";
	protected int minNumberOfRatingsPerUser = -1;
	protected int minNumberOfRatingsPerItem = -1;
	protected int sampleNUsers = -1;
	protected double density = 1.0;
	public static int maxLines = -1;
    // Should we transform the data 
	// 0 no
	// > 0: This is the threshold above which items are relevant 
	public int binarizeLevel = 0;
	// Should we remove 0 -valued ratings?
	public boolean useUnaryRatings = false;
	protected String separatorString = "\t";
	
	/**
	 * An empty constructor
	 */
	public DefaultDataLoader() {
	}	
	// =====================================================================================

	/**
	 * The method loads the Shop data from the specified file location.
	 * The method can be overwritten in a subclass
	 */
	public void loadData(DataModel dm) throws Exception {
		int counter = 0;
		// Read the file line by line and add the ratings to the data model.
		BufferedReader reader = new BufferedReader(new FileReader(filename));
		String line;
		line = reader.readLine();
		String[] tokens;
		while (line != null) {
			// Skip comment lines
			if (line.trim().startsWith("//")) {
				line = reader.readLine();
				continue;
			}
			tokens = line.split(separatorString);
			// First, add the ratings.
			dm.addRating(Integer.parseInt(tokens[0]), Integer.parseInt(tokens[1]), Float.parseFloat(tokens[2]));
			line = reader.readLine();
			counter++;
//			// debugging here..
			if (maxLines != -1) {
				if (counter >= maxLines) {
					System.out.println("DataLoader: Stopping after " + (counter)  + " lines for debug");
					break;
				}
			}
		}
		reader.close();
	}
	
	// =====================================================================================
	/**
	 * Binarizes the data model after loading
	 * @throws Exception
	 */
	public void binarize(DataModel dm) throws Exception {
		
		Set<Rating> ratingsCopy = new ObjectOpenHashSet<Rating>(dm.getRatings());
		
		// Go through the ratings
		for (Rating r : ratingsCopy) {
			// Option one - every rating is relevant
			
			if (r.rating >= this.binarizeLevel) {
				r.rating = 1;
			}
			else {
				// Remove rating in case we only have positive feedback
				if (this.useUnaryRatings) {
					dm.removeRating(r);
				}
				// Otherwise, set it to 0
				else {
					r.rating = 0;
				}
			}
		}
		// Recalculate things
		dm.recalculateUserAverages();
	}
	// =====================================================================================
	public void setSeparatorString(String value) {
		this.separatorString = value;
	}
	}
