import java.util.ArrayList;
import java.util.Random;
import java.util.Set;

import dataModel.DataModel;
import dataModel.DataLoader;
import dataModel.Rating;

public class Recommend {

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
	 
		FilteringComponent fc = new FilteringComponent();
		
	     try {
			//System.out.println("Starting data preparation for ML data set with content information");
			ContentAnalyzer preparator = new ContentAnalyzer();
			preparator.run();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		ArrayList al = new ArrayList();
		Random random=new Random();
		int r;
		r=random.nextInt(8);
		DataModel dataModel=new DataModel();
		DataLoader dl=new DataLoader();
		dl.loadData(dataModel);
		Set<Rating> ord =dataModel.getRatingsOfUser(r) ;
		System.out.println(ord);
		//System.out.println(fc.recommendItems(r+1)); 
	
		al.addAll(fc.recommendItems(r+1));
        for(int i=0;i<5;i++){
        	int item=(int) al.get(i);
        	 if(item == 17){
        	System.out.println("item 17");
              }
        	  if(item == 20){
                	System.out.println("item 20"); 
              }
            
              if(item == 13){
            	 System.out.println("item 13");
              }
            
              if(item == 15){
            	 System.out.println("item 15");
              }
              
             if(item == 8){
            	System.out.println("item 8");
             }
        }
	}

}
