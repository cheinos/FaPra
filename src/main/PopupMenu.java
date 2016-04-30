package main;

import java.awt.BorderLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;

import map.objects.ExtendedGeoPosition;
import map.objects.ExtendedWay;
import map.objects.WayType;

public class PopupMenu extends MouseAdapter {

	private MainFrame mainFrame;
	
	private JPopupMenu currentPopup = new JPopupMenu();
	
	private WayType wayType;
	private boolean oneWay;
	private int maxSpeed;
	
	/**
	 * Icons taken from: http://iconfever.com/images/portfolio/Construction-Icons-600.jpg
	 */
	private JMenuItem setStartNodeButton = new JMenuItem("Set as start point", new ImageIcon("./img/start.png"));
	private JMenuItem setEndNodeButton = new JMenuItem("Set as end point", new ImageIcon("./img/end.png"));
	private JMenuItem setNewNodeButton = new JMenuItem("Set new way point", new ImageIcon("./img/construction.png"));
	private JMenuItem selectWayButton = new JMenuItem("Select way", new ImageIcon("./img/select.png"));
	private JMenuItem deleteSelectedWayButton = new JMenuItem("Delete selected way", new ImageIcon("./img/delete.png"));
	private JMenuItem abortConstructionButton = new JMenuItem("Abort construction", new ImageIcon("./img/abort.png"));
	
	private boolean constructionHasStarted;
	
	private long constructedWayId;
	private int wayCounter = 0;
	
	private ExtendedGeoPosition constructedNode;

	public PopupMenu(MainFrame frame) {
		mainFrame = frame;

		setStartNodeButton.addMouseListener(this);
		setEndNodeButton.addMouseListener(this);
		setNewNodeButton.addMouseListener(this);
		selectWayButton.addMouseListener(this);
		deleteSelectedWayButton.addMouseListener(this);
		abortConstructionButton.addMouseListener(this);
		
		//define the default popup
		buildSelectNodesPopup();	
	}

