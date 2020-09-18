package minecrafttransportsimulator.items.components;

import java.util.List;

import mcinterface.WrapperNBT;
import minecrafttransportsimulator.jsondefs.AJSONItem;

/**Base item class for all pack-created items.  Stores information such as the
 * pack the item belongs to and the class that extends {@link AJSONItem} that
 * is the instance of the item's pack.
 * 
 * @author don_bruce
 */
public abstract class AItemPack<JSONDefinition extends AJSONItem<? extends AJSONItem<?>.General>> extends AItemBase{
	public final JSONDefinition definition;
	
	public AItemPack(JSONDefinition definition){
		super();
		this.definition = definition;
	}
	
	@Override
	public String getRegistrationName(){
		//TODO this need to be the full name when packs register with MTS.
		//return definition.packID + ":" + definition.systemName;
		return definition.systemName;
	}
	
	@Override
	public String getItemName(){
        return definition.general.name != null ? definition.general.name : definition.systemName;
	}
	
	@Override
	public void addTooltipLines(List<String> tooltipLines, WrapperNBT data){
		if(definition.general.description != null){
			for(String tooltipLine : definition.general.description.split("\n")){
				tooltipLines.add(tooltipLine);
			}
		}
	}
	
	@Override
	public String getCreativeTabID(){
		return definition.packID;
	}
	
	/**
	 *  Returns the location of the OBJ model for this item, or null if we use the default item rendering system.
	 */
	public abstract String getModelLocation();
	
	/**
	 *  Returns the location of the texture for this item, or null if we use the default item rendering system.
	 */
	public abstract String getTextureLocation();
}
