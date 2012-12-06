package edu.unca.atjones.MoreEvents;

import java.util.HashMap;

import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class InventoryAddEvent extends Event implements Cancellable{
   private static final HandlerList handlers = new HandlerList();
   private Player player;
   private Inventory inventory;
   private ItemStack item;
   private HashMap<Integer, ItemStack> previous, current;
   private boolean cancelled;

   
    public InventoryAddEvent(Player player, Inventory inventory, ItemStack item, HashMap<Integer, ItemStack> previous, HashMap<Integer, ItemStack> current) {
       this.player = player;
       this.item = item;
       this.previous = previous;
       this.current = current;
       this.inventory = inventory;
    }
    
    public Player getPlayer() {
    	return player;
    }
    
    public Inventory getInventory() {
       return inventory;
    }

    public ItemStack getItemStack() {
       return item; 
    }

    public HashMap<Integer, ItemStack> getPrevious() {
    	return previous;
    }
    
    public HashMap<Integer, ItemStack> getCurrent() {
    	return current;
    }
    
	public boolean isCancelled() {
		return cancelled;
	}

	public void setCancelled(boolean cancel) {
		cancelled = cancel;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
	public static HandlerList getHandlerList() {
	    return handlers;
	}
	
}