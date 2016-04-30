package map.objects;
import java.awt.Color;
import java.util.List;

public class ExtendedWay{
	
	private long id;	
	
	private WayType type;
	private boolean oneWay;
	private int maxSpeed;
	private String wayName;
	
	private List<Long> wayNodes;
	private double wayLength;
	
	private int utilizationRate;
	private Color wayColor = Color.BLUE;
	private float strokeWidth = 2f;

	public ExtendedWay(long id, WayType type, String wayName, boolean oneWay, int maxSpeed, double wayLength, List<Long> wayNodes){
		this.id = id;
		this.type = type;
		this.wayName = wayName;
		this.oneWay = oneWay;
		this.maxSpeed = maxSpeed;
		this.wayLength = wayLength;
		this.wayNodes = wayNodes;
	}
	
	public ExtendedWay(long id, int utilizationRate, List<Long> wayNodes){
		this.id = id;
		this.utilizationRate = utilizationRate;
		this.wayNodes = wayNodes;
	}
	
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public WayType getType() {
		return type;
	}

	public void setType(WayType type) {
		this.type = type;
	}

	public List<Long> getWayNodes() {
		return wayNodes;
	}

	public double getWayLength() {
		return wayLength;
	}

	public void setWayLength(double wayLength) {
		this.wayLength = wayLength;
	}

	public String getWayName() {
		return wayName;
	}	
	
	public int getUtilizationRate() {
		return utilizationRate;
	}

	public void setUtilizationRate(int utilizationRate) {
		this.utilizationRate = utilizationRate;
	}

	public float getStrokeWidth() {
		return strokeWidth;
	}

	public void setStrokeWidth(float strokeWidth) {
		this.strokeWidth = strokeWidth;
	}

	public boolean isOneWay() {
		return oneWay;
	}

	public void setOneWay(boolean oneWay) {
		this.oneWay = oneWay;
	}
	
	public void addWayNode(long nodeId){
		wayNodes.add(nodeId);
	}

	public int getMaxSpeed() {
		return maxSpeed;
	}

	public void setMaxSpeed(int maxSpeed) {
		this.maxSpeed = maxSpeed;
	}

	public Color getWayColor() {
		return wayColor;
	}

	public void setWayColor(Color wayColor) {
		this.wayColor = wayColor;
	}
}
