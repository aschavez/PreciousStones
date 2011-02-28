package net.sacredlabyrinth.Phaed.PreciousStones.listeners;

import java.util.HashSet;
import java.util.LinkedList;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerItemEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.block.Block;

import net.sacredlabyrinth.Phaed.PreciousStones.PreciousStones;
import net.sacredlabyrinth.Phaed.PreciousStones.Helper;
import net.sacredlabyrinth.Phaed.PreciousStones.TargetBlock;
import net.sacredlabyrinth.Phaed.PreciousStones.ChatBlock;
import net.sacredlabyrinth.Phaed.PreciousStones.managers.SettingsManager.FieldSettings;
import net.sacredlabyrinth.Phaed.PreciousStones.vectors.*;

/**
 * PreciousStones player listener
 * 
 * @author Phaed
 */
public class PSPlayerListener extends PlayerListener
{
    private final PreciousStones plugin;
    
    public PSPlayerListener(PreciousStones plugin)
    {
	this.plugin = plugin;
    }
    
    @Override
    public void onPlayerItem(PlayerItemEvent event)
    {
	Player player = event.getPlayer();
	Block block = event.getBlockClicked();
	
	if (block == null || player == null)
	{
	    return;
	}
	
	if (plugin.settings.isBypassBlock(block))
	{
	    return;
	}
	
	if (plugin.settings.isUnbreakableType(block) && plugin.um.isUnbreakable(block))
	{
	    if (plugin.um.isOwner(block, player.getName()) || plugin.settings.publicBlockDetails || plugin.pm.hasPermission(player, "preciousstones.admin.details"))
	    {
		plugin.cm.showUnbreakableDetails(plugin.um.getUnbreakable(block), player);
	    }
	    else
	    {
		plugin.cm.showUnbreakableOwner(player, block);
	    }
	}
	else if (plugin.settings.isFieldType(block) && plugin.ffm.isField(block))
	{
	    if (plugin.ffm.isAllowed(block, player.getName()) || plugin.settings.publicBlockDetails || plugin.pm.hasPermission(player, "preciousstones.admin.details"))
	    {
		plugin.cm.showFieldDetails(plugin.ffm.getField(block), player);
	    }
	    else
	    {
		plugin.cm.showFieldOwner(player, block);
	    }
	}
	else
	{
	    Field field = plugin.ffm.isDestroyProtected(block, null);
	    
	    if (field != null)
	    {
		if (plugin.ffm.isAllowed(block, player.getName()) || plugin.settings.publicBlockDetails)
		{
		    LinkedList<Field> fields = plugin.ffm.getSourceFields(block);
		    
		    plugin.cm.showProtectedLocation(fields, player);
		}
		else
		{
		    plugin.cm.showProtected(player);
		}
	    }
	}
    }
    
    @Override
    public void onPlayerMove(PlayerMoveEvent event)
    {
	Player player = event.getPlayer();
	
	// handle entries and exits from fields
	
	boolean insideField = false;
	
	LinkedList<Field> fields = plugin.ffm.getSourceFields(player);
	
	for (Field field : fields)
	{
	    FieldSettings fieldsettings = plugin.settings.getFieldSettings(field);
	    
	    if (plugin.em.isInsideField(player))
	    {
		Field previousField = plugin.em.getEnvelopingField(player);
		
		if (previousField.getOwner().equals(field.getOwner()))
		{
		    insideField = true;
		}
		else
		{
		    plugin.em.leave(player);
		}
		
		continue;
	    }
	    
	    plugin.em.enter(player, field);
	    
	    if (fieldsettings.welcomeMessage)
	    {
		plugin.cm.showWelcomeMessage(player, field.getName());
	    }
	    
	    insideField = true;
	    break;
	}
	
	if (!insideField && plugin.em.isInsideField(player))
	{
	    Field field = plugin.em.getEnvelopingField(player);
	    
	    FieldSettings fieldsettings = plugin.settings.getFieldSettings(field);
	    
	    if (fieldsettings.farewellMessage)
	    {
		plugin.cm.showFarewellMessage(player, field.getName());
	    }
	    
	    plugin.em.leave(player);
	}
	
	// check if were on a prevent entry field, only those not owned by player
	
	fields = plugin.ffm.getSourceFields(player, player.getName());
	
	for (Field field : fields)
	{
	    FieldSettings fieldsettings = plugin.settings.getFieldSettings(field);
	    
	    if (!plugin.pm.hasPermission(player, "preciousstones.bypass.entry"))
	    {
		if (fieldsettings.preventEntry)
		{
		    player.teleportTo(event.getFrom());
		    event.setCancelled(true);
		    plugin.cm.warnEntry(player, field);
		    break;
		}
	    }
	}
    }
    
