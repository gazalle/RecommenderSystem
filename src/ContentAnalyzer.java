import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.Set;



import edu.udo.cs.wvtool.config.WVTConfiguration;
import edu.udo.cs.wvtool.config.WVTConfigurationFact;
import edu.udo.cs.wvtool.generic.output.WordVectorWriter;
import edu.udo.cs.wvtool.generic.stemmer.PorterStemmerWrapper;
import edu.udo.cs.wvtool.generic.vectorcreation.TFIDF;
import edu.udo.cs.wvtool.main.WVTDocumentInfo;
import edu.udo.cs.wvtool.main.WVTFileInputList;
import edu.udo.cs.wvtool.main.WVTool;
import edu.udo.cs.wvtool.wordlist.WVTWordList;

public class ContentAnalyzer {
	
	public static String TARGET_DIRECTORY = "data/shopProducts/";
	public static String ORDER_FILE = "orders.dat";
	public static String USER_INFO = "ProductOrders.txt";
	public static String PRODUCT_INFO = "ProductDescription.txt";
	public static String TMP_DIRECTORY = TARGET_DIRECTORY + "tmp";
	public static String TF_IDF_FILENAME = "tf-idf-vectors.txt";
	public static String WORDLIST_FILENAME = "wordlist.txt";

	public void run() throws Exception {	
		// get the correct product ids
		Set<Integer> relevantProductIDs = getIDsOfProductsWithContentInfo(TARGET_DIRECTORY + PRODUCT_INFO);
		// create the ratings file from the given one and remove products without content info
		removeRatingsOfProductsWithoutContentInfo(TARGET_DIRECTORY, ORDER_FILE, relevantProductIDs, USER_INFO);
		//System.out.println("Extracted relevant ratings from data file to " + USER_INFO);
		// create the tf-idf vectors
		//System.out.println("Creating TF-IDF vectors from content information, creating temporary files");
		Set<String> filenames = runFileSplitter(TARGET_DIRECTORY, PRODUCT_INFO, TMP_DIRECTORY);
		//System.out.println("Creating output files containing tfidf vectors and wordlists");
		generateWordVectorsAndWordList(filenames,TMP_DIRECTORY,WORDLIST_FILENAME,TF_IDF_FILENAME, TARGET_DIRECTORY, 2,100,"english");
		//System.out.println("Created tf-idf vectors");
	}
	/**
	 * A method that creates a new ratings file and removes all lines that contain irrelevant product ids
	 */
	void removeRatingsOfProductsWithoutContentInfo(String targetDirectory, String targetFile, Set<Integer> relevantProductIDs, String resultFile) 
			throws Exception {
		//System.out.println("Extracting ratings with content information");
		BufferedReader reader = new BufferedReader(new FileReader(targetDirectory + targetFile));
		BufferedWriter writer = new BufferedWriter(new FileWriter(targetDirectory + resultFile));
		String line;
		String[] tokens;
		line = reader.readLine();
		int cnt = 0;
		int productId;
		double rating = 0;
		int newRating = 0;
		while (line != null) {
			tokens = line.split("::");
			productId = Integer.parseInt(tokens[1]);
			if (relevantProductIDs.contains(productId)) {
				rating = Double.parseDouble(tokens[2]);
				// rounding..
				newRating = (int) Math.ceil(rating);
				writer.write(tokens[0] + "\t" + tokens[1] + "\t" + newRating + "\t" + tokens[3]);
				writer.write("\n");
				cnt++;
			}
			line = reader.readLine();
		}
		//System.out.println("Wrote " + cnt + " ratings to target file " + resultFile);
		reader.close();
		writer.close();
	}

	
	/**
	 * A method that extracts the IDs of products for which we have content information
	 * @param filename
	 * @return
	 */
	public Set<Integer> getIDsOfProductsWithContentInfo(String filename) throws Exception {
		Set<Integer> result = new HashSet<Integer>();
		BufferedReader reader = new BufferedReader(new FileReader(filename));
		String line = reader.readLine();
		String[] tokens;
		int id;
		while (line != null) {
			tokens = line.split("#");
			id = Integer.parseInt(tokens[0]);
			result.add(id);
			line = reader.readLine();
		}
		reader.close();
		//System.out.println("Extracted " + result.size() + " relevant items from content file");
		return result;
	}
	
	
	// Reads the file and generates one input file per entry in our directory. Use product-IDs as file names
	/**
	 * Splits the given content file into individual files based on the product id for a later use
	 * for the word vector tool
	 * @param contentDirectory the directory of the source file
	 * @param contentFileName the content file
	 * @param outputDirectory the output directory for the temporary files
	 * @return a set of filenames
	 * @throws Exception
	 */
	public static Set<String> runFileSplitter(String contentDirectory, String contentFileName, String outputDirectory) throws Exception{
		
		//System.out.println("OUTPUTDIR = " + outputDirectory);
		// We know where the file is: 
		String inputFile = contentDirectory + contentFileName;
		//System.out.println("Splitting the file: " + inputFile);

		BufferedReader reader = new BufferedReader(new FileReader(inputFile));
		
		// We will return the file names at the end.
		Set<String> filenames = new HashSet<String>();
		
		// Read file line by line and create one output file
		String line = reader.readLine();
		String filename;
		int idx = -1;
		String itemID;
		BufferedWriter writer;
		while (line != null && line.length() > 0) {
			idx = line.indexOf("#");
			itemID = line.substring(0,idx);
//			System.out.println("ID: " + itemID);
			// Remove stuff
			line = line.substring(idx+1);
			// Remove the actors
			idx = line.lastIndexOf("#");
			line = line.substring(0,idx);
			
			line = line.replaceAll("#", " ");
//			System.out.println("Line: " + line);
			
			filename = itemID;
			filenames.add(filename);
			writer = new BufferedWriter(new FileWriter(outputDirectory + "/" + filename));
			writer.write(line);
			writer.close();
			
			line = reader.readLine();
		}
		reader.close();
		return filenames;
	}
	
