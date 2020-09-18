package minecrafttransportsimulator.rendering.components;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleAnimationDefinition;
import minecrafttransportsimulator.systems.VehicleAnimationSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;

/**This class represents a rotatable object of a model.
 *
 * @author don_bruce
 */
public class TransformRotatable extends ATransformRenderable{
	private final Point3d rotationAxis;
	private double rotationMagnitude;
	
	public TransformRotatable(VehicleAnimationDefinition definition){
		super(definition);
		//For the axis defined in the JSON, the axis is the normalized value of the defined vector, while the 
		//rotation magnitude is the magnitude of that vector.
		this.rotationAxis = definition.axis.copy();
		this.rotationMagnitude = rotationAxis.length();
		rotationAxis.normalize();
	}

	@Override
	public double applyTransform(EntityVehicleF_Physics vehicle, APart optionalPart, float partialTicks, double offset){
		//Update rotation axis, if required.
		if(updateRotationAxis(vehicle, rotationAxis)){
			rotationMagnitude = rotationAxis.length();
			rotationAxis.normalize();
		}
		
		//Get rotation.
		double rotation = getFactoredState(vehicle, VehicleAnimationSystem.getVariableValue(definition.variable,  partialTicks, vehicle, optionalPart));
		
		//Add offsets and clamp translation.
		rotation = rotationMagnitude*(definition.absolute ? Math.abs(rotation) : rotation) + definition.offset + offset;
		if(definition.clampMin != 0 && rotation < definition.clampMin + offset){
			rotation = definition.clampMin + offset;
		}else if(definition.clampMax != 0 && rotation > definition.clampMax - offset){
			rotation = definition.clampMax - offset;
		}
		
		//Do rotation.
		if(rotation != 0){
			GL11.glTranslated(definition.centerPoint.x, definition.centerPoint.y, definition.centerPoint.z);
			GL11.glRotated(rotation, rotationAxis.x, rotationAxis.y, rotationAxis.z);
			GL11.glTranslated(-definition.centerPoint.x, -definition.centerPoint.y, -definition.centerPoint.z);
		}
		return rotation;
	}
	
	/**
	 *  Manual helper function for updating the rotaitonAxis of a part prior to rendering.
	 *  Required for auto-rotations.  This function should return true if the axis was updated
	 *  to allow the calling method to save the axis state.
	 */
	protected boolean updateRotationAxis(EntityVehicleF_Physics vehicle, Point3d rotationAxis){
		return false;
	}
}
