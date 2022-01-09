package minecrafttransportsimulator.guis.components;

import java.util.List;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.ColorRGB;
import minecrafttransportsimulator.mcinterface.InterfaceEventsModelLoader;
import minecrafttransportsimulator.rendering.instances.RenderText;
import minecrafttransportsimulator.rendering.instances.RenderText.TextAlignment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;

/**Custom item render class.  This class is designed to render a {@link ItemStack} 
 * or list of stacks into the GUI.  This allows us to use a simple string 
 * name for the render rather than a bunch of MC calls.
 * Note that this component does not get a stack assigned during construction: you must
 * assign it manually either by setting {@link #stack} for a single stack rendering operation,
 * or {@link #stacks} for a cyclclic rendering operation.  This allows for switching items in GUIs.
 * This is especially useful in crafting GUIs, where you want a static set of item components
 * that switch their states depending on other selections.  The scale is based on the assumption that
 * a single item is 16x16px.
 *
 * @author don_bruce
 */
public class GUIComponentItem extends AGUIComponent{		
	public final float scale;
	public ItemStack stack;
	public List<ItemStack> stacks;
	private ItemStack stackToRender;
	private ItemStack lastStackRendered;
	
	/**Default item constructor.**/
	public GUIComponentItem(int x, int y, float scale){
		super(x, y, (int) (16*scale), (int) (16*scale));
		this.scale = scale;
		//Items are normally rendered with origin at bottom-right like normal models.
        //The 16 y-offset moves them to top-left orientation.
		this.textPosition.set(position.x + scale*16, position.y - 16F*scale + scale*8, textPosition.z);
	}
	
	/**Constructor for an item linked with a button.  Button is assumed to be 18x18px so item will be offset 1px to center.**/
	public GUIComponentItem(GUIComponentButton linkedButton){
		this(linkedButton.constructedX + 1, linkedButton.constructedY + 1, 1.0F);
	}
	
	@Override
	public int getZOffset(){
		return MODEL_DEFAULT_ZOFFSET;
	}

    @Override
	public void render(AGUIBase gui, int mouseX, int mouseY, boolean renderBright, boolean renderLitTexture, boolean blendingEnabled, float partialTicks){
    	if(stack != null){
    		stackToRender = stack;
    	}else if(stacks != null && !stacks.isEmpty()){
    		stackToRender = stacks.get((int) (System.currentTimeMillis()%(stacks.size()*500)/500));
    	}else{
    		stackToRender = null;
    	}
    	
    	if(stackToRender != null && !stackToRender.isEmpty()){
    		if(!stackToRender.equals(lastStackRendered)){
	    		renderable = InterfaceEventsModelLoader.getItemModel(stackToRender);
	    		lastStackRendered = stackToRender;
	    		if(stackToRender.getCount() > 1){
	    			text = String.valueOf(RenderText.FORMATTING_CHAR) + String.valueOf(RenderText.BOLD_FORMATTING_CHAR) + String.valueOf(stackToRender.getCount());
	    		}
    		}
    	}else{
    		renderable = null;
    		lastStackRendered = null;
    		text = null;
    	}
    	
    	if(renderable != null){
    		GL11.glPushMatrix();
    		
    		//Translate to position.
    		//Items are normally rendered with origin at bottom-right like normal models.
            //The 16 y-offset moves them to top-left orientation.
			GL11.glTranslated(position.x, position.y - 16F*scale, position.z);
			
			//Apply scale, but also scale up the model by 16x.
			//It's normally 1 unit -> 1 block, not 1px, and we want 16px default.
			GL11.glScalef(scale*16, scale*16, scale*16);
	        		
    		//Render.
			renderable.disableLighting = renderBright || ignoreGUILightingState;
			renderable.render();
			
			GL11.glPopMatrix();
    	}
    }
    
    @Override
    public void renderText(boolean renderTextLit){
    	RenderText.drawText(text, null, textPosition, null, ColorRGB.WHITE, TextAlignment.RIGHT_ALIGNED, scale, false, 0, 1.0F, renderTextLit);
    }
    
    @Override
	public List<String> getTooltipText(){
    	if(stackToRender != null && !stackToRender.isEmpty()){
    		Minecraft mc = Minecraft.getMinecraft();
			List<String> tooltipText = stackToRender.getTooltip(mc.player, mc.gameSettings.advancedItemTooltips ? ITooltipFlag.TooltipFlags.ADVANCED : ITooltipFlag.TooltipFlags.NORMAL);
	        for(int i = 0; i < tooltipText.size(); ++i){
	            if(i != 0){
	            	//Add grey formatting text to non-first line tooltips.
	            	tooltipText.set(i, TextFormatting.GRAY + tooltipText.get(i));
	            }
	        }
	        return tooltipText;
    	}else{
    		return null;
    	}
    }
}
