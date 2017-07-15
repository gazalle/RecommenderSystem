


public class Recommend {

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
	 
		FilteringComponent fc = new FilteringComponent();
		//ProfileLearner pf = new ProfileLearner();
		//DefaultDataLoader dl=new DefaultDataLoader();
		//dl.loadData(dm);
		//System.out.println(dm.toString());
		//System.out.println(dm.getRating(5, 1));
		//System.out.println(dm.getUsers());
		//System.out.println(dm.getItems());
		//System.out.println(Utilities101.getPastLikedItemsOfUsers(dm));
		
		//System.out.println(pf.loadUserProfiles());
		//fc.start();
		
		System.out.println(fc.recommendItems(5));
		System.out.println(fc.recommendItems(5));
		System.out.println(fc.predictRating(5,5));
	}

}
