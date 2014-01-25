import robocode.HitByBulletEvent;
import robocode.RateControlRobot;
import robocode.RobocodeFileWriter;
import robocode.ScannedRobotEvent;

import java.awt.*;
import java.io.PrintWriter;
import java.util.ArrayList;

public class BelugaBot extends RateControlRobot {
	
	double arenaRadius;
	double maxMoveAmount;
	ArrayList<Point> targetPositions;
	boolean clockwise;
	
	public void run() {
		setBodyColor(Color.blue);
		setGunColor(Color.blue);
		setRadarColor(Color.white);
		setBulletColor(Color.red);
		setScanColor(Color.white);
		
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
		
		moveTo(selectedTarget);
		
		int targetPositionsSize = targetPositions.size();
		clockwise = true;
		
		while(true) {
			if(clockwise)
				currentTargetIndex++;
			else
				currentTargetIndex--;
			moveTo(targetPositions.get(Math.abs(currentTargetIndex) % 4));
		}
	}
	
	/**************/
	/*** EVENTS ***/
	/**************/
	public void onScannedRobot(ScannedRobotEvent e) {
		if(e.getDistance() < 600) {
			fire(20);
		}
		
		if(this.getOthers() <= 3) {
			fire(5);
		}
	}
	
	public void onHitByBullet(HitByBulletEvent event) {
		clockwise = !clockwise;
	}
	
	/*****************/
	/*** OUR STUFF ***/
	/*****************/
	
	/* FROM WIKI */
	private void moveTo(Point target) {
		Point currentPosition = new Point();
		currentPosition.setLocation(getX(), getY());
		
		double distanceToTarget = currentPosition.distance(target);
		double angleToTarget = normalRelativeAngle(getAngleToTarget(currentPosition, target));
		
		if (Math.abs(angleToTarget) > 90.0) {
            distanceToTarget *= -1.0;
            if (angleToTarget > 0.0) {
                angleToTarget -= 180.0;
            }
            else {
                angleToTarget += 180.0;
            }
        }
		
		turnRight(angleToTarget - getHeading());
		ahead(distanceToTarget);
	}
	
	private double getAngleToTarget(Point source, Point target) {	
        return Math.toDegrees(Math.atan2(target.getX() - source.getX(), target.getY() - source.getY()));
	}
	
	/* FROM ROBO WIKI */
	private double normalRelativeAngle(double angle) {
        double relativeAngle = angle % 360;
        if (relativeAngle <= -180)
            return 180 + (relativeAngle % 180);
        else if (relativeAngle > 180)
            return -180 + (relativeAngle % 180);
        else
            return relativeAngle;
    }
}