	public void mouseClicked(MouseEvent evt) {
		if(!mainFrame.getConstructionMode().isSelected()){
			if (evt.getSource().equals(setStartNodeButton) && setStartNodeButton.isEnabled()) {
				// We have to distinguish if the user opened the popupMenu in
				// the table or on the map
				if (mainFrame.isClickInTable()) {
					mainFrame.setStartNode(mainFrame.getMapData().getNodeList()
							.get((long) mainFrame.getNodeTable().getValueAt(mainFrame.getSelectedRow(), 0)));
				} else { // Click on Map
					mainFrame.setStartNode(mainFrame.getMapAlgorithms()
							.getNearestNodeToMouseClick(mainFrame.getMouseListener().getClickOnMap()));
				}
			} else if(evt.getSource().equals(setEndNodeButton) && setEndNodeButton.isEnabled()) {
					mainFrame.getFinalWayList().clear();
				
				if (mainFrame.isClickInTable()) {
					mainFrame.setEndNode(mainFrame.getMapData().getNodeList()
							.get((long) mainFrame.getNodeTable().getValueAt(mainFrame.getSelectedRow(), 0)));
				} else {
					mainFrame.setEndNode(mainFrame.getMapAlgorithms()
							.getNearestNodeToMouseClick(mainFrame.getMouseListener().getClickOnMap()));
				}
				mainFrame.getCalculateAllPathsButton().setEnabled(true);
				mainFrame.getUtilizationSlider().setEnabled(false);
				
				if(mainFrame.getCompareMode().isSelected()){
					mainFrame.setCompareEndNode(mainFrame.getEndNode());
					mainFrame.setEndNode(null);
					mainFrame.calculateAllPaths();
					buildSelectNodesPopup();
				}
			}
		}else{
			if(evt.getSource().equals(setStartNodeButton) && setStartNodeButton.isEnabled()){
					mainFrame.disableUIElements();
					mainFrame.getConstructionMode().setSelected(true);
					mainFrame.getShowNodes().setEnabled(true);
					mainFrame.getShowWays().setEnabled(true);
					
						mainFrame.getFinalWayList().clear();
					
					mainFrame.setSelectedWay(null);
					mainFrame.getFinalWayBox().setSelected(false);
					
					constructedWayId = wayCounter++;
					
					constructedNode = mainFrame.getMapAlgorithms()
							.getNearestNodeToMouseClick(mainFrame.getMouseListener().getClickOnMap());
					
					//set the start point for the flag
					mainFrame.setStartNode(constructedNode);
					
					//open dialog, so that the user can set the way parameters
					createWayDialog();
					
					//If the user canceled the way dialog, we stop here
					if(constructionHasStarted){
						//Add this new way to the waylist of the found node
						mainFrame.getMapData().getNodeList().get(constructedNode.getId()).addWayId(constructedWayId);
						
						//Add the first node to the way node list of the way
						mainFrame.getMapData().getWayList().get(constructedWayId).addWayNode(constructedNode.getId());
						
						//Now that the construction has started we can enable some buttons
						setEndNodeButton.setEnabled(true);
						setNewNodeButton.setEnabled(true);
						abortConstructionButton.setEnabled(true);
						
						//We can't set a new start node while we already selected one
						setStartNodeButton.setEnabled(false);
						selectWayButton.setEnabled(false);
						deleteSelectedWayButton.setEnabled(false);
						
						//The user can only abort the construction via the popup menu button
						mainFrame.getConstructionMode().setEnabled(false);
						mainFrame.getCompareMode().setEnabled(false);
					}
			}else if(evt.getSource().equals(setEndNodeButton) && setEndNodeButton.isEnabled()){				
					constructedNode = mainFrame.getMapAlgorithms()
							.getNearestNodeToMouseClick(mainFrame.getMouseListener().getClickOnMap());
					
					//Add this new way to the waylist of the found node
					mainFrame.getMapData().getNodeList().get(constructedNode.getId()).addWayId(constructedWayId);
					
					//Add the new node as a way point to our constructed way
					mainFrame.getMapData().getWayList().get(constructedWayId).addWayNode(constructedNode.getId());

					//New way is finished
					mainFrame.setEndNode(constructedNode);												 
			}else if(evt.getSource().equals(setNewNodeButton) && setNewNodeButton.isEnabled()){	
					//get the calculated geoposition where the mouse was clicked
					constructedNode = mainFrame.getMouseListener().getClickOnMap();
					constructedNode.addWayId(constructedWayId);
					
					mainFrame.getMapData().getWayList().get(constructedWayId).addWayNode(constructedNode.getId());
					
					//Add the new node to the list of all nodes
					mainFrame.getMapData().getNodeList().put(constructedNode.getId(), constructedNode);
			}else if(evt.getSource().equals(abortConstructionButton) && abortConstructionButton.isEnabled()){
				//Abort construction and delete the unfinished way				
				for(Long id : mainFrame.getMapData().getWayList().get(constructedWayId).getWayNodes()){
					if(id!=mainFrame.getStartNode().getId()){
						mainFrame.getMapData().getNodeList().remove(id);
					}else{
						mainFrame.getStartNode().getWayIDs().remove(constructedWayId);
					}
				}
				mainFrame.getMapData().getWayList().remove(constructedWayId);				
				mainFrame.setStartNode(null);
				constructionHasEnded();
			}else if(evt.getSource().equals(selectWayButton) && selectWayButton.isEnabled()){

				if(mainFrame.getSelectedWay()!=null){
					mainFrame.setSelectedWay(null);
					selectWayButton.setText("Select way");
					deleteSelectedWayButton.setEnabled(false);
				}else{
					mainFrame.setSelectedWay(mainFrame.getMapData().getWayList().get(mainFrame.getMapAlgorithms().findNearestWayToMouseClick()));
					selectWayButton.setText("Unselect way");
					deleteSelectedWayButton.setEnabled(true);
				}
			}else if(evt.getSource().equals(deleteSelectedWayButton) && deleteSelectedWayButton.isEnabled()){
				deleteWay();
			}
		}
		
		// Draw a route after both points were set
		if (mainFrame.getStartNode() != null && mainFrame.getEndNode() != null && !mainFrame.getConstructionMode().isSelected()){
			mainFrame.startRouting();
		}
		
		if(constructionHasStarted && mainFrame.getEndNode()!=null){	
			//Show the new way as a calculated way
			mainFrame.getFinalWayList().put(constructedWayId, mainFrame.getMapData().getWayList().get(constructedWayId));
			
			mainFrame.getFinalWayBox().setSelected(true);
			constructionHasEnded();
		}
		
		((JComponent) evt.getSource()).setBackground(UIManager
				.getColor("MenuItem.background"));
		((JComponent) evt.getSource()).setForeground(UIManager
				.getColor("MenuItem.foreground"));

		currentPopup.setVisible(false);
		mainFrame.setClickInTable(false);
		mainFrame.getMapViewer().repaint();
	}
	
