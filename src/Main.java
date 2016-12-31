import java.awt.Dimension;

import javax.swing.*;

public class Main {

	
	public static void main(String[] args) {
		JFrame frame = new JFrame("Influence Map Visualizer");
		InfluenceMap myMap = new InfluenceMap(20,20);
		JPanel panel = new GridMapPanel(20,20,30,30, myMap);
		frame.add(panel);
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(new Dimension(400,300));
		
		/*
		//myMap.getInRange(2, 2, 1);
		//System.out.println("-------------");
		myMap.getInRange(2, 2, 2);
		System.out.println("-------------");
		myMap.getInRange(4, 4, 3);
		System.out.println("-------------");
		myMap.getInRange(4, 4, 4);
		*/
	}
}
