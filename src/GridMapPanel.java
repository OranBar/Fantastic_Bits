import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.*;
import javax.swing.JPanel;

public class GridMapPanel extends JPanel {

	private Shape[][] map;
	
	List<Shape> myShapes;
	private int width, height;
	private int xUnit, yUnit;
	private InfluenceMap influenceMap;
	
	GridMapPanel(int width, int height, int xUnit, int yUnit, InfluenceMap influenceMap){
		this.width = width;
		this.height = height;
		map = new Shape[width][];
		for(int i=0; i<width; i++){
			map[i] = new Shape[height];
		}
		this.xUnit = xUnit;
		this.yUnit = yUnit;
		this.myShapes = new ArrayList<Shape>();
		this.influenceMap = influenceMap;
		
		for(int x=0; x<width; x++){
			for(int y=0; y<height; y++){
				Shape shape = new Rectangle2D.Double((double)x*xUnit, (double)y*yUnit, (double)xUnit, (double) yUnit);

				myShapes.add(shape);
			}
		}
		
		addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
            	super.mouseClicked(me);
            	onMouseClicked(me);
            }
        });
	}
		
	@Override
	public void paintComponent(Graphics g){
		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D) g;
		
		for(Shape s : myShapes){
			g2d.draw(s);
		}
		
		for (int x = 0; x < map.length; x++) {
			for (int y = 0; y < map[x].length; y++) {
				double influenceValue = influenceMap.get(x, y);
				if(influenceValue > 0){
					if(influenceValue > 255){
						influenceValue = 255;
					}
					g.setColor(new Color((int)influenceValue, 0,0));
				} else if(influenceValue < 0){
					if(influenceValue < -255){
						influenceValue = -255;
					}
					g.setColor(new Color(0, 0,-(int)influenceValue)); 
				} else if(influenceValue == 0){
					g.setColor(Color.white);
				}
				
				g.fillRect(x*(xUnit)+1, y*(yUnit)+1, xUnit-1, yUnit-1);
				
			}
		}
	}
	
	public void onMouseClicked(MouseEvent me){
		for (Shape s : myShapes) {

            if (s.contains(me.getPoint())) {//check if mouse is clicked within shape
            	int x = me.getXOnScreen()/xUnit;
            	int y = (me.getYOnScreen()/yUnit)-1;
                System.out.println("Clicked  "+x+" "+y);
                
                if(me.getButton() == MouseEvent.BUTTON1){
                	influenceMap.applyInfluence(x, y, 100, 2, 2, 0.7f);
                } else {
                	influenceMap.applyInfluence(x, y, -100, 1, 0, 0.5f);
                }
            }
        }
		repaint();
	}
	
	
}
