package io.github.alpharlee.swingshot;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class SwingPhysicsTask {
	private Player player;
	private Location pivot;

	private double lineLength;

	private double theta, thetaVelocity, thetaAcceleration;
	private double phi, phiVelocity, phiAcceleration;

	private double initialPhiSign;

	private static final double gravity = 13; // Assuming that gravity is -13 blocks per second

	private static final double stepsPerSecond = 100;
	private static final double ticksPerSecond = 20;
	public static int stepsPerTick = (int) (stepsPerSecond / ticksPerSecond); // FIXME set to private final

	private static final double eyeToSwingHeightDisplacement = -0.5;

	public static double linearVelocityMultiplier = 0.5; // FIXME set to private final

	public static boolean printDebug = false; // FIXME delete
	public static int debugTooCloseCount = 0;
	public static int debugTooFarCount = 0;
	public static int debugZeroCount = 0;
	public static int debugCorrectDistCount = 0;

	public SwingPhysicsTask(Player player, Location pivot) {
		this.player = player;
		this.pivot = pivot;

		lineLength = getPlayerSwingVector().distance(pivot.toVector()); // TODO: Make line length dynamic

		// FIXME remove these testers
		{
			theta = Math.PI / 4.0;
			thetaVelocity = 0 * Math.PI / 8.0;
			thetaAcceleration = 0;

			phi = 3 * Math.PI / 4.0;
			phiVelocity = 0 * Math.PI / 4.0;
			phiAcceleration = 0;
		}

		calculateCurrentTheta();
		calculateCurrentPhi();
		initialPhiSign = Math.signum(phi); // FIXME what happens if phi is perfectly 0?
	}

	public void run() {
		// FIXME enable
//		calculateCurrentPhi();
//		calculateCurrentTheta();
		swingPlayer();
	}

	public Location getPivot() {
		return pivot.clone();
	}

	private void calculateCurrentTheta() {
		Vector downVector = new Vector(0, -1, 0);
		Vector pivotToPlayerVector = getPlayerSwingVector().subtract(pivot.toVector()).normalize();
		double dotProduct = pivotToPlayerVector.dot(downVector);

//		theta = Math.acos(dotProduct / getPlayerSwingVector().length());
		theta = Math.acos(dotProduct); // NOTE Taking shortcut with assumption that all lengths are 1

		if (!isInInitialPhiQuadrant()) {
//			theta = theta;
//			player.sendMessage(String.format("!!! Quadrant change! theta: %.3f v: %.3f", theta, thetaVelocity)); //FIXME delete
		} else {
			theta = -theta;
//			player.sendMessage(String.format("!!! theta: %.3f, v: %.3f", theta, thetaVelocity)) ;//FIXME delete
		}

		// Following are calculations for thetaVelocity

		// Get perpendicular to player's position (along the theta velocity vector)
		Vector orthogonalFromPlayer = downVector.clone().subtract(pivotToPlayerVector.clone().multiply(dotProduct)).normalize();

		// 20 times multiplier to convert from blocks per tick to blocks per second
		double linearVelocity = player.getVelocity().dot(orthogonalFromPlayer) * ticksPerSecond;
		// Get theta V by dividing the linear velocity by the radius (length between pivot and player)
		thetaVelocity = - linearVelocity / (getPlayerSwingVector().subtract(pivot.toVector())).length();
	}

	private void calculateCurrentPhi() {
		// Calculations for phi are different by nature because they all are in the XZ plane only

		double x = getPlayerSwingVector().getX() - pivot.getX();
		double z = getPlayerSwingVector().getZ() - pivot.getZ();
		phi = Math.atan2(z, x);
//		player.sendMessage("!!! calc phi: " + phi); // FIXME delete

		// Get perpendicular to player's position (i.e. along the phi velocity vector)
		Vector orthogonalFromPlayer = (new Vector(z, 0, -x)).normalize();
		double linearVelocity = player.getVelocity().dot(orthogonalFromPlayer) * ticksPerSecond; // 20 times multiplier to convert from blocks per tick to blocks per second
		phiVelocity = linearVelocity / Math.sqrt(x * x + z * z);
	}

	private boolean isInInitialPhiQuadrant() {
		return Math.signum(phi) == initialPhiSign;
	}

	private void incrementTheta() {
		double gravityStep = gravity / stepsPerSecond;
		double phiVelocityStep = phiVelocity / stepsPerSecond;

		// theta_a = (l * phi_v^2 * sin(theta)cos(theta) - g * sin(theta) ) / l
		double thetaAccelerationStep =
				(lineLength * (phiVelocityStep * phiVelocityStep) * Math.sin(theta) * Math.cos(theta)
						- gravityStep * Math.sin(theta))
						/ lineLength;

		thetaVelocity += thetaAccelerationStep; // This is the only place thetaV is calculated (doesn't need division by steps)

//		player.sendMessage( "!!! pre: " + theta); // FIXME delete
		theta += thetaVelocity / stepsPerSecond;
//		player.sendMessage(String.format("!!!post: %.3f", theta)); // FIXME DELETE

		theta %= 2.0 * Math.PI;
	}

	private void incrementPhi() {
		double thetaVelocityStep = thetaVelocity / stepsPerSecond;
		double phiVelocityStep = phiVelocity / stepsPerSecond;

		double phiAccelerationStep = -2 * phiVelocityStep * thetaVelocityStep / Math.tan(theta);

		phiVelocity += phiAccelerationStep;
		phi += phiVelocity / stepsPerSecond;

		phi %= 2.0 * Math.PI;
//		player.sendMessage("!!! post phi: " + phi); // FIXME delete
	}


	private void swingPlayer() {
		for (int i = 0; i < stepsPerTick; i++) {
			incrementTheta();
			incrementPhi();
		}

		calculateCartesians();
		drawLineFromPlayer(player, pivot.toVector());
	}

	private void drawLineFromPlayer(Player player, Vector endPoint) {
		double particleDistance = 0.25; //Distance from one particle to the next TODO set to be configurable
		double lineDistance = particleDistance;
		double maxLineDistance = getPlayerSwingVector().distance(endPoint);

		Vector particleDirection = endPoint.clone().subtract(getPlayerSwingVector())
				.normalize().multiply(particleDistance);
		Location particleLocation = getPlayerSwingLocation().add(particleDirection);

		while (lineDistance < maxLineDistance) {
			player.getWorld().spawnParticle(Particle.WATER_BUBBLE, particleLocation, 1);

			particleLocation.add(particleDirection);
			lineDistance += particleDistance;
		}
	}

	Location lastLocation = null; // FIXME delete
	double expectedDist;

	private void calculateCartesians() {
		// FIXME replace with velocity calculation instead
		double x, y , z;
		x = lineLength * Math.sin(theta) * Math.cos(phi);
		z = lineLength * Math.sin(theta) * Math.sin(phi); // Note MC uses the non-conventional axis
		y = -lineLength * Math.cos(theta);

		Vector displacement = new Vector(x, y, z);
		Location destinationLoc = pivot.clone().add(displacement);
		Vector newVelocity = destinationLoc.clone().subtract(getPlayerSwingLocation()).toVector().multiply(linearVelocityMultiplier);
		player.setVelocity(newVelocity);

		if (printDebug) { // FIXME delete this block
			//Summon particles at player's location
			//TODO: Customize/show/hide effect here
			/*
			 * Particle effect parameters:
			 * 1st: Particle effect
			 * 2nd: Location
			 * 3rd: Count
			 * 4th-6th: X, Y, Z offsets
			 * 7th: Extra (usually speed)
			 */
			player.getWorld().spawnParticle(Particle.FLAME, destinationLoc, 1, 0, 0, 0, 0); // FIXME delete

			if (lastLocation != null) {
				double actualDist = getPlayerSwingLocation().distance(lastLocation);
				player.sendMessage(ChatColor.LIGHT_PURPLE + String.format("!!! LastLoc %.3f, %.3f, %.3f. Dist: %.3f",
						lastLocation.getX(),
						lastLocation.getY(),
						lastLocation.getZ(),
						actualDist));
			}

			player.sendMessage(String.format("!!! At %.3f, %.3f, %.3f",
					getPlayerSwingLocation().getX(),
					getPlayerSwingLocation().getY(),
					getPlayerSwingLocation().getZ())); // FIXME del
			player.sendMessage(String.format("!!! Moving to %.3f, %.3f, %.3f. Dist: %.3f",
					destinationLoc.getX(),
					destinationLoc.getY(),
					destinationLoc.getZ(),
					getPlayerSwingLocation().distance(destinationLoc))); // FIXME del
			player.sendMessage(String.format("!!! speed: %.3f, %.3f, %.3f = %.3f",
					newVelocity.getX(),
					newVelocity.getY(),
					newVelocity.getZ(),
					newVelocity.length()));
		}

		lastLocation = getPlayerSwingLocation();
		expectedDist = newVelocity.length();
	}

	public Location getPlayerSwingLocation() {
		return player.getEyeLocation().clone().add(new Vector(0.0, eyeToSwingHeightDisplacement, 0.0));
	}

	public Vector getPlayerSwingVector() {
		return player.getEyeLocation().toVector().add(new Vector(0.0, eyeToSwingHeightDisplacement, 0.0));
	}

	/**
	 * Get the location of the player's feet from the given swing location
	 * @return
	 */
	private Location getPlayerLocationFromSwingLocation(Location swingLocation) {
		double eyeFeetDisplacement = 1.6200000047683716;

		return swingLocation.clone().add(0, - eyeToSwingHeightDisplacement, 0).add(0, - eyeFeetDisplacement, 0);
	}
}
