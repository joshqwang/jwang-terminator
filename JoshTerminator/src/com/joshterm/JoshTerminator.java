
package com.joshterm;

import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.Iterator;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.HashSet;
import org.bukkit.plugin.Plugin;
import java.util.UUID;
import java.util.Set;
import org.bukkit.command.CommandExecutor;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.Random;
public class JoshTerminator extends JavaPlugin implements Listener, CommandExecutor
{
    private Set<UUID> terminators;
    private long last;
    private boolean dead;
    private int deadTimer;
    public JoshTerminator() {
        this.last = 0L;
        this.dead = false;
        this.deadTimer = 2400;
    }
    
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents((Listener)this, (Plugin)this);
        this.terminators = new HashSet<UUID>();
        this.startRunnable();
    }
    
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (command.getName().equalsIgnoreCase("terminator")) {
            if (args.length != 2) {
                this.sendInvalid(sender);
                return false;
            }
            final Player player = Bukkit.getPlayer(args[1]);
            if (player == null) {
                sender.sendMessage(ChatColor.RED + "Player not found.");
                return false;
            }
            if (args[0].equalsIgnoreCase("add")) {
                this.terminators.add(player.getUniqueId());
                sender.sendMessage(ChatColor.GREEN + player.getName() + " is now a terminator.");
            }
            else if (args[0].equalsIgnoreCase("remove")) {
                this.terminators.remove(player.getUniqueId());
                sender.sendMessage(ChatColor.GREEN + player.getName() + " is no longer a terminator.");
            }
            else {
                this.sendInvalid(sender);
            }
        }
        return false;
    }
    
    private void sendInvalid(final CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "Invalid usage. Please use:");
        sender.sendMessage(ChatColor.RED + "/terminator add <name>");
        sender.sendMessage(ChatColor.RED + "/terminator remove <name>");
    }
    
    private void startRunnable() {
        new BukkitRunnable() {
            public void run() {
            	if(dead) {
	            	deadTimer -= 200;
	            	if(deadTimer <= 0) {
	            		dead = false;
	            		deadTimer = 2400;
	            		Bukkit.broadcastMessage("The Terminator has been revived!");
	            	}
            	}
                for (final UUID uuid : JoshTerminator.this.terminators) {
                    final Player terminator = Bukkit.getPlayer(uuid);
                    if (terminator == null) {
                        continue;
                    }
                    Player nearest = null;
                    double nearestDistance = Double.MAX_VALUE;
                    for (final Player player : Bukkit.getOnlinePlayers()) {
                        if (!JoshTerminator.this.terminators.contains(player.getUniqueId())) {
                            if (!player.getWorld().equals(terminator.getWorld())) {
                                continue;
                            }
                            final double dist = player.getLocation().distanceSquared(terminator.getLocation());
                            if (dist >= nearestDistance) {
                                continue;
                            }
                            nearestDistance = dist;
                            nearest = player;
                        }
                    }
                    if (nearest == null && Bukkit.getOnlinePlayers().size() > 1) {
                    	Random rand = new Random();
                    	Player teleportee = null;
                    	ArrayList<Player> players = new ArrayList<Player>(Bukkit.getOnlinePlayers());
                    	while(teleportee == null) {
                    		teleportee = players.get(rand.nextInt(players.size()));
                    		if(teleportee.equals(terminator)) {
                    			teleportee = null;
                    		}
                    	}
                    	terminator.teleport(teleportee);
                    	terminator.setHealth(20);
                		dead = true;
                		Bukkit.broadcastMessage("The terminator jumped dimensions!");
                        return;
                    }
                    Location nearestMatchY = nearest.getLocation();
                    nearestMatchY.setY(terminator.getLocation().getY());
                    nearestDistance = terminator.getLocation().distanceSquared(nearestMatchY);
                    if (nearestDistance <= 8100.0) {
                    	 
                    	 continue;
                    }
                    final Location location = terminator.getLocation().add(nearestMatchY.toVector().subtract(terminator.getLocation().toVector()).normalize().multiply(Math.max((Math.sqrt(nearestDistance) - 90)/1.2, 10)));
                    location.setY(nearest.getLocation().getY());
                    terminator.teleport(location);
                   
                }
            }
        }.runTaskTimer((Plugin)this, 0L, 50L);
    }
    
    @EventHandler
    public void onEntityDamageEvent(final EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            final Player terminator = (Player)event.getEntity();
            if (this.terminators.contains(terminator.getUniqueId())) {
            	Player nearest = getNearestPlayer(terminator.getLocation(), terminator, false);
            	if(nearest != null) {
            		if(nearest.getLocation().distanceSquared(terminator.getLocation()) > 1600) {
            			event.setCancelled(true);
            		}
            	}
            	if(this.dead) {
            		event.setCancelled(true);
            	}
            	else if(terminator.getHealth() <= event.getDamage()/2 + 0.5) {
            		terminator.setHealth(20);
            		dead = true;
            		event.setCancelled(true);
            		Bukkit.broadcastMessage("The terminator was killed!");
            	}
            	else {
            		if(event.getCause() == EntityDamageEvent.DamageCause.LAVA || event.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION || event.getCause() == EntityDamageEvent.DamageCause.DROWNING || event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            			event.setDamage(event.getDamage()/10);
            		}
            		else {
                		event.setDamage(event.getDamage()/2);

            		}
            	}
            }
        }
    }
    @EventHandler
    public void onTerminatorAttack(EntityDamageByEntityEvent event) {
        Entity e = event.getDamager();
        if (e instanceof Player) {
           if(dead && terminators.contains(e.getUniqueId())) {
        	   event.setCancelled(true);
           }
        }
    }
    @EventHandler
    public void onEndermanDeath(EntityDeathEvent event) {
        Entity e = event.getEntity();
        if (e instanceof Enderman) {
           event.getDrops().add(new ItemStack(Material.ENDER_PEARL, 1));       
        }
    }
    @EventHandler
    public void onCompassRightClick(PlayerInteractEvent event) {
    	Player p = event.getPlayer();
    	 
    	if(p.getItemInHand().getType() == Material.COMPASS){
    	    p.openInventory(inv(p));
    	}
    
    	
    }
    public Inventory inv (Player p) {
    	Inventory inv = Bukkit.createInventory(null, 9);
    	for(Player player: Bukkit.getOnlinePlayers()) {
    		if(this.terminators.contains(player.getUniqueId())) continue;
    		if(p.equals(player)) continue;
    		ItemStack item = new ItemStack(Material.PLAYER_HEAD);
    		SkullMeta skull = (SkullMeta) item.getItemMeta();
    		skull.setOwningPlayer(player);
    		skull.setDisplayName(player.getName());
    		item.setItemMeta(skull);
    		inv.addItem(item);
    	}
    	return inv;
    }
    @EventHandler
    public void onInventoryClick(final InventoryClickEvent e) {
        
        
        final ItemStack clickedItem = e.getCurrentItem();
        if ( clickedItem.getType() == Material.PLAYER_HEAD) e.setCancelled(true);
       
        // verify current item is not null
        if (clickedItem == null || clickedItem.getType() == Material.AIR || clickedItem.getType() != Material.PLAYER_HEAD) {
        	
        	return;
        }

        final Player p = (Player) e.getWhoClicked();
        Player teleportee = Bukkit.getPlayer(clickedItem.getItemMeta().getDisplayName());
        p.teleport(teleportee.getLocation());
    }
   
    @EventHandler
    public void onEntityDamageEvent(final FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            final Player terminator = (Player)event.getEntity();
            if (this.terminators.contains(terminator.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler
    public void onPlayerMoveEvent(final PlayerMoveEvent event) {
            if (this.terminators.contains(event.getPlayer().getUniqueId()) && dead) {
                event.setCancelled(true);
            }
        
    }
    
    @EventHandler
    public void onBlockPlaceEvent(final BlockPlaceEvent event) {
        if (this.terminators.contains(event.getPlayer().getUniqueId())) {
            event.getItemInHand().setAmount(64);
        }
    }
    
    @EventHandler
    public void onPlayerItemDamageEvent(final PlayerItemDamageEvent event) {
        if (this.terminators.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onEntityPickupItemEvent(final EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player) {
            final Player terminator = (Player)event.getEntity();
            if (this.terminators.contains(terminator.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler
    public void onEntityDropItemEvent(final PlayerDropItemEvent event) {
        if (this.terminators.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onPlayerPortalEvent(final PlayerPortalEvent event) {
        if (event.getTo() == null) {
            return;
        }
        if (System.currentTimeMillis() - this.last > 30000L) {
            final Player terminator = this.getNearestPlayer(event.getFrom(), event.getPlayer(), true);
            if (terminator != null && event.getPlayer().equals(this.getNearestPlayer(terminator.getLocation(), terminator, false))) {
                final Location from = event.getFrom();
                if (!from.getWorld().equals(terminator.getWorld())) {
                    return;
                }
                final int seconds = (int)Math.max(from.distance(terminator.getLocation()) / 5.0, 3.0);
                final Location to = event.getTo().clone();
                this.last = System.currentTimeMillis();
                new BukkitRunnable() {
                    public void run() {
                        terminator.teleport(to);
                    }
                }.runTaskLater((Plugin)this, (long)(20 * seconds));
            }
        }
    }
    
    private Player getNearestPlayer(final Location location, final Player ignore, final boolean terminator) {
        Player nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (final Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (!onlinePlayer.equals(ignore)) {
                if (!onlinePlayer.getWorld().equals(location.getWorld())) {
                    continue;
                }
                if (terminator && !this.terminators.contains(onlinePlayer.getUniqueId())) {
                    continue;
                }
                if (!terminator && this.terminators.contains(onlinePlayer.getUniqueId())) {
                    continue;
                }
                final double dist = onlinePlayer.getLocation().distanceSquared(location);
                if (dist >= nearestDistance) {
                    continue;
                }
                nearestDistance = dist;
                nearest = onlinePlayer;
            }
        }
        return nearest;
    }
}
