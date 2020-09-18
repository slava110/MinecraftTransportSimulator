package minecrafttransportsimulator.items.instances;

import java.util.Iterator;
import java.util.List;

import mcinterface.BuilderGUI;
import mcinterface.WrapperEntity;
import mcinterface.WrapperNBT;
import mcinterface.WrapperPlayer;
import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.IItemVehicleInteractable;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;

public class ItemTicket extends AItemBase implements IItemVehicleInteractable{
	
	@Override
	public void addTooltipLines(List<String> tooltipLines, WrapperNBT data){
		for(byte i=1; i<=3; ++i){
			tooltipLines.add(BuilderGUI.translate("info.item.ticket.line" + String.valueOf(i)));
		}
	}
	
	@Override
	public CallbackType doVehicleInteraction(EntityVehicleF_Physics vehicle, APart part, WrapperPlayer player, PlayerOwnerState ownerState, boolean rightClick){
		if(rightClick){
			if(player.isSneaking()){
				Iterator<WrapperEntity> iterator = vehicle.locationRiderMap.inverse().keySet().iterator();
				while(iterator.hasNext()){
					WrapperEntity entity = iterator.next();
					if(!(entity instanceof WrapperPlayer)){
						vehicle.removeRider(entity, iterator);
					}
				}
			}else{
				vehicle.world.loadEntities(new BoundingBox(player.getPosition(), 8D, 8D, 8D), vehicle);
			}
		}
		return CallbackType.NONE;
	}
}
