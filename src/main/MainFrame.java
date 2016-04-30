package main;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

import map.MapAlgorithms;
import map.MapData;
import map.MapPainter;
import map.TileFactory;
import map.objects.ExtendedGeoPosition;
import map.objects.ExtendedWay;

import org.jxmapviewer.JXMapViewer;

public class MainFrame extends JPanel implements ItemListener, ActionListener {

	//-------------UI STUFF----------------------
	private JFrame mainFrame = new JFrame("Fachpraktikum - Algorithmen für OSM");
	
	private JPanel mapControlPanel = new JPanel();
	private JPanel routingPanel = new JPanel();
	private JPanel routeOptionPanel = new JPanel();
	private JPanel leftPanel = new JPanel();
	private JPanel bottomPanel = new JPanel();
	
	private Object[][] tableData = {};
	
	private String[] columnsNodes = { "ID", "Lat", "Lon" };
	private String[] columnsWays = { "ID", "Street name", "Length in meter",
			"Type" };

	private JTable nodeTable = new JTable(new DefaultTableModel(tableData,
			columnsNodes) {
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return false;
		}
	});

	private JTable wayTable = new JTable(new DefaultTableModel(tableData,
			columnsWays) {
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return false;
		}
	});
	private JSlider utilizationSlider = new JSlider(JSlider.HORIZONTAL);
	
	private JLabel bottomLabel = new JLabel();
	private JLabel transportationTypeLabel = new JLabel("Transportation type");
	private JLabel wayCalculationLabel = new JLabel("Calculation mode");
	private JLabel timeToGoalLabel = new JLabel("Time: ---         ");
	private JLabel distanceToGoalLabel = new JLabel("Distance: ---   ");
	
	private JComboBox<String> availableMapsBox;	
	private JComboBox<String> tileFactoryBox;
	
	private JCheckBox showNodes = new JCheckBox("Show nodes");
	private JCheckBox showWays = new JCheckBox("Show ways");
	private JCheckBox constructionMode = new JCheckBox("Construction mode");
	private JCheckBox showFinalWay = new JCheckBox("Show calculated way");
	private JCheckBox compareMode = new JCheckBox("Compare mode");

	PopupMenu popupClass = new PopupMenu(this);
	
	private JButton compareTwoWaySetsButton = new JButton("Compare two way sets");
	private JButton calculateAllPathsButton = new JButton("Centralize all routes");
	private JButton loadMapButton = new JButton("Load selected map");

	private ButtonGroup transportationTypeGroup = new ButtonGroup();
	private JRadioButton byFoot = new JRadioButton("Foot");
	private JRadioButton byCar = new JRadioButton("Car");
	
	private ButtonGroup calculationModeGroup = new ButtonGroup();
	private JRadioButton shortestWay = new JRadioButton("Shortest Way");
	private JRadioButton fastestWay = new JRadioButton("Fastest Way");
	
	private JProgressBar progressBar = new JProgressBar();
		
	private boolean clickInTable;
	private boolean newMapIsLoad;
	private boolean calculateAllPaths;
	
	private int selectedRow;
	private float progress;
	
	//-----------MAP STUFF-----------------------
	private JXMapViewer mapViewer = new JXMapViewer();
	private MouseListener mouseListener = new MouseListener(mapViewer, this);
	
	private TileFactory factory = new TileFactory(mapViewer);
	
	private MapPainter mapPainter = new MapPainter(this);
	private MapData mapData = new MapData(MainFrame.this, nodeTable, wayTable);
	private MapAlgorithms mapAlgorithms = new MapAlgorithms(this);
	
	private ExtendedGeoPosition startNode;
	private ExtendedGeoPosition endNode;
	private ExtendedGeoPosition compareEndNode;

	private ExtendedWay selectedWay;

	private HashMap<Long, ExtendedWay> finalWayList = new HashMap<Long, ExtendedWay>();
	private HashMap<Long, ExtendedWay> compareWayList =  new HashMap<Long, ExtendedWay>();
	
	public MainFrame() {
		//----------
		//LOAD DATA
		//----------
		setTileFactories();
		
		//----------
		//MAP STUFF
		//----------
		availableMapsBox = new JComboBox<String>(mapData.getAvailableMaps());
		availableMapsBox.setSelectedItem("Miami.osm.pbf");		
		mapData.setMapString((String)availableMapsBox.getSelectedItem());		
		loadMapData();
		mapViewer.setZoom(5);

		//----------
		//MAINFRAME
		//----------
		mainFrame.setLayout(new BorderLayout());
		mainFrame.setSize(2000, 800);
		mainFrame.setLocationRelativeTo(null); //set the frame in the middle of the screen
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		// frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
		mainFrame.setVisible(true);

		mainFrame.add(leftPanel, BorderLayout.WEST);
		mainFrame.add(mapViewer, BorderLayout.CENTER);
		mainFrame.add(bottomPanel, BorderLayout.SOUTH);
		
		//----------
		//BOTTOM PANEL
		//----------
		bottomPanel.add(bottomLabel);
		
		progressBar.setStringPainted(true);
		bottomPanel.add(progressBar);
		
		//----------
		//LEFT PANEL
		//----------
		leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
		
		//MAP CONTROL
		GroupLayout mapControlLayout = new GroupLayout(mapControlPanel);
		mapControlLayout.setAutoCreateGaps(true);
		mapControlLayout.setAutoCreateContainerGaps(true);
		mapControlPanel.setLayout(mapControlLayout);
		
		mapControlLayout.setHorizontalGroup(mapControlLayout.createSequentialGroup()
				.addGroup(mapControlLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
						.addGroup(mapControlLayout.createSequentialGroup()
								.addGroup(mapControlLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
										.addComponent(showNodes))
								.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED, GroupLayout.DEFAULT_SIZE, 70)
								.addGroup(mapControlLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
										.addComponent(showWays)))
						.addGroup(mapControlLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
								.addComponent(tileFactoryBox)
								.addComponent(availableMapsBox)))
				.addGroup(mapControlLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
						.addComponent(showFinalWay)
				        .addComponent(loadMapButton))  
		);
		mapControlLayout.setVerticalGroup(
				   mapControlLayout.createSequentialGroup()
				      .addGroup(mapControlLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
    					   .addComponent(showNodes)
				           .addComponent(showWays)
				           .addComponent(showFinalWay))
				      .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, 20)
				      .addGroup(mapControlLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
				    	   .addComponent(tileFactoryBox)) 
				      .addGroup(mapControlLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
				    	   .addComponent(availableMapsBox)
				    	   .addComponent(loadMapButton))
		);		
		leftPanel.add(mapControlPanel);
				
		//TRANSPORTATION TYPE
		transportationTypeGroup.add(byFoot);
		transportationTypeGroup.add(byCar);
		
		calculationModeGroup.add(fastestWay);
		calculationModeGroup.add(shortestWay);
		
		GroupLayout transportationTypeLayout = new GroupLayout(routeOptionPanel);
		transportationTypeLayout.setAutoCreateGaps(true);
		transportationTypeLayout.setAutoCreateContainerGaps(true);
		routeOptionPanel.setLayout(transportationTypeLayout);
		
		transportationTypeLayout.setHorizontalGroup(transportationTypeLayout.createSequentialGroup()
				.addGroup(transportationTypeLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
						.addComponent(transportationTypeLabel)
				        .addComponent(byCar)
				        .addComponent(byFoot))
				.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, 50)
				.addGroup(transportationTypeLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
						.addComponent(wayCalculationLabel)
				        .addComponent(shortestWay)
				        .addComponent(fastestWay))
				.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, 50)
				.addGroup(transportationTypeLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
						.addComponent(distanceToGoalLabel)
						.addComponent(timeToGoalLabel))
		);
		transportationTypeLayout.setVerticalGroup(
				   transportationTypeLayout.createSequentialGroup()
				      .addGroup(transportationTypeLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
				           .addComponent(transportationTypeLabel)
				           .addComponent(wayCalculationLabel))
				      .addGroup(transportationTypeLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
				    		.addComponent(byCar) 
				    		.addComponent(shortestWay)
				    		.addComponent(distanceToGoalLabel))
				      .addGroup(transportationTypeLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
				    	   .addComponent(byFoot)
				           .addComponent(fastestWay)
				           .addComponent(timeToGoalLabel))
		);
		//Set default transportation type
		byCar.setSelected(true);		
		//Set default routing type
		shortestWay.setSelected(true);
		
		leftPanel.add(routeOptionPanel);
		
		//ROUTING
		GroupLayout routingLayout = new GroupLayout(routingPanel);
		routingLayout.setAutoCreateGaps(true);
		routingLayout.setAutoCreateContainerGaps(true);
		routingPanel.setLayout(routingLayout);
		
		routingLayout.setHorizontalGroup(routingLayout.createSequentialGroup()
				.addGroup(routingLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
						.addGroup(routingLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
								.addComponent(calculateAllPathsButton, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
								          Short.MAX_VALUE))
						.addGroup(routingLayout.createSequentialGroup()
								.addGroup(routingLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
										.addComponent(constructionMode))
								.addGroup(routingLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
										.addComponent(compareMode))))
				.addGroup(routingLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
				        .addComponent(utilizationSlider)
				        .addComponent(compareTwoWaySetsButton, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
						          Short.MAX_VALUE))
		);
		routingLayout.setVerticalGroup(
				routingLayout.createSequentialGroup()
				      .addGroup(routingLayout.createParallelGroup(GroupLayout.Alignment.BASELINE,false)
				    		  .addComponent(calculateAllPathsButton)
				    		  .addComponent(utilizationSlider))
				      .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED, GroupLayout.DEFAULT_SIZE, 10)
				      .addGroup(routingLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
				    		  .addComponent(constructionMode)
				    		  .addComponent(compareMode)
				    		  .addComponent(compareTwoWaySetsButton))
		);
		Hashtable<Integer, JLabel> labels =
                new Hashtable<Integer, JLabel>();
        labels.put(0, new JLabel("High"));
        labels.put(utilizationSlider.getMaximum(), new JLabel("Low"));

        utilizationSlider.putClientProperty("JSlider.isFilled", Boolean.FALSE);
        utilizationSlider.setLabelTable(labels);
        utilizationSlider.setPaintLabels(true);
		utilizationSlider.setEnabled(false);
		
		leftPanel.add(routingPanel);
		
		//TABLES
		JScrollPane nodeScrollPane = new JScrollPane(nodeTable);
		JScrollPane wayScrollPane = new JScrollPane(wayTable);

		leftPanel.add(nodeScrollPane);
		leftPanel.add(wayScrollPane);
		
		// ---------
		// LISTENER
		// ---------
		utilizationSlider.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				mapViewer.repaint();				
			}
		});

		
		nodeTable.getSelectionModel().addListSelectionListener(
				new ListSelectionListener() {
					public void valueChanged(ListSelectionEvent e) {
						if(newMapIsLoad==false){
							selectedRow = nodeTable.getSelectedRow();
	
							// It's not allowed to select more than one row
							if (selectedRow > -1) {
								nodeTable.setRowSelectionInterval(selectedRow,
										selectedRow);
							}
							
							long id = (long) nodeTable.getValueAt(selectedRow,
									0);
	
							double latitude = (double) nodeTable.getValueAt(selectedRow,
									1);
							double longitude = (double) nodeTable.getValueAt(selectedRow,
									2);
	
							ExtendedGeoPosition selectedNode = new ExtendedGeoPosition(id, latitude, longitude);
							
							List<Long> nodeList = new ArrayList<Long>();
							nodeList.add(selectedNode.getId());
							selectedWay = new ExtendedWay(-1,-1, nodeList);
							
							mapViewer.setCenterPosition(selectedNode);
						}
					}
				});

		nodeTable.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getButton() == 1) {					
					popupClass.getPopupMenu().setVisible(false);
				}

				// Right mouse button opens the popup menu
				if (e.getButton() == 3 && !newMapIsLoad && !compareMode.isSelected() && !isAllPaths()) {
					if (nodeTable.rowAtPoint(e.getPoint()) == nodeTable
							.getSelectedRow()) {
						popupClass.getPopupMenu().setLocation(e.getXOnScreen() + 10,
								e.getYOnScreen() + 10);
						clickInTable = true;
						popupClass.getPopupMenu().setVisible(true);
					} else {
						popupClass.getPopupMenu().setVisible(false);
					}
				}
			}
		});
	
		compareTwoWaySetsButton.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if(compareTwoWaySetsButton.isEnabled())
					calculateAllPaths();
			}
		});

		calculateAllPathsButton.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (calculateAllPathsButton.isEnabled()) {
					popupClass.getPopupMenu().setVisible(false);
					if (getEndNode() != null) {
						distanceToGoalLabel.setText("Distance: ---   ");
						timeToGoalLabel.setText("Time: ---         ");
						calculateAllPaths();
					} else {
						finalWayList.clear();
						distanceToGoalLabel.setText("Distance: ---   ");
						timeToGoalLabel.setText("Time: ---         ");
						createDialog("No goal node was set!", "Error!");
					} 
				}
			}

		});
		
		loadMapButton.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (loadMapButton.isEnabled()) {
					popupClass.getPopupMenu().setVisible(false);
					loadMapData();
				}
			}

		});
		
		//ItemListener
		compareMode.addItemListener(this);
		showNodes.addItemListener(this);
		showWays.addItemListener(this);
		showFinalWay.addItemListener(this);
		constructionMode.addItemListener(this);
		
		//ActionListener
		byFoot.addActionListener(this);
		byCar.addActionListener(this);
		fastestWay.addActionListener(this);
		shortestWay.addActionListener(this);
		
		//WindowListener
		mainFrame.addWindowFocusListener(new WindowListener(this));
		mainFrame.addComponentListener(new WindowListener(this));
		
		//KeyListener
		leftPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
		leftPanel.getActionMap().put("delete", new AbstractAction() {
	            @Override
	            public void actionPerformed(ActionEvent e) {
	                if(constructionMode.isSelected() && selectedWay!=null){
	                	popupClass.deleteWay();
	                	mapViewer.repaint();
	                }
	            }
	        });
		
		mapViewer.setOverlayPainter(mapPainter);
		mainFrame.validate();
	}

	/**
	 * Starts the Dijkstra algorithm for a given start node. ALL paths from the
	 * start node to all other nodes are calculated.
	 */
	public void calculateAllPaths() {
		System.out.println("\n"+"Search all routes to goal...");
		calculateAllPaths = true;
		startNode = null;
		
		showFinalWay.setSelected(false);
		showNodes.setSelected(false);
		showWays.setSelected(false);
		disableCheckboxes();

		progressBar.setValue(0);
		progressBar.setString("0%");
		mapAlgorithms = new MapAlgorithms(this);
		mapAlgorithms.execute();
	}

	/**
	 * Starts the Dijkstra algorithm for a given set of nodes. Only ONE path is
	 * calculated.
	 */
	protected void startRouting() {
		if (getStartNode() != getEndNode()) {
			System.out.println("\n"+"Search route to goal...");
			
			utilizationSlider.setEnabled(false);

			mapAlgorithms = new MapAlgorithms(this);
			mapAlgorithms.execute();

			mapViewer.repaint();
		} else {
			finalWayList.clear();
			distanceToGoalLabel.setText("Distance: ---   ");
			timeToGoalLabel.setText("Time: ---         ");
			createDialog("Start und goal node are the same!","Error!");
		}
	}
	
	/**
	 * The user can change the transportation type and the routing setting
	 * via the radio buttons. 
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		
		popupClass.getPopupMenu().setVisible(false);
		
		if (startNode != null && endNode != null && !getConstructionMode().isSelected()){
			startRouting();
		}
	}
	
	/**
	 * Build the new popup menu when the construction mode is enabled.
	 */
	public void itemStateChanged(ItemEvent e) {
		Object source = e.getItemSelectable();
		
		popupClass.getPopupMenu().setVisible(false);
		popupClass.getSelectWayButton().setText("Select way");
		
		if(source.equals(compareMode)){
			constructionMode.setSelected(false);
			constructionMode.setEnabled(false);
			compareTwoWaySetsButton.setEnabled(false);
			
			resetVariables();
			compareWayList.clear();
			setCompareEndNode(null);
			
			if(compareMode.isSelected()){
				disableUIElements();
			}else{
				enableUIElements();
			}
			
			popupClass.buildSelectNodesPopup();
		}else if (source.equals(constructionMode)) {
			if(constructionMode.isSelected()){
				resetVariables();
				
				disableUIElements();
				
				//Build new PopUpenu
				popupClass.buildConstructionPopup();
			}else{				
				popupClass.buildSelectNodesPopup();
				selectedWay = null;
				
				if(!compareMode.isSelected())
					enableUIElements();
			}
		}
		
		mapViewer.repaint();
	}
	
	/**
	 * Clears the different lists and resets the start and end node.
	 */
	public void resetVariables(){
		finalWayList.clear();
		getFinalWayBox().setSelected(false);
		getUtilizationSlider().setEnabled(false);
				
		setStartNode(null);
		setEndNode(null);
		
		selectedWay = null;
	}

	/**
	 * Load different sources for the map tiles
	 */
	private void setTileFactories() {

		String[] factoryLabels = new String[4];
		factoryLabels[0] = "OpenStreetMap";
		factoryLabels[1] = "Virtual Earth - MAP";
		factoryLabels[2] = "Virtual Earth - SATELLITE";
		factoryLabels[3] = "Virtual Earth - HYBRID";

		tileFactoryBox = new JComboBox<String>(factoryLabels);
		tileFactoryBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				popupClass.getPopupMenu().setVisible(false);
				factory.setNewTileFactory((String) tileFactoryBox.getSelectedItem());
			}
		});

	}

	/**
	 * Load the map data from the choosen file.
	 */
	private void loadMapData() {
		finalWayList.clear();
		startNode = null;
		endNode = null;
		selectedWay = null;
		newMapIsLoad = true;
		
		disableUIElements();
		disableCheckboxes();
		
		progressBar.setString("");
		progressBar.setIndeterminate(true);
		bottomLabel.setText("Loaded map: "+(String)availableMapsBox.getSelectedItem());
		
		mapData = new MapData(MainFrame.this, nodeTable, wayTable);
		mapData.setMapString((String)availableMapsBox.getSelectedItem());
		mapData.execute();
	}

	/**
	 * Creates a Dialog window with a given text.
	 * 
	 * @param dialogText
	 *            The text that should be displayed in the dialog
	 */
	public void createDialog(String dialogText, String title) {
		JDialog dialog = new JDialog(mainFrame, true);
		dialog.setTitle(title);
		JPanel messagePane = new JPanel();

		messagePane.add(new JLabel(dialogText));
		dialog.add(messagePane);

		JButton dialogButton = new JButton("Ok");
		dialogButton.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				dialog.setVisible(false);
				dialog.dispose();
			}
		});

		dialog.add(dialogButton, BorderLayout.SOUTH);
		dialog.setLocationRelativeTo(mainFrame);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		dialog.pack();
		dialog.setVisible(true);
	}
	
	/**
	 * Creates a dialog at the end of the compare mode, that rates your changes made
	 * in the construction mode.
	 */
	public void createConstructionRatingDialog(int benefitNumber, int sufferingNumber){
		JDialog dialog = new JDialog(mainFrame, true);
		dialog.setTitle("Construction rating!");
		
		JPanel dialogPanel = new JPanel();
		GroupLayout dialogLayout = new GroupLayout(dialogPanel);
		dialogLayout.setAutoCreateGaps(true);
		dialogLayout.setAutoCreateContainerGaps(true);
		dialogPanel.setLayout(dialogLayout);
		
		JLabel textLabel = new JLabel("From all nodes...");
		
		DecimalFormat convertFormat = new DecimalFormat("00.00");
		float benefitNumberConverted = ((float)benefitNumber/getMapData().getNodeList().size());
		JLabel benefitLabel = new JLabel(convertFormat.format(benefitNumberConverted*100)+" % profit from your changes!");
		
		float sufferingNumberConverted = ((float)sufferingNumber/getMapData().getNodeList().size());
		JLabel sufferingLabel = new JLabel(convertFormat.format(sufferingNumberConverted*100)+" % do NOT profit from your changes!");

		JButton dialogButton = new JButton("Ok");
		dialogButton.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				dialog.setVisible(false);
				dialog.dispose();
			}
		});
		
		//Pictures taken from: http://www.clker.com/cliparts/5/2/5/8/13476359851958638477thumbs-down-icon-red-hi.png
		BufferedImage thumbUpImage = null;
		BufferedImage thumbDownImage = null;
		try {
			thumbUpImage = ImageIO.read(new File("./img/thumbUp.png"));
			thumbDownImage = ImageIO.read(new File("./img/thumbDown.png"));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		BufferedImage currentImage = null;
		if(benefitNumber > sufferingNumber){
			currentImage = thumbUpImage;
		}else{
			currentImage = thumbDownImage;
		}
		
		JLabel imagePanel = new JLabel(new ImageIcon(currentImage));
		
		dialogLayout.setHorizontalGroup(dialogLayout.createSequentialGroup()
				.addGroup(dialogLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
						   .addComponent(textLabel)
				           .addComponent(benefitLabel)
				           .addComponent(sufferingLabel))  
				.addGroup(dialogLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
				           .addComponent(imagePanel))
		);
		dialogLayout.setVerticalGroup(
				dialogLayout.createSequentialGroup()
				.addGroup(dialogLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
						.addGroup(dialogLayout.createSequentialGroup()
								.addGroup(dialogLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
										.addComponent(textLabel))
								.addGroup(dialogLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
										.addComponent(benefitLabel)))
						.addGroup(dialogLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
								.addComponent(imagePanel)))
						.addGroup(dialogLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
								.addComponent(sufferingLabel))
		);	
		dialog.add(dialogPanel);

		dialog.add(dialogButton, BorderLayout.SOUTH);
		dialog.setLocationRelativeTo(mainFrame);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		dialog.pack();
		dialog.setVisible(true);
	}
	
	public void disableCheckboxes(){
		getFinalWayBox().setEnabled(false);
		getShowWays().setEnabled(false);
		getShowNodes().setEnabled(false);
		getConstructionMode().setEnabled(false);
		getCompareMode().setEnabled(false);
	}
	
	public void enableCheckBoxes(){
		getFinalWayBox().setEnabled(true);
		getShowWays().setEnabled(true);
		getShowNodes().setEnabled(true);
		getConstructionMode().setEnabled(true);
		getCompareMode().setEnabled(true);
	}
	
	public JRadioButton getByFoot() {
		return byFoot;
	}

	public JRadioButton getByCar() {
		return byCar;
	}

	public JRadioButton getShortestWay() {
		return shortestWay;
	}

	public JRadioButton getFastestWay() {
		return fastestWay;
	}

	public boolean nodesIsEnabled() {
		return showNodes.isSelected();
	}

	public boolean waysIsEnabled() {
		return showWays.isSelected();
	}

	public ExtendedGeoPosition getStartNode() {
		return startNode;
	}

	public void setStartNode(ExtendedGeoPosition startNode) {
		this.startNode = startNode;
	}

	public ExtendedGeoPosition getEndNode() {
		return endNode;
	}

	public void setEndNode(ExtendedGeoPosition endNode) {
		this.endNode = endNode;
	}

	public MapData getMapData() {
		return mapData;
	}

	public HashMap<Long, ExtendedWay> getFinalWayList() {
		return finalWayList;	
	}

	public ExtendedWay getSelectedWay() {
		return selectedWay;
	}
	
	public void setSelectedWay(ExtendedWay way) {
		selectedWay = way;
	}

	public void setDistanceLabel(String distance) {
		distanceToGoalLabel.setText(distance);
	}

	public boolean isAllPaths() {
		return calculateAllPaths;
	}

	public void setCalculateAllPaths(boolean calculateAllPaths) {
		this.calculateAllPaths = calculateAllPaths;
	}
	
	public boolean isCalculateAllPaths() {
		return calculateAllPaths;
	}

	public MapAlgorithms getMapAlgorithms() {
		return mapAlgorithms;
	}

	public JXMapViewer getMapViewer() {
		return mapViewer;
	}

	public void setNewMapIsLoad(boolean newMapIsLoad) {
		this.newMapIsLoad = newMapIsLoad;
	}
	
	public JProgressBar getProgressBar() {
		return progressBar;
	}
	
	/**
	 * To prevent different exceptions, we deactivate the main ui elements,
	 * so that the user can't crash the program.
	 */
	public void disableUIElements(){
		utilizationSlider.setEnabled(false);
		
		showNodes.setSelected(false);
		showWays.setSelected(false);
		
		if(!compareMode.isSelected())
			compareTwoWaySetsButton.setEnabled(false);
		
		calculateAllPathsButton.setEnabled(false);
		loadMapButton.setEnabled(false);
		
		availableMapsBox.setEnabled(false);
		tileFactoryBox.setEnabled(false);
		
		byCar.setEnabled(false);
		byFoot.setEnabled(false);
		fastestWay.setEnabled(false);
		shortestWay.setEnabled(false);
	}
	
	public void enableUIElements(){
		showNodes.setEnabled(true);
		showWays.setEnabled(true);
		showFinalWay.setEnabled(true);
		constructionMode.setEnabled(true);
		
		loadMapButton.setEnabled(true);
		
		availableMapsBox.setEnabled(true);
		tileFactoryBox.setEnabled(true);
		
		byCar.setEnabled(true);
		byFoot.setEnabled(true);
		fastestWay.setEnabled(true);
		shortestWay.setEnabled(true);
	}
	
	public void setProgress(int progress){
		progressBar.setValue(progress);
		this.progress = ((float)progress/progressBar.getMaximum())*100;
		progressBar.setString(String.valueOf((int) this.progress)+"%");
	}
	
	public JCheckBox getFinalWayBox(){
		return showFinalWay;
	}

	public boolean isClickInTable() {
		return clickInTable;
	}

	public void setClickInTable(boolean clickInTable) {
		this.clickInTable = clickInTable;
	}

	public PopupMenu getPopupClass() {
		return popupClass;
	}

	public JTable getNodeTable() {
		return nodeTable;
	}

	public int getSelectedRow() {
		return selectedRow;
	}

	public MouseListener getMouseListener() {
		return mouseListener;
	}

	public JCheckBox getConstructionMode() {
		return constructionMode;
	}

	public JCheckBox getShowNodes() {
		return showNodes;
	}

	public JCheckBox getShowWays() {
		return showWays;
	}
	
	public boolean isNewMapLoad(){
		return newMapIsLoad;
	}
	
	public JFrame getFrame(){
		return mainFrame;
	}

	public void setTimeLabel(String time) {
		timeToGoalLabel.setText(time);
	}

	public JSlider getUtilizationSlider() {
		return utilizationSlider;
	}

	public JCheckBox getCompareMode() {
		return compareMode;
	}

	public ExtendedGeoPosition getCompareEndNode() {
		return compareEndNode;
	}

	public void setCompareEndNode(ExtendedGeoPosition compareEndNode) {
		this.compareEndNode = compareEndNode;
	}

	public HashMap<Long, ExtendedWay> getCompareWayList() {
		return compareWayList;
	}

	public void setCompareWayList(HashMap<Long, ExtendedWay> list) {
		this.compareWayList = new HashMap<Long, ExtendedWay>(list);
	}

	public JButton getCompareTwoWaySetsButton() {
		return compareTwoWaySetsButton;
	}

	public JLabel getBottomLabel() {
		return bottomLabel;
	}

	public JButton getCalculateAllPathsButton() {
		return calculateAllPathsButton;
	}
	
}
