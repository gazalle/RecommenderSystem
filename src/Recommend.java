import dataModel.DataModel;

public class Recommend {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		DataModel u1= new DataModel();
		FilteringComponent fc = new FilteringComponent();
		fc.setDataModel(u1);
		System.out.println(u1.addRating(1, 1, (float) 2.1));
		System.out.println(u1.addRating(4, 2, (float) 2.1));
		System.out.println(u1.addRating(1, 3, (float) 2.0));
		//System.out.println(u1.getUserAverageRatings());
		System.out.println(fc.predictRating(1, 2));
		
		System.out.println(u1.toString());
	}

}
