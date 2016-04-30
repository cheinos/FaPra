package map;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.PriorityQueue;

import javax.swing.JLabel;
import javax.swing.SwingWorker;

import main.MainFrame;
import map.objects.ExtendedGeoPosition;
import map.objects.ExtendedWay;
import map.objects.WayType;

/**
 * This class contains many different algorithms that are used to work 
 * with the OSM maps. You can find an implementation of Dijkstra's algorithm,
 * calculate the length between two {@link ExtendedGeoPosition} or find the nearest
 * node or way to a mouse click position. 
 * @author Florian
 *
 */
public class MapAlgorithms extends SwingWorker<Void, Void> {

	private MainFrame mainFrame;
	
	private int highestIncrease = 0;
	private int highestDecrease = 0;
	
	private int incrementValue = 0;
	private int decreaseValue = 0;
	
	private boolean compareMode = false;
	
	//how many nodes benefit or suffer from the changes made in the construction mode
	private int numberOfBenefitNodes = 0;
	private int numberOfSufferingNodes = 0;

	private int earthRadius = 6371000;
	private double dLat;
	private double dLng;
	private double a;
	private double c;

	private double tempDistance;
	private double newDistance;
	private double oldDistance;	
	private double totalDistance;
	
	private double newTime;
	
	private long wayID;
	private int maxSpeed;

	private int highestUtilizationRate = 0;
	
	private DecimalFormat distanceFormat = new DecimalFormat("#0.00");
	
	private ExtendedWay currentWay;
	private ExtendedGeoPosition currentNode ;
	private ExtendedGeoPosition nearestNodeToClick;
	
	private ExtendedGeoPosition secondNearestNodeToClick;
	private ExtendedGeoPosition previousNode;
	private ExtendedGeoPosition nextNode;
	
	private List<ExtendedGeoPosition> tempList = new ArrayList<ExtendedGeoPosition>();
	private List<ExtendedGeoPosition> neighborNodes = new ArrayList<ExtendedGeoPosition>();
	
	private List<Long> finalWayNodes = new ArrayList<Long>();
	
	private PriorityQueue<ExtendedGeoPosition> unvisitedNodesForDistance = new PriorityQueue<ExtendedGeoPosition>(
			new Comparator<ExtendedGeoPosition>() {

				@Override
				public int compare(ExtendedGeoPosition gp1,
						ExtendedGeoPosition gp2) {
					if(gp1.getDistanceToThisNode() > gp2.getDistanceToThisNode()){
						return 1;
					}else{
						return -1;
					}
				}
			});
	
	private PriorityQueue<ExtendedGeoPosition> unvisitedNodesForTime = new PriorityQueue<ExtendedGeoPosition>(
			new Comparator<ExtendedGeoPosition>() {

				@Override
				public int compare(ExtendedGeoPosition gp1,
						ExtendedGeoPosition gp2) {
					if(gp1.getTimeToThisNode() > gp2.getTimeToThisNode()){
						return 1;
					}else{
						return -1;
					}
				}
			});

	public MapAlgorithms(MainFrame mainFrame) {
		this.mainFrame = mainFrame;
	}

	/**
	 * Calculates the distance between a given GeoPosition and all other
	 * Geopositions that are available
	 * 
	 * @param geoPosition
	 *            The GeoPosition that was calculated from the coordinates of the mouse click.
	 * @return The nearest GeoPosition related to the given GeoPosition
	 */
	public ExtendedGeoPosition getNearestNodeToMouseClick(
			ExtendedGeoPosition geoPosition) {
		nearestNodeToClick = null;
		oldDistance = 0;

		for (ExtendedGeoPosition gp : mainFrame.getMapData()
				.getNodeList().values()) {
			newDistance = haversineFormula(gp, geoPosition);
			if (oldDistance == 0 || newDistance <= oldDistance) {
				nearestNodeToClick = gp;
				oldDistance = newDistance;
			}
		}
		
		System.out.println("ID of found node: "+nearestNodeToClick.getId());
		
		return nearestNodeToClick;
	}

