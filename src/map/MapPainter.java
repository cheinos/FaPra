package map;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

import main.MainFrame;
import map.objects.ExtendedGeoPosition;
import map.objects.ExtendedWay;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.painter.AbstractPainter;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.TileFactory;

/**
 * The painter class that draws all nodes and ways on top of the map
 * @author Florian
 *
 */
public class MapPainter extends AbstractPainter<JXMapViewer> {

	private Point2D firstGeoToPixelPoint;
	private Point2D secondGeoToPixelPoint;

	private int nodeSize = 7;

	private Rectangle rectangle;
	private TileFactory factory;
	private int zoomLevel;
	private JXMapViewer mapViewer;

	private BasicStroke wayStroke = new BasicStroke(1.f);
	private MainFrame mainFrame;

	private BufferedImage startImage;
	private BufferedImage endImage;
	private BufferedImage markImage;

	private GeoPosition firstNode;
	private GeoPosition secondNode;

	private ExtendedWay constructedWay;

	public MapPainter(MainFrame mainFrame) {
		mapViewer = mainFrame.getMapViewer();
		factory = mapViewer.getTileFactory();
		this.mainFrame = mainFrame;

		try {
			startImage = ImageIO.read(new File("./img/start.png"));
			endImage = ImageIO.read(new File("./img/end.png"));
			markImage = ImageIO.read(new File("./img/mark.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void doPaint(Graphics2D g, JXMapViewer mapView, int width, int height) {

		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		zoomLevel = mapViewer.getZoom();
		rectangle = mapViewer.getViewportBounds();
		g.setStroke(wayStroke);
		
		g.setColor(Color.RED);		
		if (mainFrame.nodesIsEnabled() || mainFrame.waysIsEnabled()) {
			for (ExtendedWay w : mainFrame.getMapData().getWayList().values()) {
				for (int i = 0; i < w.getWayNodes().size(); i++) {

					calculateFirstNode(mainFrame.getMapData().getNodeList().get(w.getWayNodes().get(i)));

					// Draw the nodes
					if (mainFrame.nodesIsEnabled()) {
							g.fillRect((int) (firstGeoToPixelPoint.getX() - rectangle.getX()),
									(int) (firstGeoToPixelPoint.getY() - rectangle.getY()), nodeSize, nodeSize);
					}

					// Draw the ways
					if (mainFrame.waysIsEnabled() && (i + 1) < w.getWayNodes().size()) {
						calculateSecondNode(mainFrame.getMapData().getNodeList().get(w.getWayNodes().get(i + 1)));
							drawLine(g);
					}

				}
			}
		}
		
		// Draw all ways that were calculated and added to the finalWayList
				if (!mainFrame.getFinalWayList().isEmpty() && mainFrame.getFinalWayBox().isSelected()) {
					for (ExtendedWay w : mainFrame.getFinalWayList().values()) {				
						g.setStroke(new BasicStroke(w.getStrokeWidth()));

						for (int i = 0; i < w.getWayNodes().size(); i++) {
							calculateFirstNode(mainFrame.getMapData().getNodeList().get(w.getWayNodes().get(i)));

							if ((i + 1) < w.getWayNodes().size()) {
								calculateSecondNode(mainFrame.getMapData().getNodeList().get(w.getWayNodes().get(i + 1)));
								
								if(mainFrame.getUtilizationSlider().isEnabled()){
									if(w.getUtilizationRate()>=mainFrame.getUtilizationSlider().getValue()){
										g.setColor(w.getWayColor());
										drawLine(g);
									}
								}else{
									g.setColor(w.getWayColor());
									drawLine(g);
								}
								
							}

						}
					}
				}

				// When the construction mode is enabled, we draw only the way the user
				// defines
				if (mainFrame.getConstructionMode().isSelected()
						&& mainFrame.getPopupClass().getConstructionState()) {
					constructedWay = mainFrame.getMapData().getWayList().get(mainFrame.getPopupClass().getConstructedWayId());
					g.setStroke(new BasicStroke(constructedWay.getStrokeWidth()));

					for (int i = 0; i < constructedWay.getWayNodes().size(); i++) {
						calculateFirstNode(mainFrame.getMapData().getNodeList().get(constructedWay.getWayNodes().get(i)));

						g.setColor(constructedWay.getWayColor());
						g.fillRect((int) (firstGeoToPixelPoint.getX() - rectangle.getX()),
								(int) (firstGeoToPixelPoint.getY() - rectangle.getY()), nodeSize, nodeSize);

						if ((i + 1) < constructedWay.getWayNodes().size()) {
							calculateSecondNode(
									mainFrame.getMapData().getNodeList().get(constructedWay.getWayNodes().get(i + 1)));
							
							g.setColor(constructedWay.getWayColor());
							drawLine(g);
						}
					}

				}
		
		// Draw a mark for the selected point or color all nodes of a selected way
		if (mainFrame.getSelectedWay() != null) {
			for (int i = 0; i < mainFrame.getSelectedWay().getWayNodes().size(); i++) {
				calculateFirstNode(mainFrame.getMapData().getNodeList().get(mainFrame.getSelectedWay().getWayNodes().get(i)));
				
				g.setColor(mainFrame.getSelectedWay().getWayColor());
				g.fillRect((int) (firstGeoToPixelPoint.getX() - rectangle.getX()),
						(int) (firstGeoToPixelPoint.getY() - rectangle.getY()), nodeSize, nodeSize);
				
				if ((i + 1) < mainFrame.getSelectedWay().getWayNodes().size()) {
					calculateSecondNode(mainFrame.getMapData().getNodeList().get(mainFrame.getSelectedWay().getWayNodes().get(i + 1)));

					g.setColor(mainFrame.getSelectedWay().getWayColor());
					drawLine(g);
				}
			}
		}
		
		// Draw a flag for the start point
		if (mainFrame.getStartNode() != null) {
			calculateFirstNode(mainFrame.getStartNode());

			g.drawImage(startImage, (int) (firstGeoToPixelPoint.getX() - rectangle.getX()),
					(int) (firstGeoToPixelPoint.getY() - rectangle.getY()) - startImage.getHeight(), null);
		}

		// Draw a flag for the end point
		if (mainFrame.getEndNode() != null) {
			calculateFirstNode(mainFrame.getEndNode());

			g.drawImage(endImage, (int) (firstGeoToPixelPoint.getX() - rectangle.getX()),
					(int) (firstGeoToPixelPoint.getY() - rectangle.getY()) - startImage.getHeight(), null);
		}
		
		//Draw a mark for the compare end node that is set in the compare mode
		if (mainFrame.getCompareEndNode() != null) {
			calculateFirstNode(mainFrame.getCompareEndNode());

			g.drawImage(markImage, (int) (firstGeoToPixelPoint.getX() - rectangle.getX()),
					(int) (firstGeoToPixelPoint.getY() - rectangle.getY()) - startImage.getHeight(), null);
		}

	}
	
	public void drawLine(Graphics2D g){
		g.drawLine((int) (firstGeoToPixelPoint.getX() - rectangle.getX()),
				(int) (firstGeoToPixelPoint.getY() - rectangle.getY()),
				(int) (secondGeoToPixelPoint.getX() - rectangle.getX()),
				(int) (secondGeoToPixelPoint.getY() - rectangle.getY()));
	}

	private void calculateFirstNode(ExtendedGeoPosition currentNode) {

		firstNode = new GeoPosition(currentNode.getLatitude(), currentNode.getLongitude());
		firstGeoToPixelPoint = factory.geoToPixel(firstNode, zoomLevel);
	}

	private void calculateSecondNode(ExtendedGeoPosition currentNode) {

		secondNode = new GeoPosition(currentNode.getLatitude(), currentNode.getLongitude());
		secondGeoToPixelPoint = factory.geoToPixel(secondNode, zoomLevel);
	}

}
