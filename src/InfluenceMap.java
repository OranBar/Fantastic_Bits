import java.util.stream.*;
import java.awt.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;

public class InfluenceMap{
	
	protected float[][] influenceMap;
	protected int width, height;
	
	
	public InfluenceMap(int width, int height){
		this.width = width;
		this.height = height;
		this.influenceMap = new float[width][];
		for(int i=0; i<width; i++){
			this.influenceMap[i] = new float[height];
		}
	}
	
	public float get(int x, int y){
		return influenceMap[x][y];
	}
	
	public ArrayList<Integer[]> getInRange(int x, int y, int radius){
		ArrayList<Integer[]> inRange = new ArrayList<Integer[]>();
		
		//First all of the elements lined up to the center
		if(isOnBoard(x+radius, y)){
			inRange.add(new Integer[]{x+radius, y});
		}
		if(isOnBoard(x-radius, y)){
			inRange.add(new Integer[]{x-radius, y});			
		}
		if(isOnBoard(x, y+radius)){
			inRange.add(new Integer[]{x, y+radius});
		}
		if(isOnBoard(x, y-radius)){
			inRange.add(new Integer[]{x, y-radius});
		}
		
		//Then all of the elements that diagonally connecy the first 4 elements.
		for(int i=radius-1; i>0; i--){
			int xOff = i, yOff = radius-i;
			
			if(isOnBoard(x+xOff, y+yOff)){
				inRange.add(new Integer[]{x+xOff, y+yOff});
			}
			if(isOnBoard(x-xOff, y+yOff)){
				inRange.add(new Integer[]{x-xOff, y+yOff});			
			}
			if(isOnBoard(x+xOff, y-yOff)){
				inRange.add(new Integer[]{x+xOff, y-yOff});
			}
			if(isOnBoard(x-xOff, y-yOff)){
				inRange.add(new Integer[]{x-xOff, y-yOff});
			}
		}
	
//		System.out.println("In range of "+x+" "+y);
//		inRange.stream().forEach( n -> System.out.println(n[0]+" "+n[1]));
		return inRange;
	}
	
	private boolean isOnBoard(int x, int y){
		try{
			float temp = influenceMap[x][y];
			return true;
		} catch(ArrayIndexOutOfBoundsException e){
			return false;
		}
	}
	
	public int[][] getAllNeighbours(int x, int y){
		int noOfNeighbours = 8;
		if(x == 0 || x == width-1){
			noOfNeighbours -= 3;
		}
		if(y == 0 || y == width-1){
			if(noOfNeighbours < 8){
				noOfNeighbours -= 2;
			} else {
				noOfNeighbours -= 3;
			}
		}
		
		int currNeighbours = 0;
		int[][] neighbours = new int[noOfNeighbours][];
		for(int i=-1; i<=1; i=i+2){
			for(int j=-1; j<=1; j=j+2){
				
				int xNeighbour = x+i, yNeighbour= y+j;
    			if(xNeighbour >= 0 && xNeighbour <= width-1 
    			&& yNeighbour >= 0 && yNeighbour <= height-1){
    				neighbours[currNeighbours] = new int[]{xNeighbour, yNeighbour};
    				currNeighbours++;
    			}
			}
		}
		for(int i=-1; i<=1; i++){
			for(int j=-1; j<=1; j++){
				if(Math.abs(i)+Math.abs(j) == 1){
					int xNeighbour = x+i, yNeighbour= y+j;
					if(xNeighbour >= 0 && xNeighbour <= width-1 
							&& yNeighbour >= 0 && yNeighbour <= height-1){
						neighbours[currNeighbours] = new int[]{xNeighbour, yNeighbour};
						currNeighbours++;
					}
				}
			}
		}
		return neighbours;
	}
	
	public int[][] getNeighbours(int x, int y){
		int noOfNeighbours = 4;
		if(x == 0 || x == width-1){
			noOfNeighbours -= 1;
		}
		if(y == 0 || y == width-1){
			noOfNeighbours -= 1;
		}
		
		int currNeighbours = 0;
		int[][] neighbours = new int[noOfNeighbours][];
		
		for(int i=-1; i<=1; i++){
			for(int j=-1; j<=1; j++){
				if(Math.abs(i)+Math.abs(j) == 1){
					int xNeighbour = x+i, yNeighbour= y+j;
					if(xNeighbour >= 0 && xNeighbour <= width-1 
							&& yNeighbour >= 0 && yNeighbour <= height-1){
						neighbours[currNeighbours] = new int[]{xNeighbour, yNeighbour};
						currNeighbours++;
					}
				}
			}
		}
		
		return neighbours;
	}
	
	public void applyInfluence(int x, int y, float amount, int fullAmountDistance, int reducedAmountDistance, float distanceDecay){
//		applyInfluenceRecursive(x, y, amount, fullAmountDistance+1, reducedAmountDistance, distanceDecay, new LinkedList<Integer[]>());
		applyInfluenceIterative(x, y, amount, fullAmountDistance, reducedAmountDistance, distanceDecay);
	}
	
	private void applyInfluenceIterative(int x, int y, float amount, int fullAmountDistance, int reducedAmountDistance, float distanceDecay){
		for(int r=0; r < fullAmountDistance; r++){
			for(Integer[] tile : getInRange(x, y, r+1)){
				int tileX = tile[0], tileY = tile[1];
				influenceMap[tileX][tileY] += amount;
			}
		}
		for(int r = 0; r < reducedAmountDistance; r++){
			for(Integer[] tile : getInRange(x, y, r+1+fullAmountDistance)){
				int tileX = tile[0], tileY = tile[1];
				influenceMap[tileX][tileY] += (float) (amount * Math.pow(distanceDecay, r+1));
			}
		}
	}
	
	public void applyInfluenceRecursive(int x, int y, float amount, int fullAmountDistance, int reducedAmountDistance, float distanceDecay, LinkedList<Integer[]> visited){
		if(fullAmountDistance < 0 && reducedAmountDistance < 0) { System.err.println("Error!"); }
		
		//If i visited this node already, don't do anything. Skip.
		for(Integer[] visitedNode : visited){
			if((int)visitedNode[0] == x && (int)visitedNode[1] == y){
				return;
			}
		}
		
		if(fullAmountDistance > 0){
			influenceMap[x][y] += amount;
			visited.add(new Integer[]{x,y});
			System.out.println("influenceMap "+x+" "+y+" now "+influenceMap[x][y] );
			for(int[] neighbour : getNeighbours(x, y)){
				applyInfluenceRecursive(neighbour[0], neighbour[1], amount, fullAmountDistance-1, reducedAmountDistance, distanceDecay, visited);
			}
		} else 
		if(reducedAmountDistance > 0){
			influenceMap[x][y] += amount * distanceDecay;
			visited.add(new Integer[]{x,y});
			for(int[] neighbour : getNeighbours(x, y)){
				applyInfluenceRecursive(neighbour[0], neighbour[1], amount * distanceDecay, 0, reducedAmountDistance-1, distanceDecay, visited);
			}
		}
	}
	
	public void applyInfluence(float amount, int fullAmountDistance, int reducedAmountDistance, float distanceDecay, LinkedList<Integer[]> visited, int... points){
		if(points.length%2 == 1){
			System.err.println("invalid number of points args");
		}
		
		int noOfPoints = points.length/2;
		
		amount /= noOfPoints;
		
		for(int i=0; i<noOfPoints; i++){
			int pointX = points[i];
			int pointY = points[i+1];
			
			applyInfluence(pointX, pointY, amount, fullAmountDistance, reducedAmountDistance, distanceDecay);
		}
		
	}
}