	/**
	 * Implementation of the Dijkstra Algorithm which calculates the shortest path between two given nodes.
	 * 
	 * @param startNode
	 * @param endNode
	 * @return A list which contains all nodes on the path between the given start and end node. 
	 */
	public List<Long> dijkstraAlgorithm(ExtendedGeoPosition startNode, ExtendedGeoPosition endNode) {

		//Necessary preparations for the algorithm
		mainFrame.getFinalWayList().clear();
		unvisitedNodesForDistance.clear();
		unvisitedNodesForTime.clear();
		finalWayNodes.clear();
		
		for (ExtendedGeoPosition gp : mainFrame.getMapData().getNodeList().values()) {
			//Store the 'old' values for the compare mode
			if(!mainFrame.getCompareWayList().isEmpty()){
				gp.setCompareModeDistance(gp.getDistanceToThisNode());
				gp.setCompareModeTime(gp.getTimeToThisNode());
			}
			gp.setDistanceToThisNode(Double.POSITIVE_INFINITY);
			gp.setTimeToThisNode(Double.POSITIVE_INFINITY);
			gp.setPreviousNode(null);
			gp.setVisited(false);
		}
		startNode.setDistanceToThisNode(0);
		startNode.setTimeToThisNode(0);
		
		if(mainFrame.getShortestWay().isSelected()){
			unvisitedNodesForDistance.add(startNode);
		}else{
			unvisitedNodesForTime.add(startNode);
		}
		
		 while(!unvisitedNodesForDistance.isEmpty() || !unvisitedNodesForTime.isEmpty()){			
			if(mainFrame.getShortestWay().isSelected()){
				 currentNode = unvisitedNodesForDistance.poll();
				 unvisitedNodesForDistance.addAll(getNeighborNodes(currentNode));
			}else{
				currentNode = unvisitedNodesForTime.poll();
				unvisitedNodesForTime.addAll(getNeighborNodes(currentNode));
			}		 	
		 	currentNode.setVisited(true);
		 	
		 	if(!mainFrame.isAllPaths()){ //If the variable isn't set, we calculate only one path from a given start to a given end node
		 		if(currentNode.getId() == endNode.getId()){//Found the end node
			 		System.out.println("Found a possible route!");
					 
					 finalWayNodes.add(endNode.getId());
					 ExtendedGeoPosition nextNode = mainFrame.getEndNode();

					 mainFrame.setDistanceLabel("Distance: "+distanceFormat.format(nextNode.getDistanceToThisNode()/1000)+"km");
					 
					 double totalSecs = nextNode.getTimeToThisNode();					 					 
					 int hours = (int) (totalSecs / 3600);
					 int minutes = (int) ((totalSecs % 3600) / 60);
					 int seconds = (int) (totalSecs % 60);
					 String timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds);
					 
					 mainFrame.setTimeLabel("Time: "+timeString+"h");
					 
					 while(nextNode.getId()!=startNode.getId()){ //go the path recursively backwards from the end to the start node
						 nextNode = mainFrame.getMapData()
									.getNodeList().get(finalWayNodes.get(finalWayNodes.size()-1)).getPreviousNode();
						 finalWayNodes.add(nextNode.getId());
					 }
			 		break;
			 	}
		 	}
		 			 	
		 }
		 
