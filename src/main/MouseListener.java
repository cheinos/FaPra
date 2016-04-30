package main;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;
import javax.swing.event.MouseInputListener;

import map.objects.ExtendedGeoPosition;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.input.CenterMapListener;
import org.jxmapviewer.input.PanKeyListener;
import org.jxmapviewer.input.PanMouseInputListener;
import org.jxmapviewer.input.ZoomMouseWheelListenerCursor;
import org.jxmapviewer.viewer.GeoPosition;


/**
 * All necessary mouse functions to work with the map are implemented in this class.
 * @author Florian
 *
 */
public class MouseListener {

	private JPopupMenu popupMenu;
	private GeoPosition clickOnMap;
	
	private int newNodeCounter = 0;
	
	public MouseListener(JXMapViewer mapViewer, MainFrame mainFrame) {
		popupMenu = mainFrame.getPopupClass().getPopupMenu();
		
		MouseInputListener mia = new PanMouseInputListener(mapViewer);
		mapViewer.addMouseListener(mia);
		mapViewer.addMouseMotionListener(mia);

		mapViewer.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if(e.getButton()==1){					
					popupMenu.setVisible(false);
				}
				
				if(e.getButton()==3){
					mapViewer.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
					
					if(!mainFrame.isNewMapLoad() && !mainFrame.isAllPaths()){
						popupMenu = mainFrame.getPopupClass().getPopupMenu();
						
						clickOnMap = mapViewer.convertPointToGeoPosition(e.getPoint());
						popupMenu.setLocation(e.getXOnScreen()+10,
								e.getYOnScreen()+10);
						popupMenu.setVisible(true);
					}
				}
			}

			@Override
			public void mouseReleased(MouseEvent evt) {
				// Fix annoying cursor bug
				mapViewer.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			}

		});
		
		mapViewer.addMouseWheelListener(new ZoomMouseWheelListenerCursor(mapViewer));
		mapViewer.addKeyListener(new PanKeyListener(mapViewer));

		mapViewer.addMouseListener(new CenterMapListener(mapViewer));

	}

	public ExtendedGeoPosition getClickOnMap() {
		return new ExtendedGeoPosition(newNodeCounter++, clickOnMap.getLatitude(), clickOnMap.getLongitude());
	}
}