	/**
	 * The method opens all the files, creates the word vectors and stores the data in the
	 * files called 
	 * @param filenames
	 */
	/**
	 * The method opens a set of files and creates the tf-idf vectors and wordlist-files
	 * @param filenames names of the files to analyze
	 * @param contentDirectory the directory where to look at
	 * @param wordListFileName the name of the file containing the wordlists 
	 * @param tfidfFileName the name of the file containing the content inf
	 * @param outputDirectory the output directory
	 * @throws Exception
	 */
	public static void generateWordVectorsAndWordList(Set<String> filenames, 
														String contentDirectory, 
														String wordListFileName, 
														String tfidfFileName, 
														String outputDirectory,
														int minFrequency,
														int maxFrequency,
														String language
														) throws Exception {

			// Create the word vector tool
			WVTool wvt = new WVTool(true);
			WVTFileInputList list = new WVTFileInputList(0);
			
			// Add all the files
			int cnt = 0;
			for (String filename : filenames) {
				list.addEntry(new WVTDocumentInfo(contentDirectory + "/" + filename, "txt","",language));
				cnt++;
			}
			
			//System.out.println("Processed " + cnt + " files");
			
			
			// Stemming
			WVTConfiguration config = new WVTConfiguration();
			config.setConfigurationRule(WVTConfiguration.STEP_STEMMER, new WVTConfigurationFact(new PorterStemmerWrapper()));
				
			
			
			// create the word list
			WVTWordList wordList = wvt.createWordList(list, config);

			// pruning seems to be necessary? 
			wordList.pruneByFrequency(minFrequency, maxFrequency);
			
			// Store the results somewhere
			wordList.storePlain(new FileWriter(outputDirectory + "/" + wordListFileName));
			
			// Also the outputs
			String tempFile = contentDirectory + tfidfFileName + ".temp";
			//System.out.println("Trying to write to " + tempFile);
			
			FileWriter fileWriter = new FileWriter(tempFile);
			WordVectorWriter wvw = new WordVectorWriter(fileWriter, true);
			config.setConfigurationRule( WVTConfiguration.STEP_OUTPUT, new WVTConfigurationFact(wvw));
			config.setConfigurationRule(WVTConfiguration.STEP_VECTOR_CREATION, new WVTConfigurationFact(new TFIDF()));

			// Create everything and go
			wvt.createVectors(list, config, wordList);
			
			wvw.close();
			fileWriter.close();

			// Now delete all the files we have created
			File fileToDelete;
			for (String filename : filenames) {
				fileToDelete = new File(filename);
				fileToDelete.delete();
			}
			
			// transform the outputfile and only keep the id instead of the filename
			BufferedReader reader = new BufferedReader(new FileReader(tempFile));
			BufferedWriter writer = new BufferedWriter(new FileWriter(outputDirectory + tfidfFileName));
			String line = reader.readLine();
			int idx1 = -1;
			int idx2 = -1;
			String fname;
			String id;
			while (line != null) {
				idx1 = line.indexOf(";");
				fname = line.substring(0,idx1);
				// find the 
				idx2 = line.lastIndexOf("/");
				id = fname.substring(idx2+1);
				writer.write(id + ";" + line.substring(idx1+1) + "\n");
				line = reader.readLine();
			}
			reader.close();
			writer.close();
			fileToDelete = new File(tempFile);
			fileToDelete.delete();

	} 
	

}


