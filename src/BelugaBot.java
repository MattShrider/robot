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
import java.util.HashMap;


public class BelugaBot extends AdvancedRobot {
	
	String name;
	double energy;
	double bearing;
	double distance;
	double heading;
	double velocity;
	
	Enemy chargingAt = null;
	Enemy shootingAt = null;
	boolean aimDirectionRight = false;
	
	/** 
	 * Private class for representing enemies on the field.  Construction based on the onScannedRobot event.
	 */
	private class Enemy {
		String name;
		double energy;
		double bearing;
		double distance;
		double heading;
		double velocity;
		Point location;
		
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
		
	}
	
	double arenaRadius;
	double maxMoveAmount;
	ArrayList<Point> targetPositions;
	boolean clockwise;
	int turncounter = 0;
	HashMap<String, Enemy> enemies = new HashMap<String, Enemy>();
	
	public void run() {
		setBodyColor(Color.blue);
		setGunColor(Color.blue);
		setRadarColor(Color.white);
		setBulletColor(Color.red);
		setScanColor(Color.white);
		
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
		for(int i = 0; i < 4; i++) {
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
		double counter = 0;
		
		while(true) {
			if (chargingAt == null){
				if(getDistanceRemaining() == 0 && getTurnRemaining() == 0){
					if (clockwise)
						currentTargetIndex++;
					else
						currentTargetIndex--;
				}
				
				Point nextGoto = targetPositions.get(Math.abs(currentTargetIndex) % 4);
				gogo( (int) nextGoto.getX(), (int) nextGoto.getY());
				setTurnGunRight(20);
				execute();
			} else {
				gogo(chargingAt.location.x, chargingAt.location.y);
				
				double gunBearing = getGunHeading() - chargingAt.heading;
				
				setTurnGunRight(20);
				if(gunBearing > -10 && gunBearing < 10) {
					if(aimDirectionRight)
						setTurnGunRight(6);
					else
						setTurnGunLeft(6);
					
					if(counter % 4 == 0)
						aimDirectionRight = !aimDirectionRight;
					counter++;
				}
				
				//they got away
				if(chargingAt.distance > 250) {
					chargingAt = null;
				}
				
				execute();
			}
		}
	}
	
	/**************/
	/*** EVENTS ***/
	/**************/
	public void onScannedRobot(ScannedRobotEvent e) {
		Point location = new Point();
		//location of enemy is our location plus enemy bearing vector
		location.x = (int) (e.getDistance() * Math.sin(e.getBearingRadians() + getHeadingRadians()) + getX());
		location.y = (int) (e.getDistance() * Math.cos(e.getBearingRadians() + getHeadingRadians()) + getY());
		
		if (enemies.containsKey(e.getName()))
			enemies.get(e.getName()).update(e.getEnergy(), e.getBearing(), e.getDistance(), e.getHeading(), e.getVelocity(), location);
		else
			enemies.put(e.getName(),new Enemy(e.getName(), e.getEnergy(), e.getBearing(), e.getDistance(), e.getHeading(), e.getVelocity(), location));
		
		if (e.getDistance() <= 200 && getOthers() < 3)
			if (chargingAt == null || chargingAt.distance < e.getDistance())
				chargingAt = enemies.get(e.getName());
		if(e.getDistance() < 300 || e.getVelocity() <= 1) {
			//TODO - change the fire() method to firingAt and handle it in the main while loop.
			fire(3);
		} else if (getOthers() >= 10) {
			fire(3);
		}
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
}