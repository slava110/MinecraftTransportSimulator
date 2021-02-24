package minecrafttransportsimulator.entities.instances;

import minecrafttransportsimulator.MasterLoader;
import minecrafttransportsimulator.baseclasses.Damage;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.components.AEntityE_Multipart;
import minecrafttransportsimulator.items.instances.ItemPart;
import minecrafttransportsimulator.jsondefs.JSONPartDefinition;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.packets.components.InterfacePacket;
import minecrafttransportsimulator.packets.instances.PacketPartGroundDevice;
import minecrafttransportsimulator.rendering.components.InterfaceRender;
import minecrafttransportsimulator.rendering.instances.ParticleSmoke;
import minecrafttransportsimulator.sound.InterfaceSound;
import minecrafttransportsimulator.sound.SoundInstance;
import minecrafttransportsimulator.systems.ConfigSystem;

/**A ground device is simply a part of a vehicle that touches the ground.
 * This class is used to perform ground physics, which include steering, 
 * turning, and hill climbing.  Can be a wheel-based part that rolls and 
 * provides power from engines, a solid part that doesn't provide power but
 * still allows for movement, a longer part with multiple hitboxes, a 
 * floating part, etc.  Each property is set via the JSON definition, though
 * a few are vehicle-dependent. 
 * 
 * @author don_bruce
 */
public class PartGroundDevice extends APart{
	public static final Point3d groundDetectionOffset = new Point3d(0, -0.05F, 0);
	public static final Point3d groundOperationOffset = new Point3d(0, -0.25F, 0);
	
	//External states for animations.
	public boolean skipAngularCalcs = false;
	public double angularPosition;
	public double angularVelocity;
	
	//Internal states for control and physics.
	private boolean isFlat;
	private boolean contactThisTick = false;
	private int ticksCalcsSkipped = 0;
	private double prevAngularVelocity;
	private final PartGroundDeviceFake fakePart;
	
	public PartGroundDevice(AEntityE_Multipart<?> entityOn, JSONPartDefinition placementDefinition, WrapperNBT data, APart parentPart){
		super(entityOn, placementDefinition, data, parentPart);
		this.isFlat = data.getBoolean("isFlat");
		
		//If we are a long ground device, add a fake ground device at the offset to make us
		//have a better contact area.  If we are a fake part calling this as a super constructor,
		//we will be marked as such.  Check that to prevent loops.  Also set some parameters manually
		//as fake parts have a few special properties.
		//Don't add the fake part until the first update loop.  This prevents save/load errors.
		if(!isFake() && getLongPartOffset() != 0 && !placementDefinition.isSpare){
			//Need to swap placement for fake part so it uses the offset.
			Point3d actualPlacement = placementDefinition.pos;
			placementDefinition.pos = placementDefinition.pos.copy().add(0D, 0D, getLongPartOffset());
			fakePart = new PartGroundDeviceFake(this, placementDefinition, data, null);
			placementDefinition.pos = actualPlacement;
			//This hack prevents us from adding this part to the main list during vehicle construction.
			if(vehicleOn.partSlotBoxes != null){
				vehicleOn.addPart(fakePart, true);
			}else{
				vehicleOn.partsFromNBT.add(fakePart);
			}
		}else{
			fakePart = null;
		}
	}
	
	@Override
	public void attack(Damage damage){
		if(damage.isExplosion || Math.random() < 0.5){
			setFlatState(true);
		}
	}
	
	@Override
	public void update(){
		super.update();
		if(vehicleOn != null){
			if(vehicleOn.groundDeviceCollective.groundedGroundDevices.contains(this)){
				//If we aren't skipping angular calcs, change our velocity accordingly.
				if(!skipAngularCalcs){
					prevAngularVelocity = angularVelocity;
					angularVelocity = getDesiredAngularVelocity();
				}
				
				//Set contact for wheel skidding effects.
				if(definition.ground.isWheel){
					if(Math.abs(prevAngularVelocity)/(vehicleOn.groundVelocity/(getHeight()*Math.PI)) < 0.25 && vehicleOn.velocity > 0.3){
						//Sudden angular velocity increase.  Mark for skidding effects if the block below us is hard.
						Point3d blockPositionBelow = position.copy().add(0, -1, 0);
						if(!world.isAir(blockPositionBelow) && world.getBlockHardness(blockPositionBelow) >= 1.25){
							contactThisTick = true;
						}
					}
					
					//If we have a slipping wheel, count down and possibly pop it.
					if(!vehicleOn.world.isClient() && !isFlat){
						if(!skipAngularCalcs){
							if(ticksCalcsSkipped > 0){
								--ticksCalcsSkipped;
							}
						}else{
							++ticksCalcsSkipped;
							if(Math.random()*50000 < ticksCalcsSkipped){
								setFlatState(true);
							}
						}
					}
				}
				
				//Check for colliding entities and damage them.
				if(!vehicleOn.world.isClient() && vehicleOn.velocity >= ConfigSystem.configObject.damage.wheelDamageMinimumVelocity.value){
					boundingBox.widthRadius += 0.25;
					boundingBox.depthRadius += 0.25;
					final double wheelDamageAmount;
					if(!ConfigSystem.configObject.damage.wheelDamageIgnoreVelocity.value){
						wheelDamageAmount = ConfigSystem.configObject.damage.wheelDamageFactor.value*vehicleOn.velocity*vehicleOn.currentMass/1000F;
					}else{
						wheelDamageAmount = ConfigSystem.configObject.damage.wheelDamageFactor.value*vehicleOn.currentMass/1000F;
					}
					Damage wheelDamage = new Damage("wheel", wheelDamageAmount, boundingBox, this, vehicleOn.getController());
					vehicleOn.world.attackEntities(wheelDamage, null);
					boundingBox.widthRadius -= 0.25;
					boundingBox.depthRadius -= 0.25;
				}
			}else if((placementOffset.z > 0 && !vehicleOn.definition.motorized.isFrontWheelDrive) || (placementOffset.z <= 0 && !vehicleOn.definition.motorized.isRearWheelDrive)){
				if(vehicleOn.brake > 0 || vehicleOn.parkingBrakeOn){
					angularVelocity = 0;
				}else if(angularVelocity>0){
					angularVelocity = (float) Math.max(angularVelocity - 0.05, 0);
				}
			}
			angularPosition += angularVelocity;
		}
	}
	
