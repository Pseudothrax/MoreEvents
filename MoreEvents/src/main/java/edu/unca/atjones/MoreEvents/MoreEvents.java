package edu.unca.atjones.MoreEvents;

import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class MoreEvents extends JavaPlugin implements Listener {

	private MoreEventsLogger logger;
	private PluginManager pm;
	private HashMap<String,Integer> tasks;
	
    @Override
    public void onEnable() {

        saveDefaultConfig();
        
		pm = getServer().getPluginManager();
		pm.registerEvents(this, this);
		logger = new MoreEventsLogger(this);

		
		tasks = new HashMap<String,Integer>();
    }
    
    @Override
    public void onDisable() {
    	getServer().getScheduler().cancelTasks(this);
		pm = null;
    }
    
    @SuppressWarnings("unchecked")
	@EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
    	
    	final Item i = event.getItem();
    	final ItemStack s = i.getItemStack().clone();
    	final Material m = s.getType();
    	final Player p = event.getPlayer();    	
    	final PlayerInventory inv = p.getInventory();
    	
    	//Generate task key
    	String key = p.getName() + ":" + m.toString() + ":" + i.getUniqueId().toString();
    	final String mainKey = "key:" + key;
    	final String timeoutKey = "timeout:" + key;
    	
    	/**
    	 * Fake:
    	 *    The easiest way to determine how the inventory should wind up after adding the given item is
    	 *    to create a fake inventory identical to the player's, add the item, and then use the result.
    	 */
    	Inventory fakeInv = Bukkit.getServer().createInventory(null, InventoryType.PLAYER);
    	ListIterator<ItemStack> inventoryIterator = inv.iterator();
    	while(inventoryIterator.hasNext()) {
    		int index = inventoryIterator.nextIndex();
    		ItemStack stack = inventoryIterator.next();
    		if(stack!=null) fakeInv.setItem(index, stack.clone());
    	}
    	fakeInv.addItem(i.getItemStack().clone());
    	final HashMap<Integer, ItemStack> fake = (HashMap<Integer, ItemStack>) fakeInv.all(m);
    	
    	/**
    	 * Previous:
    	 *   To keep track of what the inventory was like initially, the player's inventory must be duplicated.
    	 *   This is done carefully, cloning each item stack as we go, to avoid issues later.
    	 */
    	HashMap<Integer,ItemStack> temp = (HashMap<Integer,ItemStack>) inv.all(m);
    	Iterator<Integer> keys = temp.keySet().iterator();
    	final HashMap<Integer, ItemStack> previous = new HashMap<Integer, ItemStack>();
    	while(keys.hasNext()) {
    		Integer slot = keys.next();
    		previous.put(slot, temp.get(slot).clone());
    	}
    	
    	//Schedule task to remove all scheduled behavior after timeout period
    	int timeoutTask = getServer().getScheduler().scheduleAsyncDelayedTask(this, new Runnable()
    	{
    		public void run() {
				getServer().getScheduler().cancelTask(tasks.get(mainKey));
				tasks.remove(mainKey);
    		}
    		
		},30);
		tasks.put(timeoutKey, timeoutTask);
		
    	//Schedule Task to repeat until the item is picked up
		int mainTask = getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() 
		{
			
			public void run() {
				if(!i.isValid()) { //if picked up
					
					/**
					 * Current:
					 *   Now that the item has been removed from the world, it is safe to assume that the item 
					 *   has been picked up by the player. The player's current inventory is used to create
					 *   a hash of all relevant stacks, similar to previous and fake.
					 */
					HashMap<Integer, ItemStack> current = (HashMap<Integer, ItemStack>) p.getInventory().all(m);
					
					InventoryAddEvent addEvent = new InventoryAddEvent(p,inv,s,previous,current);
					pm.callEvent(addEvent);
					
					if(addEvent.isCancelled()) { //if Cancelled, undo inventory changes
						Set<Integer> keys = current.keySet();
						Iterator<Integer> keyIterator = keys.iterator();
						while(keyIterator.hasNext()) {
							Integer slot = keyIterator.next();
							ItemStack currSlotStack = current.get(slot).clone();
							int currAmount = currSlotStack.getAmount();
							int prevAmount = 0;
							int fakeAmount = 0;
							
							if(previous.containsKey(slot)) { 
								prevAmount = previous.get(slot).getAmount();
							}
							
							if(fake.containsKey(slot)) { 
								fakeAmount = fake.get(slot).getAmount();
							}

							if(fakeAmount != prevAmount) {
								int newAmount = currAmount - fakeAmount + prevAmount;
								if(newAmount > 0) {
									currSlotStack.setAmount(newAmount);
									inv.setItem(slot, currSlotStack);
								}
								else inv.clear(slot);
							}
						}
					}
					
					getServer().getScheduler().cancelTask(tasks.get(timeoutKey));
					getServer().getScheduler().cancelTask(tasks.get(mainKey));
					tasks.remove(timeoutKey);
					tasks.remove(mainKey);
				}
			}
		}, 10, 10);
		tasks.put(mainKey, mainTask);

		
    }

}
