import robocode.HitByBulletEvent;
import robocode.RateControlRobot;
import robocode.ScannedRobotEvent;

import java.awt.*;
import java.util.ArrayList;

public class BelugaBot extends RateControlRobot {
	
	double arenaRadius;
	double maxMoveAmount;
	
	public void run() {
		setBodyColor(Color.yellow);
		setGunColor(Color.green);
		setRadarColor(Color.yellow);
		setBulletColor(Color.green);
		setScanColor(Color.yellow);
		
		//gather some data for future use
		double fieldWidth = getBattleFieldWidth();
		double fieldHeight = getBattleFieldHeight();
		if(fieldWidth < fieldHeight) {
			maxMoveAmount = fieldHeight;
			arenaRadius = fieldWidth;
		} else {
			maxMoveAmount = fieldWidth;
			arenaRadius = fieldHeight;
		}
		
		double xStart, yStart;
		xStart = getX();
		yStart = getY();

		//find closest of 4 target start positions
		Point selectedTarget = new Point();
		double minDistance = Integer.MAX_VALUE;
		double[] xTargets = {fieldWidth/2, fieldWidth, fieldWidth/2, 0};
		double[] yTargets = {fieldHeight, fieldHeight/2, 0, fieldHeight/2};
		
		for(int i = 0; i < 4; i++) {
			double distance;
			distance = Math.sqrt(Math.pow(xTargets[i] - xStart, 2) + Math.pow(yTargets[i] - yStart, 2));
			
			if(distance < minDistance) {
				minDistance = distance;
				selectedTarget.setLocation(xTargets[i], yTargets[i]);
			}
		}
		
		//find angle to selected target position
		

		//find a corner
		turnLeft(getHeading() - 45);
		ahead(maxMoveAmount);
		turnRight(90);
		ahead(maxMoveAmount);
		
		while(true) {
			
		}
	}
	
	public void onScannedRobot(ScannedRobotEvent e) {
		
	}
	
	public void onHitByBullet(HitByBulletEvent event) {
		back(40);
	}
}