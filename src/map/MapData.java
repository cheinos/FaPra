package map;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

import main.MainFrame;
import map.objects.ExtendedGeoPosition;
import map.objects.ExtendedWay;
import map.objects.WayType;

import org.jxmapviewer.viewer.GeoPosition;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.core.task.v0_6.RunnableSource;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

/**
 * Loads a .osm.pbf file and creates {@link ExtendedWay} and {@link ExtendedGeoPosition} objects,
 * that can be stored in hashmaps.
 * @author Florian
 *
 */
public class MapData extends SwingWorker<Void, Void> {

	private String mapString;

	private HashMap<Long, ExtendedWay> wayList = new HashMap<Long, ExtendedWay>();
	private HashMap<Long, ExtendedGeoPosition> nodeList = new HashMap<Long, ExtendedGeoPosition>();

	private Node tempNode;
	private Way tempWay;
	private ExtendedWay tempExtendedWay;

	private MainFrame mainFrame;

	private DefaultTableModel nodeTableModel;
	private DefaultTableModel wayTableModel;

	private WayType type;
	private boolean oneWay;
	private int maxSpeed;
	private String wayName;

	private boolean startReadingNodes = true;
	private boolean startReadingWays = false;

	private Pattern pattern = Pattern.compile("[0-9]+");
	private Matcher matcher;
	private Entity entity;
	

	public MapData(MainFrame mainFrame, JTable nodeTable, JTable wayTable) {
		this.mainFrame = mainFrame;
		nodeTableModel = (DefaultTableModel) nodeTable.getModel();
		wayTableModel = (DefaultTableModel) wayTable.getModel();
	}