	@Override
	public void remove(){
		super.remove();
		if(fakePart != null){
			fakePart.isValid = false;
		}
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public ItemPart getItem(){
		return isFlat ? null : super.getItem();
	}
	
	@Override
	public float getWidth(){
		return definition.ground.width;
	}
	
	@Override
	public float getHeight(){
		return isFlat ? definition.ground.flatHeight : definition.ground.height;
	}
	
	@Override
	public Point3d getRenderingRotation(float partialTicks, boolean animationValue){
		return new Point3d((definition.ground.isWheel || (animationValue && definition.ground.isTread)) ? EntityVehicleF_Physics.SPEED_FACTOR*(angularPosition + angularVelocity*partialTicks)*360D : 0, 0, 0D);
	}
	
	/**
	 * Attempts to set the ground device flat state to the passed-in state.  Checks to make
	 * sure the ground device can actually go flat if it is being requested to do so.
	 */
	public void setFlatState(boolean setFlat){
		if(world.isClient()){
			if(setFlat){
				InterfaceSound.playQuickSound(new SoundInstance(this, MasterLoader.resourceDomain + ":wheel_blowout"));
			}
		}else{
			//On the server, can we go flat and does the config let us?
			//Or if we are repairing, are we flat in the first place?
			if(setFlat){
				if(isFlat || definition.ground.flatHeight == 0 || !ConfigSystem.configObject.damage.wheelBreakage.value){
					return;
				}
			}else{
				if(!isFlat){
					return;
				}
			}
			//Valid conditions, send packet before continuing.
			InterfacePacket.sendToAllClients(new PacketPartGroundDevice(this, setFlat));
		}
		
		//Set flat state and new bounding box.
		isFlat = setFlat;
		boundingBox.heightRadius = getHeight();
		if(vehicleOn != null){
			vehicleOn.groundDeviceCollective.updateBounds();
		}
	}
	
	public boolean getFlatState(){
		return isFlat;
	}
	
	public float getFrictionLoss(){
		Point3d groundPosition = position.copy().add(0, -1, 0);
		if(!world.isAir(groundPosition)){
			return 0.6F - world.getBlockSlipperiness(groundPosition) + world.getRainStrength(groundPosition)*0.1F;
		}else{
			return 0;
		}
	}
	
	public double getDesiredAngularVelocity(){
		if(vehicleOn != null){
			if(vehicleOn.skidSteerActive){
				if(placementOffset.x > 0){
					return getLongPartOffset() == 0 ? vehicleOn.rudderAngle/2000D/(getHeight()*Math.PI) : vehicleOn.rudderAngle/2000D;
				}else if(placementOffset.x < 0){
					return getLongPartOffset() == 0 ? -vehicleOn.rudderAngle/2000D/(getHeight()*Math.PI) : -vehicleOn.rudderAngle/2000D;
				}else{
					return 0;
				}
			}else{
				if(vehicleOn.goingInReverse){
					return getLongPartOffset() == 0 ? -vehicleOn.groundVelocity/(getHeight()*Math.PI) : -vehicleOn.groundVelocity;
				}else{
					return getLongPartOffset() == 0 ? vehicleOn.groundVelocity/(getHeight()*Math.PI) : vehicleOn.groundVelocity;
				}
			}
		}else{
			return 0;
		}
	}
	
	public float getMotiveFriction(){
		return !isFlat ? definition.ground.motiveFriction : definition.ground.motiveFriction/10F;
	}
	
	public float getLateralFriction(){
		return !isFlat ? definition.ground.lateralFriction : definition.ground.lateralFriction/10F;
	}
		
	public float getLongPartOffset(){
		return placementDefinition.extraCollisionBoxOffset != 0 ? placementDefinition.extraCollisionBoxOffset : definition.ground.extraCollisionBoxOffset;
	}
	
	@Override
	public void spawnParticles(){
		if(vehicleOn != null){
			if(contactThisTick){
				for(byte i=0; i<4; ++i){
					InterfaceRender.spawnParticle(new ParticleSmoke(world, position, new Point3d(Math.random()*0.10 - 0.05, 0.15, Math.random()*0.10 - 0.05), 1.0F, 1.0F, 1.0F, 1.0F, 1.0F));
				}
				InterfaceSound.playQuickSound(new SoundInstance(this, MasterLoader.resourceDomain + ":" + "wheel_striking"));
				contactThisTick = false;
			}
			if(skipAngularCalcs && vehicleOn.groundDeviceCollective.groundedGroundDevices.contains(this)){
				for(byte i=0; i<4; ++i){
					InterfaceRender.spawnParticle(new ParticleSmoke(world, position, new Point3d(Math.random()*0.10 - 0.05, 0.15, Math.random()*0.10 - 0.05), 1.0F, 1.0F, 1.0F, 1.0F, 1.0F));
				}
				InterfaceRender.spawnBlockBreakParticles(position.copy().add(0, -1, 0), false);
			}
		}
	}
	
	@Override
	public void save(WrapperNBT data){
		super.save(data);
		data.setBoolean("isFlat", isFlat);
	}
}