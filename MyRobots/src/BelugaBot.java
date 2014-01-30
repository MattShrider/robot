import robocode.AdvancedRobot;
import robocode.HitByBulletEvent;
import robocode.RateControlRobot;
import robocode.RobocodeFileWriter;
import robocode.RobotDeathEvent;
import robocode.Rules;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;


public class BelugaBot extends AdvancedRobot {
	
	final private static int LONGDISTANCE = 800;
	final private static int MEDIUMDISTANCE = 200;
	final private static int SHORTDISTANCE = 100;
	
	final private static double ACCURACYTHRESHOLD = 50;
	
	String name;
	double energy;
	double bearing;
	double distance;
	double heading;
	double velocity;
	
	Enemy chargingAt = null;
	
	/** 
	 * Private class for representing enemies on the field.  Construction based on the onScannedRobot event.
	 */
	private class Enemy implements Comparable {
		String name;
		double energy;
		double bearing;
		double distance;
		double heading;
		double velocity;
		Point location;
		double accuracy;
		
		public Enemy(String name, double energy, double bearing, double distance, double heading, double velocity, Point location ) {
			this.name = name;
			this.energy = energy;
			this.bearing = bearing;
			this.distance = distance;
			this.heading = heading;
			this.velocity = velocity;
			this.location = location;
		}
		
		public void update(double en, double be, double dis, double he, double vel, Point loc) {
			this.energy = en;
			this.bearing = be;
			this.distance = dis;
			this.heading = he;
			this.velocity = vel;
			this.location = loc;
		}
		
		/** Calculate the accuracy level of taking a shot.  Higher accuracy is better.
		 * This method will be used as the comparable element in our shoot at list.
		 * If the risk of all bots is above a threshold, don't take the shot.
		 */
		public void calculateAccuracy(){
			
			
		}

		@Override
		public int compareTo(Object o) {
			Enemy other = (Enemy) o;
			double ourVelocity = Math.abs(this.velocity);
			double theirVelocity = Math.abs(other.velocity);
			
			//THe following statements are multiple sorting methods, starting
			//  from the top of the list.
			
			//if a bot is disabled, just kill it
			if (this.energy == 0)
				return -1;
			
			//If an enemy is VERY close, shoot at it first
			if (this.distance < SHORTDISTANCE && other.distance < SHORTDISTANCE){
				if( this.distance < other.distance)
					return -1;
				else
					return 1;
			}
			
			//Shoot at the closest enemy if more than one enemy is stopped.
			if (this.velocity == 0 && other.velocity == 0){
				if (this.distance <= other.distance)
					return -1;
				else
					return 1;
			}
			//stopped enemies have the next highest priority
			if (this.velocity == 0)
				return -1;
			if (other.velocity == 0)
				return 1;
			//Next, sort by distance to enemies if the distance is below a threshold.
			if (this.distance < other.distance && this.distance < MEDIUMDISTANCE)
				return -1;
			if (this.distance > other.distance && other.distance < MEDIUMDISTANCE)
				return 1;
			
			//finally, sort by velocities.
			if(ourVelocity > theirVelocity) {
				return 1;
			} else if(ourVelocity < theirVelocity) {
				return -1;
			}
			
			return 0;
		}
		
		public String toString() {
			return this.name;
		}
	}
	
	double arenaRadius;
	double maxMoveAmount;
	ArrayList<Point> targetPositions;
	boolean clockwise;
	int turncounter = 0;
	HashMap<String, Enemy> enemies = new HashMap<String, Enemy>();
	
	public void run() {
		setBodyColor(Color.yellow);
		setGunColor(Color.red);
		setRadarColor(Color.green);
		setBulletColor(Color.red);
		setScanColor(Color.green);
		
		this.setAdjustRadarForGunTurn(true);
		//setAdjustGunForRobotTurn(true);
		//setAdjustRadarForRobotTurn(true);
		
		//gather some data for future use
		double fieldWidth = getBattleFieldWidth();
		double fieldHeight = getBattleFieldHeight();
		double paddingToWall;
		if(fieldWidth < fieldHeight) {
			maxMoveAmount = fieldHeight;
			arenaRadius = fieldWidth;
			paddingToWall = fieldHeight * 0.04;
		} else {
			maxMoveAmount = fieldWidth;
			arenaRadius = fieldHeight;
			paddingToWall = fieldWidth * 0.04;
		}

		//find closest of 4 target start positions
		Point selectedTarget = new Point();
		Point currentPosition = new Point();
		currentPosition.setLocation(getX(), getY());
		
		double minDistance = Integer.MAX_VALUE;
		double[] xTargets = {fieldWidth/2, fieldWidth - paddingToWall, fieldWidth/2, 0 + paddingToWall};
		double[] yTargets = {fieldHeight - paddingToWall, fieldHeight/2, 0 + paddingToWall, fieldHeight/2};
		
		int currentTargetIndex = 0;
		
		targetPositions = new ArrayList<Point>();
		for(int i = 0; i < xTargets.length; i++) {
			Point tempPoint = new Point();
			tempPoint.setLocation(xTargets[i], yTargets[i]);
			targetPositions.add(tempPoint);
			
			double distance;
			distance = Math.sqrt(Math.pow(xTargets[i] - currentPosition.getX(), 2) + Math.pow(yTargets[i] - currentPosition.getY(), 2));
			
			if(distance < minDistance) {
				minDistance = distance;
				selectedTarget.setLocation(xTargets[i], yTargets[i]);
				currentTargetIndex = i;
			}
		}
		
		clockwise = true;
		
		int colorIterator = 0;
		ArrayList<Color> colorList = new ArrayList<Color>();
		colorList.add(new Color(23, 127, 117));
		colorList.add(new Color(33, 182, 168));
		colorList.add(new Color(127, 23, 105));
		colorList.add(new Color(255, 203, 244));
		colorList.add(new Color(182, 149, 33));
		
		while(true) {
			if (getTime() % 10 == 0){
				setScanColor(colorList.get(colorIterator++ % colorList.size()));
				setBodyColor(colorList.get(colorIterator++ % colorList.size()));
			}
			
			ArrayList<Enemy> shootOrder = new ArrayList<Enemy>(enemies.values());
			Collections.sort(shootOrder);
			if(shootOrder.size() > 0){
				Enemy nmy = shootOrder.get(0);
				double newx = nmy.location.x + nmy.velocity * Math.sin(nmy.heading) * getBulletTravelTime(nmy.distance + nmy.velocity, 3);
				double newy = nmy.location.y + nmy.velocity * Math.cos(nmy.heading) * getBulletTravelTime(nmy.distance + nmy.velocity, 3);
				fireAt(new Point((int) newx, (int) newy));
				execute();
			}
			
			setTurnRadarRight(360);
			
			if (chargingAt == null){
				if(getDistanceRemaining() == 0 && getTurnRemaining() == 0){
					if (clockwise)
						currentTargetIndex++;
					else
						currentTargetIndex--;
				}
				
				Point nextGoto = targetPositions.get(Math.abs(currentTargetIndex) % 4);
				gogo( (int) nextGoto.getX(), (int) nextGoto.getY());
			} else {
				gogo(chargingAt.location.x, chargingAt.location.y);
				
				//they got away
				if(chargingAt.distance > 400) {
					chargingAt = null;
				}
			}
			
			execute();
		}
	}
	