    @Override
    public void onPlayerCommand(PlayerChatEvent event)
    {
	String[] split = event.getMessage().split(" ");
	Player player = event.getPlayer();
	Block block = player.getWorld().getBlockAt(player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ());
	
	if (split[0].equalsIgnoreCase("/ps") && plugin.pm.hasPermission(player, "preciousstones.benefit.ps"))
	{
	    event.setCancelled(true);
	    
	    if (split.length > 1)
	    {
		if (split[1].equals("allow") && plugin.pm.hasPermission(player, "preciousstones.whitelist.allow"))
		{
		    if (split.length == 3)
		    {
			Field field = plugin.ffm.inOneAllowedVector(block, player);
			
			if (field != null)
			{
			    String playerName = split[2];
			    
			    int count = plugin.ffm.addAllowed(player, field, playerName);
			    
			    if (count > 0)
			    {
				ChatBlock.sendMessage(player, ChatColor.AQUA + playerName + " was added to the allowed list of " + count + " force-fields");
			    }
			    else
			    {
				ChatBlock.sendMessage(player, ChatColor.AQUA + playerName + " is already on the list");
			    }
			}
			else
			{
			    plugin.cm.showNotFound(player);
			}
			
			return;
		    }
		}
		else if (split[1].equals("remove") && plugin.pm.hasPermission(player, "preciousstones.whitelist.remove"))
		{
		    if (split.length == 3)
		    {
			Field field = plugin.ffm.inOneAllowedVector(block, player);
			
			if (field != null)
			{
			    String playerName = split[2];
			    
			    int count = plugin.ffm.removeAllowed(player, field, playerName);
			    
			    if (count > 0)
			    {
				ChatBlock.sendMessage(player, ChatColor.AQUA + playerName + " was removed from the allowed list of " + count + " force-fields");
			    }
			    else
			    {
				ChatBlock.sendMessage(player, ChatColor.RED + playerName + " not found or is the last player on the list");
			    }
			}
			else
			{
			    plugin.cm.showNotFound(player);
			}
			
			return;
		    }
		}
		else if (split[1].equals("setname") && plugin.pm.hasPermission(player, "preciousstones.benefit.setname"))
		{
		    if (split.length >= 3)
		    {
			String playerName = "";
			
			for (int i = 2; i < split.length; i++)
			{
			    playerName += split[i] + " ";
			}
			playerName = playerName.trim();
			
			if (playerName.length() > 0)
			{
			    Field field = plugin.ffm.inOneAllowedVector(block, player);
			    
			    if (field != null)
			    {
				int count = plugin.ffm.setNameFields(player, field, playerName);
				
				if (count > 0)
				{
				    ChatBlock.sendMessage(player, ChatColor.AQUA + "Renamed " + count + " force-fields to " + playerName);
				}
				else
				{
				    plugin.cm.showNotFound(player);
				}
				return;
			    }
			    else
			    {
				plugin.cm.showNotFound(player);
			    }
			    return;
			}
		    }
		}
		else if (split[1].equals("delete") && plugin.pm.hasPermission(player, "preciousstones.admin.delete"))
		{
		    if (split.length == 2)
		    {
			Field field = plugin.ffm.inOneAllowedVector(block, player);
			
			if (field != null)
			{
			    int count = plugin.ffm.deleteFields(player, field);
			    
			    if (count > 0)
			    {
				ChatBlock.sendMessage(player, ChatColor.AQUA + "Protective field removed from " + count + " force-fields");
				
				if (plugin.settings.logBypassDelete)
				{
				    PreciousStones.log.info("[ps] Protective field removed from " + count + " force-fields by " + player.getName() + " near " + field.toString());
				}
			    }
			    else
			    {
				plugin.cm.showNotFound(player);
			    }
			}
			else
			{
			    plugin.cm.showNotFound(player);
			}
			
			return;
		    }
		    else if (split.length == 3)
		    {
			if (Helper.isInteger(split[2]))
			{
			    LinkedList<Field> fields = plugin.ffm.getFieldsOfType(Integer.parseInt(split[2]), player.getWorld());
			    
			    for (Field field : fields)
			    {
				plugin.ffm.release(field);
			    }
			    
			    if (fields.size() > 0)
			    {
				ChatBlock.sendMessage(player, ChatColor.AQUA + "Protective field removed from " + fields.size() + " force-fields of type " + split[2]);
				
				if (plugin.settings.logBypassDelete)
				{
				    PreciousStones.log.info("[ps] Protective field removed from " + fields.size() + " force-fields of type " + split[2] + " by " + player.getName() + " near " + fields.get(0).toString());
				}
			    }
			    else
			    {
				plugin.cm.showNotFound(player);
			    }
			    return;
			}
		    }
		}
		else if (split[1].equals("setowner") && plugin.pm.hasPermission(player, "preciousstones.admin.setowner"))
		{
		    if (split.length == 3)
		    {
			String owner = split[2];
			
			TargetBlock tb = new TargetBlock(player, 100, 0.2, new int[] { 0, 6, 8, 9, 37, 38, 39, 40, 50, 51, 55, 59, 63, 68, 69, 70, 72, 75, 76, 83, 85 });
			
			if (tb != null)
			{
			    Block targetblock = tb.getTargetBlock();
			    
			    if (targetblock != null)
			    {
				if (plugin.settings.isUnbreakableType(targetblock))
				{
				    Unbreakable unbreakable = plugin.um.getUnbreakable(targetblock);
				    
				    if (unbreakable != null)
				    {
					unbreakable.setOwner(owner);
					ChatBlock.sendMessage(player, ChatColor.AQUA + "Owner set to " + owner);
					return;
				    }
				}
				
				if (plugin.settings.isFieldType(targetblock))
				{
				    Field field = plugin.ffm.getField(targetblock);
				    
				    if (field != null)
				    {
					field.setOwner(owner);
					ChatBlock.sendMessage(player, ChatColor.AQUA + "Owner set to " + owner);
					return;
				    }
				}
			    }
			}
			
			ChatBlock.sendMessage(player, ChatColor.AQUA + "You are not pointing at a force-field or unbreakable block");
			return;
		    }
		}
		else if (split[1].equals("list") && plugin.pm.hasPermission(player, "preciousstones.admin.list"))
		{
		    if (split.length == 3)
		    {
			if (Helper.isInteger(split[2]))
			{
			    LinkedList<Unbreakable> unbreakables = plugin.um.getUnbreakablesInArea(player, Integer.parseInt(split[2]));
			    LinkedList<Field> fields = plugin.ffm.getFieldsInArea(player, Integer.parseInt(split[2]));
			    
			    for (Unbreakable u : unbreakables)
			    {
				ChatBlock.sendMessage(player, ChatColor.AQUA + u.toString());
			    }
			    
			    for (Field f : fields)
			    {
				ChatBlock.sendMessage(player, ChatColor.AQUA + f.toString());
			    }
			    
			    if (unbreakables.size() == 0 && fields.size() == 0)
			    {
				ChatBlock.sendMessage(player, ChatColor.AQUA + "No force-field or unbreakable blocks found");
			    }
			    return;
			}
		    }
		}
		else if (split[1].equals("info") && plugin.pm.hasPermission(player, "preciousstones.admin.info"))
		{
		    if (split.length == 2)
		    {
			LinkedList<Field> fields = plugin.ffm.getSourceFields(block);
			
			for (Field field : fields)
			{
			    plugin.cm.showFieldDetails(field, player);
			}
			
			if (fields.size() == 0)
			{
			    plugin.cm.showNotFound(player);
			}
			return;
		    }
		}
		else if (split[1].equals("reload") && plugin.pm.hasPermission(player, "preciousstones.admin.reload"))
		{
		    if (split.length == 2)
		    {
			plugin.settings.loadConfiguration();
			
			ChatBlock.sendMessage(player, ChatColor.AQUA + "Configuration reloaded");
			return;
		    }
		}
		else if (split[1].equals("save") && plugin.pm.hasPermission(player, "preciousstones.admin.save"))
		{
		    if (split.length == 2)
		    {
			plugin.sm.save();
			
			ChatBlock.sendMessage(player, ChatColor.AQUA + "PStones saved to files");
			return;
		    }
		}
		else if (split[1].equals("allowall") && plugin.pm.hasPermission(player, "preciousstones.whitelist.allowall"))
		{
		    if (split.length == 3)
		    {
			String playerName = split[2];
			
			int count = plugin.ffm.allowAll(player, playerName);
			
			if (count > 0)
			{
			    ChatBlock.sendMessage(player, ChatColor.AQUA + playerName + " was added to the allowed list of " + count + " force-fields");
			}
			else
			{
			    ChatBlock.sendMessage(player, ChatColor.AQUA + playerName + " is already on all your lists");
			}
			
			return;
		    }
		}
		else if (split[1].equals("removeall") && plugin.pm.hasPermission(player, "preciousstones.whitelist.removeall"))
		{
		    if (split.length == 3)
		    {
			String playerName = split[2];
			
			int count = plugin.ffm.removeAll(player, playerName);
			
			if (count > 0)
			{
			    ChatBlock.sendMessage(player, ChatColor.AQUA + playerName + " was removed from the allowed list of " + count + " force-fields");
			}
			else
			{
			    ChatBlock.sendMessage(player, ChatColor.AQUA + playerName + " is not in any of your lists");
			}
			
			return;
		    }
		}
		else if (split[1].equals("allowed") && plugin.pm.hasPermission(player, "preciousstones.whitelist.allowed"))
		{
		    if (split.length == 2)
		    {
			Field field = plugin.ffm.inOneAllowedVector(block, player);
			
			if (field != null)
			{
			    HashSet<String> allallowed = plugin.ffm.getAllAllowed(player, field);
			    
			    if (allallowed.size() > 0)
			    {
				String out = "";
				
				for (String i : allallowed)
				{
				    out += ", " + i;
				}
				
				ChatBlock.sendMessage(player, ChatColor.AQUA + "Allowed: " + out.substring(2));
			    }
			    else
			    {
				ChatBlock.sendMessage(player, ChatColor.RED + "No players allowed in this force-field");
			    }
			}
			else
			{
			    plugin.cm.showNotFound(player);
			}
			
			return;
		    }
		}
		else if (split[1].equals("who") && plugin.pm.hasPermission(player, "preciousstones.benefit.who"))
		{
		    if (split.length == 2)
		    {
			Field field = plugin.ffm.inOneAllowedVector(block, player);
			
			if (field != null)
			{
			    HashSet<String> inhabitants = plugin.ffm.getWho(player, field);
			    
			    if (inhabitants.size() > 0)
			    {
				String out = "";
				
				for (String i : inhabitants)
				{
				    out += ", " + i;
				}
				
				ChatBlock.sendMessage(player, ChatColor.AQUA + "Inhabitants: " + out.substring(2));
			    }
			    else
			    {
				ChatBlock.sendMessage(player, ChatColor.RED + "No players found in these overlapped force-fields");
			    }
			}
			else
			{
			    plugin.cm.showNotFound(player);
			}
			
			return;
		    }
		}
		else if (split[1].equals("on") && plugin.pm.hasPermission(player, "preciousstones.benefit.onoff"))
		{
		    if(plugin.plm.isDisabled(player))
		    {
			plugin.plm.setDisabled(player, false);
			ChatBlock.sendMessage(player, ChatColor.AQUA + "Enabled the placing of pstones");
		    }
		    else
		    {
			ChatBlock.sendMessage(player, ChatColor.RED + "Pstone placement is already enabled");
		    }
		    
		    return;
		}
		else if (split[1].equals("off") && plugin.pm.hasPermission(player, "preciousstones.benefit.onoff"))
		{
		    if(!plugin.plm.isDisabled(player))
		    {
			plugin.plm.setDisabled(player, true);
			ChatBlock.sendMessage(player, ChatColor.AQUA + "Disabled the placing of pstones");
		    }
		    else
		    {
			ChatBlock.sendMessage(player, ChatColor.RED + "Pstone placement is already disabled");
		    }
		    
		    return;
		}
	    }
	    
	    ChatBlock.sendBlank(player);
	    ChatBlock.sendMessage(player, ChatColor.YELLOW + plugin.getDescription().getName() + " " + plugin.getDescription().getVersion());
	    
	    if (plugin.pm.hasPermission(player, "preciousstones.whitelist.allow"))
	    {
		ChatBlock.sendMessage(player, ChatColor.AQUA + "/ps allow [player] " + ChatColor.GRAY + "- Add player to overlapping fields");
	    }
	    
	    if (plugin.pm.hasPermission(player, "preciousstones.whitelist.allowall"))
	    {
		ChatBlock.sendMessage(player, ChatColor.AQUA + "/ps allowall [player] " + ChatColor.GRAY + "- Add player to all your fields");
	    }
	    
	    if (plugin.pm.hasPermission(player, "preciousstones.whitelist.allowed"))
	    {
		ChatBlock.sendMessage(player, ChatColor.AQUA + "/ps allowed " + ChatColor.GRAY + "- List all allowed players in overlapping fields");
	    }
	    
	    if (plugin.pm.hasPermission(player, "preciousstones.whitelist.remove"))
	    {
		ChatBlock.sendMessage(player, ChatColor.AQUA + "/ps remove [player] " + ChatColor.GRAY + "- Remove player from overlapping fields");
	    }
	    
	    if (plugin.pm.hasPermission(player, "preciousstones.whitelist.removeall"))
	    {
		ChatBlock.sendMessage(player, ChatColor.AQUA + "/ps removeall [player] " + ChatColor.GRAY + "- Remove player from all your fields");
	    }
	    
	    if (plugin.pm.hasPermission(player, "preciousstones.benefit.who"))
	    {
		ChatBlock.sendMessage(player, ChatColor.AQUA + "/ps who " + ChatColor.GRAY + "- List all inhabitants inside the overlapping fields");
	    }
	    
	    if (plugin.settings.haveNameable() && plugin.pm.hasPermission(player, "preciousstones.benefit.setname"))
	    {
		ChatBlock.sendMessage(player, ChatColor.AQUA + "/ps setname [name] " + ChatColor.GRAY + "- Set the name of force-fields");
	    }
	    
	    if (plugin.pm.hasPermission(player, "preciousstones.benefit.onoff"))
	    {
		ChatBlock.sendMessage(player, ChatColor.AQUA + "/ps [on|off] " + ChatColor.GRAY + "- Disable or re-eable placing of pstones");
	    }
	    
	    if (plugin.pm.hasPermission(player, "preciousstones.admin.delete"))
	    {
		ChatBlock.sendMessage(player, ChatColor.DARK_RED + "/ps delete " + ChatColor.GRAY + "- Delete the field(s) you're standing on");
		ChatBlock.sendMessage(player, ChatColor.DARK_RED + "/ps delete [blockid] " + ChatColor.GRAY + "- Delete the field(s) from this type");
	    }
	    
	    if (plugin.pm.hasPermission(player, "preciousstones.admin.info"))
	    {
		ChatBlock.sendMessage(player, ChatColor.DARK_RED + "/ps info " + ChatColor.GRAY + "- Get info for the field youre standing on");
	    }
	    
	    if (plugin.pm.hasPermission(player, "preciousstones.admin.list"))
	    {
		ChatBlock.sendMessage(player, ChatColor.DARK_RED + "/ps list [chunks-in-radius]" + ChatColor.GRAY + "- Lists all pstones in area");
	    }
	    
	    if (plugin.pm.hasPermission(player, "preciousstones.admin.setowner"))
	    {
		ChatBlock.sendMessage(player, ChatColor.DARK_RED + "/ps setowner [player] " + ChatColor.GRAY + "- Of the block you're pointing at");
	    }
	    
	    if (plugin.pm.hasPermission(player, "preciousstones.admin.reload"))
	    {
		ChatBlock.sendMessage(player, ChatColor.DARK_RED + "/ps reload " + ChatColor.GRAY + "- Reload configuraton file");
	    }

	    if (plugin.pm.hasPermission(player, "preciousstones.admin.save"))
	    {
		ChatBlock.sendMessage(player, ChatColor.DARK_RED + "/ps save " + ChatColor.GRAY + "- Save force field files");
	    }
	}
    }
}
