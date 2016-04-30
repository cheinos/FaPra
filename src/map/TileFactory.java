package map;

import java.io.File;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.VirtualEarthTileFactoryInfo;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.LocalResponseCache;
import org.jxmapviewer.viewer.TileFactoryInfo;

/**
 * Creates different {@link TileFactoryInfo} objects to load the map views for the user.
 * @author Florian
 *
 */
public class TileFactory {

	// Create a TileFactoryInfo for OpenStreetMap
	private TileFactoryInfo osmInfo = new OSMTileFactoryInfo();
	private TileFactoryInfo veInfoMap = new VirtualEarthTileFactoryInfo(
			VirtualEarthTileFactoryInfo.MAP);
	private TileFactoryInfo veInfoSatellite = new VirtualEarthTileFactoryInfo(
			VirtualEarthTileFactoryInfo.SATELLITE);
	private TileFactoryInfo veInfoHybrid = new VirtualEarthTileFactoryInfo(
			VirtualEarthTileFactoryInfo.HYBRID);
	
	private DefaultTileFactory defaultTileFactory;
	private JXMapViewer mapViewer;

	public TileFactory(JXMapViewer mapViewer) {

		this.mapViewer = mapViewer;
		
		defaultTileFactory = new DefaultTileFactory(osmInfo);
		
		//how many tiles shall be loaded in the background
		defaultTileFactory.setThreadPoolSize(16); 
		mapViewer.setTileFactory(defaultTileFactory);

		// Setup local file cache
		File cacheDir = new File(System.getProperty("user.home")
				+ File.separator + ".jxmapviewer2");
		LocalResponseCache.installResponseCache(osmInfo.getBaseURL(), cacheDir,
				false);
	}

	/**
	 * Sets the TileFactory of the map to a selected value.
	 * @param selectedItem The selected TileFactory from the ComboBox
	 */
	public void setNewTileFactory(String selectedItem) {
		
		switch(selectedItem){
			case "OpenStreetMap": 
				defaultTileFactory = new DefaultTileFactory(osmInfo);
				mapViewer.setTileFactory(defaultTileFactory);
				break;
			case "Virtual Earth - MAP":	
				defaultTileFactory = new DefaultTileFactory(veInfoMap);
				mapViewer.setTileFactory(defaultTileFactory);
				
				if(mapViewer.getZoom()<veInfoMap.getMinimumZoomLevel())
					mapViewer.setZoom(veInfoMap.getMinimumZoomLevel());
				break;
			case "Virtual Earth - SATELLITE":	
				defaultTileFactory = new DefaultTileFactory(veInfoSatellite);
				mapViewer.setTileFactory(defaultTileFactory);
				
				if(mapViewer.getZoom()<veInfoSatellite.getMinimumZoomLevel())
					mapViewer.setZoom(veInfoSatellite.getMinimumZoomLevel());
				break;
			case "Virtual Earth - HYBRID":	
				defaultTileFactory = new DefaultTileFactory(veInfoHybrid);
				mapViewer.setTileFactory(defaultTileFactory);
				
				if(mapViewer.getZoom()<veInfoHybrid.getMinimumZoomLevel())
					mapViewer.setZoom(veInfoHybrid.getMinimumZoomLevel());
				break;
		}
		
	}

}