		 return finalWayNodes;
	}
	
	
	/**
	 * Adds the node before the current node to a temporarily list.
	 * @param positionInList The position of the current node in the way node list of the current way
	 */
	public void addPreviousNode(int positionInList){
		if (positionInList - 1 >= 0){// special case: the first node has no previous node
			if(!mainFrame.getMapData()
					.getNodeList().get(
					currentWay.getWayNodes().get(positionInList - 1)).isVisited()){//Only add nodes to the neighbor list that are UNVISITED
								tempList.add(mainFrame.getMapData()
										.getNodeList().get(
						currentWay.getWayNodes().get(positionInList - 1)));	
			}
		}
	}
	
	/**
	 * Adds the node after the current node to a temporarily list.
	 * @param positionInList The position of the current node in the way node list of the current way
	 */
	public void addNextNode(int positionInList){
		if (positionInList + 1 < currentWay.getWayNodes().size()) { //special case: the last node has no next node
			if(!mainFrame.getMapData()
					.getNodeList().get(
					currentWay.getWayNodes().get(positionInList + 1)).isVisited()){
				tempList.add(mainFrame.getMapData()
						.getNodeList().get(
						currentWay.getWayNodes().get(positionInList + 1)));
			}
		}
	}

	/**
	 * Get the neigbhor nodes for a given node.
	 * 
	 * @param currentNode
	 * @return
	 */
	public List<ExtendedGeoPosition> getNeighborNodes(
			ExtendedGeoPosition currentNode) {
		tempList.clear();
		neighborNodes.clear();
		
		for (Long wayID : currentNode.getWayIDs()) {
			currentWay = mainFrame.getMapData().getWayList().get(wayID);
			
			// Get the position of the current node on each way, so that we can get
			// its neighbor nodes
			int positionInList = currentWay.getWayNodes().indexOf(
					currentNode.getId());
				
			if(mainFrame.getByCar().isSelected() && (currentWay.getType().equals(WayType.HIGHWAY) || currentWay.getType().equals(WayType.HIGHWAY_WITH_FOOTWAY))){
				if(!mainFrame.isAllPaths()){
					
					//Check for one ways. If the current node is NOT a node of a one way,
					//we consider all his neighbor nodes from this way and all the other
					//ways around it
					if(!currentWay.isOneWay()){
						// Previous neighbor node
						addPreviousNode(positionInList);
					}
					
					//we found a one way and consider only the next node on this way
					addNextNode(positionInList);
				}else{ 
					//If we calculate all paths to a goal, we want to get the routes from all nodes to the goal 
					//and not the routes from the goal to all nodes. Thats why we have to handle all the one ways
					//in the other direction					
					addPreviousNode(positionInList);
					
					if(!currentWay.isOneWay()){
						addNextNode(positionInList);
					}
				}	
			}else if(mainFrame.getByFoot().isSelected() && (currentWay.getType().equals(WayType.FOOTWAY) || currentWay.getType().equals(WayType.HIGHWAY_WITH_FOOTWAY))){
				//Pedrestrians can use one ways in each direction if a sidewalk exists
				addPreviousNode(positionInList);
				addNextNode(positionInList);
			}
		}
		
		// Calculate the distances between the start node an its neighbor nodes
		// Big Improvement: Instead of adding ALL neighbors, we only add neighbors, which have a smaller distance / better time
		// Compare old: 569782 <--> new: 143 unvisited neighbor nodes for the same route
		for (ExtendedGeoPosition gp : tempList) {
			wayID = findMatchingWayID(currentNode, gp);
			maxSpeed = mainFrame.getMapData().getWayList().get(wayID).getMaxSpeed();
			
			newDistance = haversineFormula(currentNode, gp);
			totalDistance = currentNode.getDistanceToThisNode()+newDistance;
								
			if(mainFrame.getByFoot().isSelected()){
				newTime = (newDistance/((double)(1/3.6)*5))+currentNode.getTimeToThisNode(); //maxSpeed of a Pedestrian is 5
				
				//For Pedestrians the fastest way is also the shortest way
				if(totalDistance < gp.getDistanceToThisNode()){ //only set the new distance if it is smaller then the old distance to this node
					gp.setDistanceToThisNode(totalDistance);
					gp.setTimeToThisNode(newTime);
					gp.setPreviousNode(currentNode);
					neighborNodes.add(gp);
				}
			}else{
				newTime = (newDistance/((double)(1/3.6)*maxSpeed))+currentNode.getTimeToThisNode();
				
				if(mainFrame.getShortestWay().isSelected()){
					if(totalDistance < gp.getDistanceToThisNode()){ //only set the new distance if it is smaller then the old distance to this node
						gp.setDistanceToThisNode(totalDistance);
						gp.setTimeToThisNode(newTime);
						gp.setPreviousNode(currentNode);
						neighborNodes.add(gp);
					}	
				}else{ //fastest way			
					if(newTime < gp.getTimeToThisNode()){
						gp.setDistanceToThisNode(totalDistance);
						gp.setTimeToThisNode(newTime);
						gp.setPreviousNode(currentNode);
						neighborNodes.add(gp);
					}
				}
			}
		}
		
		return neighborNodes;
	}
	
	/**
	 * Each route between a start and end node is split into small ways that only contain two nodes.
	 * By this, we can increase a counter every time such a small way is used. In the end, we can draw
	 * the width of each way based on its counter.
	 */
	public void calculateStreetUtilizationRate(ExtendedGeoPosition endNode) {
		System.out.println("Start the utilization counting...");
		mainFrame.getBottomLabel().setText("Start the utilization counting...");
		
		List<ExtendedGeoPosition> tempList = new ArrayList<ExtendedGeoPosition>(mainFrame.getMapData()
				.getNodeList().values());
		//List<ExtendedGeoPosition> halfSize = tempList.subList(0, tempList.size()/2);
		List<Long> wayNodes = new ArrayList<Long>();
		
		for (ExtendedGeoPosition gp : tempList) {
//				mainFrame.setProgress(tempList.indexOf(gp));
			
			if (gp.getPreviousNode() != null) {
				ExtendedGeoPosition nextNode = gp;
				
				while (nextNode.getId() != endNode.getId()) {
					
						//Path not included in the list
						if (!mainFrame.getFinalWayList().containsKey(nextNode.getId())) {
							wayNodes = new ArrayList<Long>();
							wayNodes.add(nextNode.getId());
							wayNodes.add(nextNode.getPreviousNode().getId());
							
							//put a new Way into the list, that contains only two Nodes and a counter
							mainFrame.getFinalWayList().put(nextNode.getId(), new ExtendedWay(nextNode.getId(),1,
									wayNodes));
							
							//go recursively over all nodes 
							nextNode = nextNode.getPreviousNode();
						} else {//the Way is already in our list, so we only have to increase the counter
							
							int newUtilizationRate = mainFrame.getFinalWayList().get(
									nextNode.getId()).getUtilizationRate() + 1;
							if (newUtilizationRate > highestUtilizationRate)
								highestUtilizationRate = newUtilizationRate;

							mainFrame.getFinalWayList().get(nextNode.getId()).setUtilizationRate(
									newUtilizationRate);
							nextNode = nextNode.getPreviousNode();
						}
				}
			}
		}
		
		for (ExtendedWay w : mainFrame.getFinalWayList().values()) {
			for (float i = 0.001f; i < 0.02; i = i + 0.001f) {
				if (w.getUtilizationRate() > highestUtilizationRate * i) {
					float strokeWidth = (float) (i * 1000 / 1.1);
					if(strokeWidth > 1){
						w.setStrokeWidth(strokeWidth);
					}
				}
			}
		}
		mainFrame.getBottomLabel().setText("Finished the utilization counting!");
			
		System.out.println("Finished the utilization counting!");
		System.out.println("Highest utilization rate: "+highestUtilizationRate);
		
	}

	/**
	 * Calculates the length of a way based on a given set of nodes
	 * 
	 * @param nodes
	 * @return the length of the way in meters
	 */
	public double calculateWayLength(List<Long> nodes) {
		double wayLength = 0;

		for (int i = 0; i < nodes.size() - 1; i++) {
			wayLength += haversineFormula(
					mainFrame.getMapData()
					.getNodeList().get(nodes.get(i)), mainFrame.getMapData()
					.getNodeList().get(nodes.get(i + 1)));
		}

		return wayLength;
	}

	/**
	 * The haversine formula is an equation important in navigation, giving
	 * great-circle distances between two points on a sphere from their
	 * longitudes and latitudes.
	 * 
	 * @param checkPosition
	 * @param geoPosition
	 * @return
	 */
	public double haversineFormula(ExtendedGeoPosition checkPosition,
			ExtendedGeoPosition geoPosition) {
		dLat = Math.toRadians(checkPosition.getLatitude()
				- geoPosition.getLatitude());
		dLng = Math.toRadians(checkPosition.getLongitude()
				- geoPosition.getLongitude());
		a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
				+ Math.cos(Math.toRadians(geoPosition.getLatitude()))
				* Math.cos(Math.toRadians(checkPosition.getLatitude()))
				* Math.sin(dLng / 2) * Math.sin(dLng / 2);
		c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		tempDistance = (earthRadius * c);

		return tempDistance;
	}
	
	/**
	 * If the user enabled the construction mode, it's possible to select ways on the map.
	 * This method looks for a second node that is near the clicked position. After such a node is
	 * found, the wayID lists of both nodes a checked for an id that is contained in both lists.
	 * The found id indicates that both nodes lie on the same way.
	 * 
	 * @return The wayID of the found way, that is the nearest to the mouse click on the map
	 */
	public long findNearestWayToMouseClick(){
		ExtendedGeoPosition clickPositionOnMap = mainFrame.getMouseListener().getClickOnMap();
		nearestNodeToClick = getNearestNodeToMouseClick(clickPositionOnMap);
		
		double distance = Double.POSITIVE_INFINITY;
		double distanceBetweenNodes;
		
		for (Long wayID : nearestNodeToClick.getWayIDs()) {
			ExtendedWay currentWay = mainFrame.getMapData().getWayList().get(wayID);
			int positionInList = currentWay.getWayNodes().indexOf(
					nearestNodeToClick.getId());
			
			//Get previous node on the way list of the current node
			if (positionInList - 1 >= 0){
				 previousNode = mainFrame.getMapData()
				.getNodeList().get(
						currentWay.getWayNodes().get(positionInList - 1));
				 
					distanceBetweenNodes = mainFrame.getMapAlgorithms().haversineFormula(previousNode, clickPositionOnMap);
					
					//Compare distances
					if(distanceBetweenNodes < distance){
						distance = distanceBetweenNodes;
						secondNearestNodeToClick = previousNode;
					}
			}
			
			//Get next node on the way list of the current node
			if (positionInList + 1 < currentWay.getWayNodes().size()) { 
				nextNode = mainFrame.getMapData()
				.getNodeList().get(
				currentWay.getWayNodes().get(positionInList + 1));
				
				distanceBetweenNodes = mainFrame.getMapAlgorithms().haversineFormula(nextNode, clickPositionOnMap);
				if(distanceBetweenNodes < distance){
					distance = distanceBetweenNodes;
					secondNearestNodeToClick = nextNode;
				}
			}
		}
		
		return findMatchingWayID(nearestNodeToClick,secondNearestNodeToClick);
	}
	
	/**
	 * Search for a match in both wayID lists of the selected nodes
	 * 
	 * @param firstNode
	 * @param secondNode
	 * @return
	 */
	public long findMatchingWayID(ExtendedGeoPosition firstNode, ExtendedGeoPosition secondNode){
		long foundWayID = 0;
		
		for(Long firstWayID: firstNode.getWayIDs()){
			for(Long secondWayID : secondNode.getWayIDs()){
				
				//An id was found that is contained in both lists
				if(firstWayID.compareTo(secondWayID)==0){
					foundWayID = firstWayID;
					break;
				}
			}
		}
		
		return foundWayID;
	}

	@Override
	protected Void doInBackground() {
		mainFrame.disableUIElements();
		
		if(mainFrame.isCalculateAllPaths()){ //calculate all ways
			if(mainFrame.getCompareMode().isSelected()){
				dijkstraAlgorithm(mainFrame.getCompareEndNode(), null);
				calculateStreetUtilizationRate(mainFrame.getCompareEndNode());
			}else{
				dijkstraAlgorithm(mainFrame.getEndNode(), null);
				calculateStreetUtilizationRate(mainFrame.getEndNode());
			}
		}else{ //calculate only one way
			dijkstraAlgorithm(mainFrame.getStartNode(),
					mainFrame.getEndNode());
		}

		return null;
	}
	
	@Override
	public void done(){
		if(!mainFrame.isCalculateAllPaths()){
			if (!finalWayNodes.isEmpty()) { // A way from start to end node was found
				mainFrame.getFinalWayList().put((long) -1, new ExtendedWay(-1,1, finalWayNodes));
			} else { // No way was found
				System.out.println("No route could be found!");
				mainFrame.setDistanceLabel("Distance to goal: ---");
				mainFrame.setTimeLabel("Time to goal: ---");
				mainFrame.createDialog("No route could be found!","Error!");
			}
		}else{
			mainFrame.getUtilizationSlider().setMaximum(highestUtilizationRate/400);
			mainFrame.getUtilizationSlider().setValue(0);
			
			Hashtable<Integer, JLabel> labels =
	                new Hashtable<Integer, JLabel>();
	        labels.put(0, new JLabel("High"));
	        labels.put(mainFrame.getUtilizationSlider().getMaximum(), new JLabel("Low"));
	        
	        mainFrame.getUtilizationSlider().setLabelTable(labels);
			mainFrame.getUtilizationSlider().setEnabled(true);		
			
			if(highestUtilizationRate==0)
				mainFrame.createDialog("This end point is not reachable!", "Error!");
		}
		
		if(mainFrame.getCompareMode().isSelected()){
			
			//UI stuff
			mainFrame.getConstructionMode().setEnabled(true);			
			mainFrame.getCompareTwoWaySetsButton().setEnabled(true);			
			mainFrame.getShowWays().setEnabled(true);
			mainFrame.getShowNodes().setEnabled(true);
			mainFrame.getFinalWayBox().setEnabled(true);
			
			//reset variables
			incrementValue = 0;
			highestIncrease = 0;
			decreaseValue = 0;
			highestDecrease = 0;
			
			if(mainFrame.getCompareWayList().isEmpty()){
				mainFrame.setCompareWayList(mainFrame.getFinalWayList());
			}else{ //comparewaylist and finalwaylist are set and can now be used for a comparison
				mainFrame.getConstructionMode().setSelected(false);
				
				//calculate the highest increase and decrease between the two lists
				for(ExtendedWay w: mainFrame.getFinalWayList().values()){
					if(mainFrame.getCompareWayList().get(w.getId()) != null){
						incrementValue = w.getUtilizationRate() - mainFrame.getCompareWayList().get(w.getId()).getUtilizationRate();
						decreaseValue = mainFrame.getCompareWayList().get(w.getId()).getUtilizationRate() - w.getUtilizationRate();
						
						if(incrementValue > highestIncrease){
							highestIncrease = incrementValue;
						}
						if(decreaseValue > highestDecrease){
							highestDecrease = decreaseValue;
						}
					}
				}
				System.out.println();
				System.out.println("Highest Increase: "+highestIncrease);
				System.out.println("Highest Decrease: "+highestDecrease);
				

				numberOfBenefitNodes = 0;
				numberOfSufferingNodes = 0;
				
				boolean shortestWayMode = mainFrame.getShortestWay().isSelected();
				
				for(ExtendedGeoPosition gp : mainFrame.getMapData().getNodeList().values()){
					if(shortestWayMode){
						if(gp.getDistanceToThisNode()!=Double.POSITIVE_INFINITY){
							if(gp.getDistanceToThisNode() < gp.getCompareModeDistance()){
								numberOfBenefitNodes++;
							}else if(gp.getDistanceToThisNode() > gp.getCompareModeDistance()){
								numberOfSufferingNodes++;
							}
						}
					}else{
						if(gp.getTimeToThisNode() !=Double.POSITIVE_INFINITY){
							if(gp.getTimeToThisNode() < gp.getCompareModeTime()){
								numberOfBenefitNodes++;
							}else if(gp.getTimeToThisNode() > gp.getCompareModeTime()){
								numberOfSufferingNodes++;
							}
						}
					}
				}
				System.out.println();
				System.out.println("Number of nodes that profit from your changes: "+numberOfBenefitNodes);
				System.out.println("Number of nodes that do not profit from your changes: "+numberOfSufferingNodes);
				
				//based on the highest values, we can now set the colors
				for(ExtendedWay w: mainFrame.getFinalWayList().values()){
					if(mainFrame.getCompareWayList().get(w.getId()) != null){
						if(w.getUtilizationRate() > mainFrame.getCompareWayList().get(w.getId()).getUtilizationRate()){
							incrementValue = w.getUtilizationRate() - mainFrame.getCompareWayList().get(w.getId()).getUtilizationRate();
							
							setIncreaseColor(w);
						}else if(w.getUtilizationRate() < mainFrame.getCompareWayList().get(w.getId()).getUtilizationRate()){
							decreaseValue = mainFrame.getCompareWayList().get(w.getId()).getUtilizationRate() - w.getUtilizationRate();								
							
							setDecreaseColor(w);								
						}							
					}else{ //we have to handle all ways separately, that were built by the user
						setIncreaseColor(w);
					}
				}
				
				mainFrame.setCompareWayList(mainFrame.getFinalWayList());
				mainFrame.setEndNode(null);
				compareMode = true;
			}
		}else{
			mainFrame.enableUIElements();
			mainFrame.getCalculateAllPathsButton().setEnabled(true);
		}
		
		//Activate UI elements
		mainFrame.setCalculateAllPaths(false);
		mainFrame.getFinalWayBox().setSelected(true);
		mainFrame.enableCheckBoxes();
		
		mainFrame.getProgressBar().setValue(mainFrame.getProgressBar().getMaximum());
		mainFrame.getProgressBar().setString("100%");
		
		mainFrame.getMapViewer().repaint();
		
		if(compareMode){
			if(highestIncrease == 0 && highestDecrease == 0){
				mainFrame.createDialog("No changes could be detected!", "Warning!");
			}else{
				mainFrame.createConstructionRatingDialog(numberOfBenefitNodes, numberOfSufferingNodes);
				compareMode = false;
			}
		}
	}
	
	/**
	 * Based on the highest increase value and the utilizationRate of a {@link ExtendedWay}, we can set the color 
	 * for the way w
	 * @param w The {@link ExtendedWay} whose paint color is set
	 */
	public void setIncreaseColor(ExtendedWay w){
		if(w.getUtilizationRate() > 0 && w.getUtilizationRate() < (int)highestIncrease*1/5){
			w.setWayColor(new Color(255,255,102));
		}else if(w.getUtilizationRate() > (int)highestIncrease*1/5 && w.getUtilizationRate() < (int)highestIncrease*2/5){
			w.setWayColor(new Color(255,153,51));
		}else if(w.getUtilizationRate() > (int)highestIncrease*2/5 && w.getUtilizationRate() < (int)highestIncrease*3/5){
			w.setWayColor(new Color(255,0,0));
		}else if(w.getUtilizationRate() > (int)highestIncrease*3/5 && w.getUtilizationRate() < (int)highestIncrease*4/5){
			w.setWayColor(new Color(204,0,0));
		}else if(w.getUtilizationRate() > (int)highestIncrease*4/5){
			w.setWayColor(new Color(153,0,0));
		}
	}
	
	/**
	 * Based on the highest decrease value and the utilizationRate of a {@link ExtendedWay}, we can set the color 
	 * for the way w
	 * @param w The {@link ExtendedWay} whose paint color is set
	 */
	public void setDecreaseColor(ExtendedWay w){
		if(decreaseValue > 0 && decreaseValue < (int)highestDecrease*1/5){
			w.setWayColor(new Color(178,255,102));
		}else if(decreaseValue > (int)highestDecrease*1/5 && decreaseValue < (int)highestDecrease*2/5){
			w.setWayColor(new Color(153,255,51));
		}else if(decreaseValue > (int)highestDecrease*2/5 && decreaseValue < (int)highestDecrease*3/5){
			w.setWayColor(new Color(128,255,0));
		}else if(decreaseValue > (int)highestDecrease*3/5 && decreaseValue < (int)highestDecrease*4/5){
			w.setWayColor(new Color(102,204,0));
		}else if(decreaseValue > (int)highestDecrease*4/5){
			w.setWayColor(new Color(0,153,0));
		}
	}
}
