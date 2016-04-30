package map.objects;
import java.util.ArrayList;
import java.util.List;

import org.jxmapviewer.viewer.GeoPosition;


public class ExtendedGeoPosition extends GeoPosition{
	
	private long id;
	
	private List<Long> wayIds;
	
	private ExtendedGeoPosition previousNode;
	
	private double distanceToThisNode = Double.POSITIVE_INFINITY;
	private double timeToThisNode = Double.POSITIVE_INFINITY;
	
	private double compareModeDistance = Double.POSITIVE_INFINITY;
	private double compareModeTime = Double.POSITIVE_INFINITY;

	private boolean isVisited = false;
	
	public ExtendedGeoPosition(long id, double latitude, double longitude){
		super(latitude,longitude);
		this.id = id;
		
		wayIds = new ArrayList<Long>();
	}

	public long getId() {
		return id;
	}

	public void addWayId(long wayId) {
		wayIds.add(wayId);
	}
	
	public List<Long> getWayIDs(){
		return wayIds;
	}

	public ExtendedGeoPosition getPreviousNode() {
		return previousNode;
	}

	public void setPreviousNode(ExtendedGeoPosition previousNode) {
		this.previousNode = previousNode;
	}

	public double getDistanceToThisNode() {
		return distanceToThisNode;
	}

	public void setDistanceToThisNode(double distanceToThisNode) {
		this.distanceToThisNode = distanceToThisNode;
	}

	public boolean isVisited() {
		return isVisited;
	}

	public void setVisited(boolean isVisited) {
		this.isVisited = isVisited;
	}
	
	public double getTimeToThisNode() {
		return timeToThisNode;
	}

	public void setTimeToThisNode(double timeToThisNode) {
		this.timeToThisNode = timeToThisNode;
	}

	public double getCompareModeDistance() {
		return compareModeDistance;
	}

	public void setCompareModeDistance(double compareModeDistance) {
		this.compareModeDistance = compareModeDistance;
	}

	public double getCompareModeTime() {
		return compareModeTime;
	}

	public void setCompareModeTime(double compareModeTime) {
		this.compareModeTime = compareModeTime;
	}
}