	public void deleteWay() {
		if(mainFrame.getSelectedWay().getWayNodes().size()>=2){
			for(Long nodeId : mainFrame.getSelectedWay().getWayNodes()){
				//Delete the selected way from the way list of the current node
				mainFrame.getMapData().getNodeList().get(nodeId).getWayIDs().remove(mainFrame.getSelectedWay().getId());
				
				//If a node has no more entries in its way list we delete the node
				if(mainFrame.getMapData().getNodeList().get(nodeId).getWayIDs().isEmpty()){
					mainFrame.getMapData().getNodeList().remove(nodeId);
				}
			}
			
			//Delete the way itself
			mainFrame.getMapData().getWayList().remove(mainFrame.getSelectedWay().getId());
			mainFrame.setSelectedWay(null);
			selectWayButton.setText("Select way");
			
			deleteSelectedWayButton.setEnabled(false);
		}
		
	}

	/**
	 * Create a dialog to set the different parameters of a new way.
	 */
	public void createWayDialog() {
		JDialog dialog = new JDialog(mainFrame.getFrame(),true);		
		dialog.setTitle("Set the way parameters");
		
		JPanel dialogPanel = new JPanel();
		GroupLayout dialogLayout = new GroupLayout(dialogPanel);
		dialogLayout.setAutoCreateGaps(true);
		dialogLayout.setAutoCreateContainerGaps(true);
		dialogPanel.setLayout(dialogLayout);
		
		
		//Label for the WayType
		JLabel wayTypeLabel = new JLabel("Set way type: ");
		
		//ComboBox for the WayType
		WayType[] wayTypeArray = new WayType[3];
		wayTypeArray[0] = WayType.HIGHWAY;
		wayTypeArray[1] = WayType.FOOTWAY;
		wayTypeArray[2] = WayType.HIGHWAY_WITH_FOOTWAY;
		
		JComboBox<WayType> wayTypeBox = new JComboBox<WayType>(wayTypeArray);
		wayTypeBox.setSelectedItem(null);
		wayTypeBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				wayType = (WayType) wayTypeBox.getSelectedItem();
			}
		});	
		
		//Label for the one way
		JLabel oneWayLabel = new JLabel("Set one way: ");
		
		//ComboBox for the one way setting
		Boolean[] oneWayArray = new Boolean[2];
		oneWayArray[0] = true;
		oneWayArray[1] = false;
		JComboBox<Boolean> oneWayBox = new JComboBox<Boolean>(oneWayArray);
		oneWayBox.setSelectedItem(null);
		oneWayBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {				
				oneWay = (boolean)oneWayBox.getSelectedItem();
			}
		});			
		
		//Label for the max speed
		JLabel maxSpeedLabel = new JLabel("Set the max. speed: ");
		
		//ComboBox for the one way setting
		Integer[] maxSpeedArray = new Integer[13];
		for(int i=1; i<14; i++){
			maxSpeedArray[i-1] = i*10;
		}
		JComboBox<Integer> maxSpeedBox = new JComboBox<Integer>(maxSpeedArray);
		maxSpeedBox.setSelectedItem(null);
		maxSpeedBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				maxSpeed = (Integer) maxSpeedBox.getSelectedItem();
			}
		});
		
		dialogLayout.setHorizontalGroup(dialogLayout.createSequentialGroup()
				.addGroup(dialogLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
				           .addComponent(wayTypeLabel)
				           .addComponent(oneWayLabel)
				           .addComponent(maxSpeedLabel))
				.addGroup(dialogLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
				           .addComponent(wayTypeBox)
				           .addComponent(oneWayBox)
				           .addComponent(maxSpeedBox))  
		);
		dialogLayout.setVerticalGroup(
				dialogLayout.createSequentialGroup()
				      .addGroup(dialogLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
				           .addComponent(wayTypeLabel)
				           .addComponent(wayTypeBox))
				      .addGroup(dialogLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
				    	   .addComponent(oneWayLabel)
				           .addComponent(oneWayBox))
				      .addGroup(dialogLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
					    	.addComponent(maxSpeedLabel)
					        .addComponent(maxSpeedBox))
		);	
		dialog.add(dialogPanel);

		JButton okButton = new JButton("Ok");
		okButton.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if(oneWayBox.getSelectedItem()!=null && wayTypeBox.getSelectedItem() != null && maxSpeedBox.getSelectedItem() != null){
					//In the end we add the new way to the list
					mainFrame.getMapData().getWayList().put(constructedWayId, new ExtendedWay(constructedWayId, wayType, null, oneWay, maxSpeed, -1, new ArrayList<Long>()));
					constructionHasStarted = true;
					
					dialog.setVisible(false);
					dialog.dispose();	
				}else{ //the user didn't set all fields
					mainFrame.createDialog("Please select a value for each field!","Error!");
				}
				
			}
		});
		
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				constructionHasEnded();				
				
				dialog.setVisible(false);
				dialog.dispose();
			}
			
		});

		JPanel buttonPanel = new JPanel();
		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);
		
		dialog.add(buttonPanel, BorderLayout.SOUTH);
		dialog.setLocationRelativeTo(mainFrame);
		dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		dialog.setSize(400, 200);
		dialog.setVisible(true);
	}

	/**
	 * After the construction has been finished or aborted we enable all ui elements again.
	 */
	public void constructionHasEnded(){
		mainFrame.setEndNode(null);
		mainFrame.setStartNode(null);

		wayType = null;
		oneWay = false;
		maxSpeed = 0;
		constructionHasStarted = false;
		
		mainFrame.getCompareMode().setEnabled(true);
		
		if(mainFrame.getCompareMode().isSelected()){
			mainFrame.getFinalWayBox().setEnabled(true);
			
			mainFrame.getConstructionMode().setSelected(false);
			mainFrame.getConstructionMode().setEnabled(true);
			buildSelectNodesPopup();
		}else{
			mainFrame.getConstructionMode().setSelected(false);
			mainFrame.enableUIElements();
		}
		
		selectWayButton.setEnabled(true);
		deleteSelectedWayButton.setEnabled(true);
		
		currentPopup.setVisible(false);
		mainFrame.setClickInTable(false);
		mainFrame.getMapViewer().repaint();
	}
	
	public void mouseEntered(MouseEvent evt) {
		((JComponent) evt.getSource()).setBackground(UIManager.getColor("MenuItem.selectionBackground"));
		((JComponent) evt.getSource()).setForeground(UIManager.getColor("MenuItem.selectionForeground"));
	}

	public void mouseExited(MouseEvent evt) {
		((JComponent) evt.getSource()).setBackground(UIManager.getColor("MenuItem.background"));
		((JComponent) evt.getSource()).setForeground(UIManager.getColor("MenuItem.foreground"));
	}

	public JPopupMenu getPopupMenu() {
		return currentPopup;
	}

	public JMenuItem getStartItem() {
		return setStartNodeButton;
	}

	public JMenuItem getEndItem() {
		return setEndNodeButton;
	}

	public JMenuItem getNewNodeItem() {
		return setNewNodeButton;
	}

	public long getConstructedWayId() {
		return constructedWayId;
	}
	
	public boolean getConstructionState(){
		return constructionHasStarted;
	}
	
	public JMenuItem getAbortConstructionButton() {
		return abortConstructionButton;
	}

	public void buildSelectNodesPopup(){
		currentPopup.removeAll();
		
		currentPopup.add(setStartNodeButton);
		currentPopup.add(setEndNodeButton);
		
		if(mainFrame.getCompareMode().isSelected()){
			setStartNodeButton.setEnabled(false);
		}else{
			setStartNodeButton.setEnabled(true);
		}
		
		if(mainFrame.getCompareEndNode()==null){
			setEndNodeButton.setEnabled(true);
		}else{
			setEndNodeButton.setEnabled(false);
		}
	}
	
	public void buildConstructionPopup(){
		currentPopup.removeAll();
		
		currentPopup.add(setNewNodeButton);
		currentPopup.addSeparator();
		currentPopup.add(setStartNodeButton);
		currentPopup.add(setEndNodeButton);
		currentPopup.addSeparator();
		currentPopup.add(selectWayButton);
		currentPopup.add(deleteSelectedWayButton);
		currentPopup.add(abortConstructionButton);	
		
		//When a new construction has started, the user has to define
		//a start node before he can proceed
		setStartNodeButton.setEnabled(true);
		setEndNodeButton.setEnabled(false);
		setNewNodeButton.setEnabled(false);
		abortConstructionButton.setEnabled(false);
		deleteSelectedWayButton.setEnabled(false);
	}

	public JMenuItem getSelectWayButton() {
		return selectWayButton;
	}
	
}
