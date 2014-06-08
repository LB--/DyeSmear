package com.lb_stuff.dyesmear;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.entity.Player;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.DyeColor;
import org.bukkit.material.MaterialData;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.*;
import java.io.*;
import org.bukkit.event.block.Action;

public class DyeSmear extends JavaPlugin implements Listener
{
	@Override
	public void onEnable()
	{
		getCommand("smear").setExecutor(new SmearCommand(this));
		getServer().getPluginManager().registerEvents(this, this);
	}
	@Override
	public void onDisable()
	{
		//
	}

	private boolean canInteract(Material m)
	{
		switch(m)
		{
			case GLASS:
			case STAINED_GLASS:
			case THIN_GLASS:
			case STAINED_GLASS_PANE:
			case HARD_CLAY:
			case STAINED_CLAY:
				return true;
		}
		return false;
	}
	private boolean canScrape(Material m)
	{
		switch(m)
		{
			case STAINED_GLASS:
			case STAINED_GLASS_PANE:
			case STAINED_CLAY:
				return true;
		}
		return false;
	}
	private Material upgrade(Material m)
	{
		switch(m)
		{
			case GLASS:
				return Material.STAINED_GLASS;
			case THIN_GLASS:
				return Material.STAINED_GLASS_PANE;
			case HARD_CLAY:
				return Material.STAINED_CLAY;
		}
		return m;
	}
	private Material downgrade(Material m)
	{
		switch(m)
		{
			case STAINED_GLASS:
				return Material.GLASS;
			case STAINED_GLASS_PANE:
				return Material.THIN_GLASS;
			case STAINED_CLAY:
				return Material.HARD_CLAY;
		}
		return m;
	}

	private Map<Player, Set<Block>> smears = new HashMap<>();
	public void cancelSmear(Player p)
	{
		if(smears.containsKey(p))
		{
			smears.remove(p);
		}
	}
	private Set<Block> validateBlocks(Player p)
	{
		if(!smears.containsKey(p)) smears.put(p, new HashSet<Block>());
		Set<Block> blocks = smears.get(p);
		for(Iterator<Block> it = blocks.iterator(); it.hasNext(); )
		{
			Block b = it.next();
			if(!canInteract(b.getType()))
			{
				it.remove();
			}
			else
			{
				BlockBreakEvent bbe = new BlockBreakEvent(b, p);
				getServer().getPluginManager().callEvent(bbe);
				if(bbe.isCancelled())
				{
					it.remove();
				}
			}
		}
		return blocks;
	}
	private void addBlock(Player p, Block b)
	{
		if(!smears.containsKey(p)) smears.put(p, new HashSet<Block>());
		smears.get(p).add(b);
		validateBlocks(p);
	}
	private void scrape(Player p, boolean remove)
	{
		Map<DyeColor, Integer> colors = new HashMap<>();
		Set<Block> blocks = smears.get(p);
		for(Iterator<Block> it = blocks.iterator(); it.hasNext(); )
		{
			Block b = it.next();
			DyeColor c = DyeColor.getByWoolData(b.getData());
			if(!colors.containsKey(c)) colors.put(c, 0);
			if(!downgrade(b.getType()).equals(b.getType()))
			{
				colors.put(c, colors.get(c)+1);
				b.setType(downgrade(b.getType()));
			}
			if(remove) it.remove();
		}
		for(Map.Entry<DyeColor, Integer> color : colors.entrySet())
		{
			if(color.getValue() >= 8)
			{
				ItemStack result = new ItemStack(Material.INK_SACK, color.getValue()/8, color.getKey().getDyeData());
				p.getWorld().dropItem(p.getLocation(), result).setPickupDelay(0);
			}
		}
	}
	private void smear(Player p, ItemStack item)
	{
		DyeColor c = DyeColor.getByDyeData(item.getData().getData());
		int amount = item.getAmount();
		int dyes = 0;
		Set<Block> blocks = smears.get(p);
		for(Iterator<Block> it = blocks.iterator(); it.hasNext(); )
		{
			Block b = it.next();
			if(dyes <= 0)
			{
				if(--amount < 0)
				{
					break;
				}
				dyes += 8;
			}
			if(!upgrade(b.getType()).equals(b.getType()))
			{
				b.setType(upgrade(b.getType()));
				b.setData(c.getWoolData());
				--dyes;
			}
			it.remove();
		}
		if(amount <= 0)
		{
			item.setType(Material.AIR);
		}
		item.setAmount(amount);
	}

	@EventHandler
	public void onBlockInteract(PlayerInteractEvent e)
	{
		Player player = e.getPlayer();
		if(e.hasItem() && e.hasBlock())
		{
			ItemStack item = e.getItem();
			boolean isSmearer = item.getType().equals(Material.INK_SACK);
			boolean isScraper = item.getType().equals(Material.DIAMOND);
			if(isSmearer || isScraper)
			{
				if(player.hasPermission("DyeSmear.smear"))
				{
					if(e.getAction().equals(Action.LEFT_CLICK_BLOCK))
					{
						Block block = e.getClickedBlock();
						addBlock(player, block);
						player.sendMessage("Selected "+smears.get(player).size()+" blocks to scrape/smear");
					}
					else if(e.getAction().equals(Action.RIGHT_CLICK_BLOCK))
					{
						validateBlocks(player);
						scrape(player, isScraper);
						if(isSmearer)
						{
							smear(player, item);
							player.setItemInHand(item);
						}
						if(smears.get(player).size() == 0)
						{
							player.sendMessage("Finished scraping/smearing");
						}
						else
						{
							player.sendMessage("Ran out of supplies - "+smears.get(player).size()+" blocks not smeared");
						}
					}
				}
				else
				{
					player.sendMessage("You don't have permission to use DyeSmear (DyeSmear.smear)");
				}
			}
		}
	}
	@EventHandler
	public void onPlayerLeave(PlayerQuitEvent e)
	{
		cancelSmear(e.getPlayer());
	}
}