	/**************/
	/*** EVENTS ***/
	/**************/
	public void onScannedRobot(ScannedRobotEvent e) {
		if (e.getEnergy() == 0){
			out.println(e.getName() + "Is disabled!");
		}
		
		Point location = new Point();
		//location of enemy is our location plus enemy bearing vector
		location.x = (int) (e.getDistance() * Math.sin(e.getBearingRadians() + getHeadingRadians()) + getX());
		location.y = (int) (e.getDistance() * Math.cos(e.getBearingRadians() + getHeadingRadians()) + getY());

		if (enemies.containsKey(e.getName()))
			enemies.get(e.getName()).update(e.getEnergy(), e.getBearing(), e.getDistance(), e.getHeadingRadians(), e.getVelocity(), location);
		else {
			Enemy newEnemy = new Enemy(e.getName(), e.getEnergy(), e.getBearing(), e.getDistance(), e.getHeading(), e.getVelocity(), location);
			enemies.put(e.getName(), newEnemy);
		}
		
		if (e.getDistance() <= 300 && getOthers() < 3)
			if (chargingAt == null || chargingAt.distance < e.getDistance())
				chargingAt = enemies.get(e.getName());
	}
	
	public void onHitByBullet(HitByBulletEvent event) {
		clockwise = !clockwise;
	}
	
	public void onRobotDeath(RobotDeathEvent e){
		if (chargingAt == enemies.get(e.getName()))
			chargingAt = null;

		enemies.remove(e.getName());
	}
	
	/* FROM ROBO WIKI */
	
	//NOTE - These methods do NOT work for rate control robots.
	
	/**
	 * This method is very verbose to explain how things work.
	 * Do not obfuscate/optimize this sample.
	 */
	private void goTo(double x, double y) {
		/* Transform our coordinates into a vector */
		x -= getX();
		y -= getY();
	 
		/* Calculate the angle to the target position */
		double angleToTarget = Math.atan2(x, y);
	 
		/* Calculate the turn required get there */
		double targetAngle = Utils.normalRelativeAngle(angleToTarget - getHeadingRadians());
	 
		/* 
		 * The Java Hypot method is a quick way of getting the length
		 * of a vector. Which in this case is also the distance between
		 * our robot and the target location.
		 */
		double distance = Math.hypot(x, y);
	 
		/* This is a simple method of performing set front as back */
		double turnAngle = Math.atan(Math.tan(targetAngle));
		setTurnRightRadians(turnAngle);
		if(targetAngle == turnAngle) {
			setAhead(distance);
		} else {
			setBack(distance);
		}
	}
	
	private void gogo(int x, int y) {
	    double a;
	    setTurnRightRadians(Math.tan(
	        a = Math.atan2(x -= (int) getX(), y -= (int) getY()) 
	              - getHeadingRadians()));
	    setAhead(Math.hypot(x, y) * Math.cos(a));
	}
	
	/** Gets the time it will take in game ticks for the bullet to reach the enemy **/
	public static long getBulletTravelTime(double distanceToEnemy, double bulletPower) {
	     return (long) Math.ceil(distanceToEnemy / (20 - (3 * bulletPower)));
	}
	
	public void fireAt(Point target) {
		double distance = Math.sqrt(Math.pow(target.getX()-getX(), 2) + Math.pow(target.getY()-getY(), 2));
		double angle = Math.atan2(target.getX() - this.getX(), target.getY() - this.getY());
		double targetAngle = Utils.normalRelativeAngle(angle - getGunHeadingRadians());
		setTurnGunRightRadians(targetAngle);
		
		if(this.getGunHeat() == 0 && distance < LONGDISTANCE)
			setFire(3);
	}
}