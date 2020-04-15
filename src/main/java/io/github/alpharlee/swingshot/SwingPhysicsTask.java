package io.github.alpharlee.swingshot;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class SwingPhysicsTask {
	private Player player;
	private Location pivot;

	private double lineLength;

	private double theta, thetaVelocity;
	private double phi, phiVelocity;

	private double initialPhiSign;

	private static final double gravity = 13; // Assuming that gravity is -13 blocks per second

	private static final double stepsPerSecond = 1000;
	private static final double ticksPerSecond = 20;
	public static int stepsPerTick = (int) (stepsPerSecond / ticksPerSecond); // FIXME set to private final

	private static final double eyeToSwingHeightDisplacement = -0.5;

	public static double linearVelocityMultiplier = 0.85; // FIXME set to private final

	// The location that the next iteration should land at
	private Location destinationLoc;

	public static boolean printDebug = false; // FIXME delete

	public SwingPhysicsTask(Player player, Location pivot) {
		this.player = player;
		this.pivot = pivot;

		lineLength = getPlayerSwingVector().distance(pivot.toVector()); // TODO: Make line length dynamic

		// FIXME remove these testers
		{
			theta = Math.PI / 4.0;
			thetaVelocity = 0 * Math.PI / 8.0;

			phi = 3 * Math.PI / 4.0;
			phiVelocity = 0 * Math.PI / 4.0;
		}

		calculateCurrentTheta();
		calculateCurrentPhi();
		initialPhiSign = Math.signum(phi);

		destinationLoc = getPlayerSwingLocation();
	}

	public void run() {
		swingPlayer();
	}

	public Location getPivot() {
		return pivot.clone();
	}

	private void calculateCurrentTheta() {
		Vector downVector = new Vector(0, -1, 0);
		Vector pivotToPlayerVector = getPlayerSwingVector().subtract(pivot.toVector()).normalize();
		double dotProduct = pivotToPlayerVector.dot(downVector);

		theta = Math.acos(dotProduct); // NOTE Taking shortcut with assumption that all lengths are 1

		// Following are calculations for thetaVelocity

		// Get perpendicular to player's position (along the theta velocity vector)
		Vector orthogonalFromPlayer = downVector.clone().subtract(pivotToPlayerVector.clone().multiply(dotProduct));

		double linearVelocity; // The velocity in-line with the orthoganal component. Wait until orthogonal calculated (to avoid NaN errors)

		if (orthogonalFromPlayer.lengthSquared() > 0.000001) {
			orthogonalFromPlayer = orthogonalFromPlayer.normalize();

			// 20 times multiplier to convert from blocks per tick to blocks per second
			linearVelocity = player.getVelocity().dot(orthogonalFromPlayer) * ticksPerSecond;
		} else {
			// The orthogonal vector is effectively zero, just add the linear velocity because it's already tangential
			linearVelocity = player.getVelocity().length() * ticksPerSecond;
		}

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
		Vector orthogonalFromPlayer = (new Vector(z, 0, -x));
		if (orthogonalFromPlayer.lengthSquared() > 0.000001) {
			orthogonalFromPlayer = orthogonalFromPlayer.normalize();
			double linearVelocity = player.getVelocity().dot(orthogonalFromPlayer) * ticksPerSecond; // 20 times multiplier to convert from blocks per tick to blocks per second
			phiVelocity = linearVelocity / Math.sqrt(x * x + z * z);
		} else {
			// Orthogonal from player is zero (e.g. player directly under pivot), so all linear velocity goes to theta, not phi
			phiVelocity = 0;
		}
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

		theta += thetaVelocity / stepsPerSecond;

		// Drag effect
		thetaVelocity *= 0.999999;
		theta %= 2.0 * Math.PI;
	}

	private void incrementPhi() {
		double thetaVelocityStep = thetaVelocity / stepsPerSecond;
		double phiVelocityStep = phiVelocity / stepsPerSecond;

		double tanTheta = Math.tan(theta);
		double phiAccelerationStep;
		if (Math.abs(tanTheta) > 0.000001) {
			phiAccelerationStep = -2 * phiVelocityStep * thetaVelocityStep / tanTheta;
		} else {
			phiAccelerationStep = 0;
		}

		phiVelocity += phiAccelerationStep;

		phi += phiVelocity / stepsPerSecond;

		// Drag effect
		phiVelocity *= 0.999999;

		phi %= 2.0 * Math.PI;
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
			player.getWorld().spawnParticle(Particle.SUSPENDED, particleLocation, 1, 0, 0, 0, 0, null, true);

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
		destinationLoc = pivot.clone().add(displacement);
		Vector unscaledVelocity = destinationLoc.clone().subtract(getPlayerSwingLocation()).toVector();
		Vector newVelocity;

		// Test to see if colliding with wall
		RayTraceResult movingIntoBlockRayTrace = movingIntoBlock(unscaledVelocity);
		if (movingIntoBlockRayTrace == null || movingIntoBlockRayTrace.getHitBlock() == null) {
			// No wall detected
			newVelocity = unscaledVelocity.clone().multiply(linearVelocityMultiplier);

		} else {
			// Wall detected

			// Do wall collision:
			// Get dot product of unscaledVelocity orthogonal to the blockFace's (normal) vector
			Vector blockFaceDirection = movingIntoBlockRayTrace.getHitBlockFace().getDirection();
			Vector normalVelocity = blockFaceDirection.multiply(unscaledVelocity.clone().dot(blockFaceDirection));
			Vector parallelVelocity = unscaledVelocity.clone().subtract(normalVelocity);
			newVelocity = parallelVelocity.clone().multiply(linearVelocityMultiplier);

			// Apply wall friction
			newVelocity.multiply(0.90);

			calculateCurrentTheta();
			calculateCurrentPhi();
		}

		// Set the player's velocity
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
			player.getWorld().spawnParticle(Particle.END_ROD, destinationLoc, 1, 0, 0, 0, 0); // FIXME delete
		}

	}

	private RayTraceResult movingIntoBlock(Vector direction) {
		boolean shouldCheck = false;
		RayTraceResult rayTraceResult = null;
		Block targetBlock = null;
		double height = 0;
		while (!shouldCheck) {
			// Do a ray test from the player's location (plus height)
			// FIXME offset raytrace by player's hitbox (i.e. do a raytrace from the outside to determine player dimensions)

			rayTraceResult = player.getWorld().rayTraceBlocks(player.getLocation().add(0, height, 0), direction, 1d, FluidCollisionMode.NEVER, true);
			if (rayTraceResult != null && rayTraceResult.getHitBlock() != null) {
				return rayTraceResult;
			}

			height += 0.5; // Increment height and check again TODO what increments should be tested?
			shouldCheck = targetBlock != null || height <= player.getHeight();
		}

		return null;
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
		double eyeFeetDisplacement = 1.62;

		return swingLocation.clone().add(0, - eyeToSwingHeightDisplacement, 0).add(0, - eyeFeetDisplacement, 0);
	}
}