	@Override
	protected Void doInBackground() {
		System.out.println("----------------------------");
		System.out.println("Load map data from file " + mapString + "...");

		wayList.clear();
		nodeList.clear();

		nodeTableModel.setRowCount(0);
		wayTableModel.setRowCount(0);

		Sink sinkImplementation = new Sink() {

			@Override
			public void process(EntityContainer entityContainer) {
				entity = entityContainer.getEntity();

				if (startReadingNodes) {
					System.out.println("\t" + "Read Nodes...");
					startReadingNodes = false;
					startReadingWays = true;
				}

				if (entity instanceof Node) {

					tempNode = (Node) entity;
					nodeList.put(tempNode.getId(),
							new ExtendedGeoPosition(tempNode.getId(), tempNode.getLatitude(), tempNode.getLongitude()));

					nodeTableModel
							.addRow(new Object[] { tempNode.getId(), tempNode.getLatitude(), tempNode.getLongitude() });

				}

				if (startReadingWays) {
					System.out.println("\t" + "Read Ways...");
					startReadingWays = false;
				}

				if (entity instanceof Way) {
					tempWay = (Way) entity;

					// Reset the parameters
					type = WayType.EMPTY;
					wayName = null;
					oneWay = false;
					maxSpeed = -1;

					for (Tag t : tempWay.getTags()) {
						if (t.getKey().equals("name")) {
							wayName = t.getValue();
						}

						if (t.getKey().equals("oneway")) {
							if (t.getValue().equals("yes")) {
								oneWay = true;
							}
						}

						if (t.getKey().equals("maxspeed") || t.getKey().equals("maxspeed:forward")) {
							matcher = pattern.matcher(t.getValue());
							while (matcher.find()) {
								maxSpeed = Integer.parseInt(t.getValue().substring(matcher.start(), matcher.end()));
							}

						}

						if (t.getKey().equals("highway")) {

							// If Max speed couldn't be set, because there was
							// no declaration for it,
							// we set the max speed based on the street type
							if(maxSpeed==-1){
								if (t.getValue().equals("motorway")) {
									maxSpeed = 130;
								} else if (t.getValue().equals("motorway_link") || t.getValue().equals("primary")
										|| t.getValue().equals("primary_link")) {
									maxSpeed = 100;
								} else if (t.getValue().equals("secondary") || t.getValue().equals("secondary_link")
										|| t.getValue().equals("tertiary") || t.getValue().equals("tertiary_link")
										|| t.getValue().equals("trunk") || t.getValue().equals("trunk_link")) {
									maxSpeed = 70;
								} else if (t.getValue().equals("unclassified") 
										|| t.getValue().equals("road")) {
									maxSpeed = 50;
								} else if (t.getValue().equals("living_street") || t.getValue().equals("service") || t.getValue().equals("residential")
										|| t.getValue().equals("track")) {
									maxSpeed = 30;
								} else if (t.getValue().equals("footway") || t.getValue().equals("pedestrian")
										|| t.getValue().equals("steps") || t.getValue().equals("path")) {
									maxSpeed = 5;
								} else if (t.getValue().equals("cycleway")) {
									maxSpeed = 20;
								}
							}
							
							// Set the WayType
							if (t.getValue().equals("motorway") || t.getValue().equals("motorway_link")
									|| t.getValue().equals("primary")
									|| t.getValue().equals("primary_link")
									|| t.getValue().equals("trunk") || t.getValue().equals("trunk_link")
									|| t.getValue().equals("unclassified")
									|| t.getValue().equals("road")) {
								type = WayType.HIGHWAY;
							} else if (t.getValue().equals("residential") || t.getValue().equals("living_street") || t.getValue().equals("service")
									|| t.getValue().equals("track") || t.getValue().equals("cycleway") 
									|| t.getValue().equals("secondary") || t.getValue().equals("secondary_link") || t.getValue().equals("tertiary") || t.getValue().equals("tertiary_link")) {
								type = WayType.HIGHWAY_WITH_FOOTWAY;
							} else if (t.getValue().equals("footway") || t.getValue().equals("pedestrian")
									|| t.getValue().equals("steps") || t.getValue().equals("path")) {
								type = WayType.FOOTWAY;
							}

						} else if (t.getKey().equals("footway")) {
							type = WayType.FOOTWAY;
						} else if (t.getKey().equals("sidewalk")) {
							if (!t.getValue().equals("none")) {
								type = WayType.HIGHWAY_WITH_FOOTWAY;
							} else {
								type = WayType.HIGHWAY;
							}

						}
					}

					// Filter unnecessary ways
					if (!type.equals(WayType.EMPTY)) {
						/*
						 * Necessary fix because Osmosis maps only Ways to
						 * WayNodeLists. But a WayNode has only an ID attribute
						 * and no Latitude and Longitude. So we have to create a
						 * list by our own.
						 */
						List<Long> nodeIDs = new ArrayList<Long>();

						for (WayNode wn : tempWay.getWayNodes()) {
							nodeIDs.add(wn.getNodeId());

							// For each node on the map we add the IDs of the							
							// ways it belongs to
							nodeList.get(wn.getNodeId()).addWayId(tempWay.getId());
						}

						tempExtendedWay = new ExtendedWay(tempWay.getId(), type, wayName, oneWay, maxSpeed,
								mainFrame.getMapAlgorithms().calculateWayLength(nodeIDs), nodeIDs);

						wayList.put(tempExtendedWay.getId(), tempExtendedWay);
						wayTableModel.addRow(new Object[] { tempExtendedWay.getId(), tempExtendedWay.getWayName(),
								(int) tempExtendedWay.getWayLength(), tempExtendedWay.getType() });
					}
				}
			}

			@Override
			public void complete() {
				System.out.println("\n"+"Size of unfiltered list: " + nodeList.values().size());

				// Delete all nodes that don't belong to a way
				Iterator<Long> iterator = nodeList.keySet().iterator();
				while (iterator.hasNext()) {
					if (nodeList.get(iterator.next().longValue()).getWayIDs().isEmpty())
						iterator.remove();
				}
				System.out.println("Size of filtered list: " + nodeList.values().size()+"\n");

				startReadingNodes = true;
				startReadingWays = false;
			}

			@Override
			public void initialize(Map<String, Object> arg0) {
			}

			@Override
			public void release() {
			}
		};

		try {
			RunnableSource reader = new crosby.binary.osmosis.OsmosisReader(new FileInputStream("./maps/" + mapString));
			reader.setSink(sinkImplementation);
			reader.run();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		System.out.println("Finished reading map data!");
		System.out.println("----------------------------");

		return null;
	}

	@Override
	protected void done() {
		GeoPosition geo = new GeoPosition(getNodeList().values().iterator().next().getLatitude(),
				getNodeList().values().iterator().next().getLongitude());
		mainFrame.getMapViewer().setAddressLocation(geo);

		mainFrame.getProgressBar().setMaximum(nodeList.values().size());
		mainFrame.getProgressBar().setValue(mainFrame.getProgressBar().getMaximum());
		mainFrame.getProgressBar().setString("100%");
		mainFrame.getProgressBar().setIndeterminate(false);

		mainFrame.enableUIElements();
		mainFrame.enableCheckBoxes();

		mainFrame.setNewMapIsLoad(false);
	}
	
	/**
	 * Loads all available maps from the ./maps folder. Only files that end with .pbf are considered.
	 * @return A String array with all map names
	 */
	public String[] getAvailableMaps() {
		File f = new File("./maps");

		// Filter all files that are have no osm.pbf extension
		File[] fileArray = f.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".pbf");
			}
		});

		String[] fileNames = new String[fileArray.length];

		for (int i = 0; i < fileArray.length; i++) {
			fileNames[i] = fileArray[i].getName();
		}

		return fileNames;
	}

	public HashMap<Long, ExtendedWay> getWayList() {
		return wayList;
	}

	public HashMap<Long, ExtendedGeoPosition> getNodeList() {
		return nodeList;
	}

	public void setMapString(String mapString) {
		this.mapString = mapString;
	}

	public String getMapString() {
		return mapString;
	}

}
