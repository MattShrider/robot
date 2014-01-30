import robocode.HitByBulletEvent;
import robocode.HitRobotEvent;
import robocode.AdvancedRobot;
import robocode.ScannedRobotEvent;

import java.awt.*;

public class Frankenwalls extends AdvancedRobot {

	double fieldWidth, fieldHeight;
	
	int movementDirection = 1, //1 for ahead, -1 for back 
		aimDirection = 1, //1 for left, -1 for right
		closestWall = 1; //1 for top, -1 for bottom
	
	boolean lockedIn = false; //engaged with enemy
	
	double verticalPadding, horizontalPadding; //padding to give from the wall
	
	public void run() {
		setBodyColor(Color.yellow);
		setGunColor(Color.red);
		setRadarColor(Color.blue);
		setBulletColor(Color.green);
		setScanColor(Color.white);

		fieldWidth = this.getBattleFieldWidth();
		fieldHeight = this.getBattleFieldHeight();
		
		verticalPadding = 0.04 * (fieldWidth > fieldHeight ? fieldHeight : fieldWidth);
		closestWall = fieldHeight - getY() <  fieldHeight / 2 ? 1 : -1;
		double closestDistance;
		horizontalPadding = verticalPadding * 5;
		
		//face either top or bottom wall
		if(closestWall == 1) {
			//face top wall
			turnRight(-getHeading());
			closestDistance = fieldHeight - getY() - verticalPadding;
		} else {
			//face bottom wall
			turnRight(-getHeading() + 180);
			closestDistance = getY() - verticalPadding;
		}
		
		//go to top or bottom wall with padding
		ahead(closestDistance);
		
		//turn toward arena
		turnRight(90);
		turnGunRight(90);
		
		movementDirection *= closestWall;
		if(getX() < fieldHeight / 2) {
			movementDirection *= -1;
		}
		
		while (true) {
			setGunRotation(160);
			setLateralMovement();
			
			execute();
		}
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		if(lockedIn) {
			fire(1);
			fire(1);
		} else if(e.getDistance() <= 300 || getOthers() >= 5) {
			setFire(3);
		} else {
			setFire(2);
		}
		
		if(e.getDistance() <= 400) {
			aimDirection *= -1; //creates a locked in effect
			lockedIn = true;
		}
		
		lockedIn = false;
	}
	
	public void onHitRobot(HitRobotEvent e) {
		movementDirection *= -1;
		
		double enemyBearing = e.getBearing();
		setTurnGunRight(enemyBearing);
		setFire(2);
		execute();
	}
	
	//rotate gun to cover radarCoverage amount of degrees
	public void setGunRotation(double radarCoverage) {
		double degreesToCover = radarCoverage / 2;
		
		double gunHeading = getGunHeading();
		if(closestWall == -1) {
			gunHeading = (gunHeading + 180) % 360;
		}
		
		if(aimDirection == -1)
			setTurnGunRight(7);
		else if(aimDirection == 1)
			setTurnGunRight(-7);
		
		if(gunHeading >= 360 - degreesToCover && aimDirection == -1)
			aimDirection = 1;
		else if(gunHeading <= degreesToCover && aimDirection == 1)
			aimDirection = -1;
		
		//lockedIn cancels gun rotation
		if(lockedIn)
			setTurnGunRight(0);
	}
	
	public void setLateralMovement() {
		double relativeX = getX();
		if(closestWall == -1) {
			relativeX = fieldWidth - relativeX;
		}
		
		if(movementDirection == -1)
			setAhead(20);
		else if(movementDirection == 1)
			setAhead(-20);
		
		if(relativeX >= fieldWidth - horizontalPadding && movementDirection == -1)
			movementDirection = 1;
		else if(relativeX <= horizontalPadding && movementDirection == 1)
			movementDirection = -1;
	}
}