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
	private double dyeCost(Material m)
	{
		switch(m)
		{
			case GLASS:
				return 1.0/8.0;
			case STAINED_GLASS:
				return 1.0/8.0;
			case THIN_GLASS:
				return 3.0/64.0;
			case STAINED_GLASS_PANE:
				return 3.0/64.0;
			case HARD_CLAY:
				return 1.0/8.0;
			case STAINED_CLAY:
				return 1.0/8.0;
		}
		return 0.0;
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
		Map<DyeColor, Double> colors = new HashMap<>();
		Set<Block> blocks = smears.get(p);
		for(Iterator<Block> it = blocks.iterator(); it.hasNext(); )
		{
			Block b = it.next();
			DyeColor c = DyeColor.getByWoolData(b.getData());
			if(!colors.containsKey(c)) colors.put(c, 0.0);
			if(!downgrade(b.getType()).equals(b.getType()))
			{
				colors.put(c, colors.get(c)+dyeCost(b.getType()));
				b.setType(downgrade(b.getType()));
			}
			if(remove) it.remove();
		}
		for(Map.Entry<DyeColor, Double> color : colors.entrySet())
		{
			while(color.getValue() >= 1.0)
			{
				ItemStack result = new ItemStack(Material.INK_SACK, 1, color.getKey().getDyeData());
				p.getWorld().dropItem(p.getLocation(), result).setPickupDelay(0);
				color.setValue(color.getValue()-1.0);
			}
		}
	}
	private void smear(Player p, ItemStack item)
	{
		DyeColor c = DyeColor.getByDyeData(item.getData().getData());
		int amount = item.getAmount();
		double dyes = 0.0;
		Set<Block> blocks = smears.get(p);
		for(Iterator<Block> it = blocks.iterator(); it.hasNext(); )
		{
			Block b = it.next();
			if(dyes <= 0.0)
			{
				if(--amount < 0)
				{
					break;
				}
				dyes += 1.0;
			}
			if(!upgrade(b.getType()).equals(b.getType()))
			{
				b.setType(upgrade(b.getType()));
				b.setData(c.getWoolData());
				dyes -= dyeCost(b.getType());
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
			Block block = e.getClickedBlock();
			if(!canInteract(block.getType()))
			{
				return;
			}
			boolean isSmearer = item.getType().equals(Material.INK_SACK);
			boolean isScraper = item.getType().equals(Material.DIAMOND);
			if(isSmearer || isScraper)
			{
				if(player.hasPermission("DyeSmear.smear"))
				{
					if(e.getAction().equals(Action.LEFT_CLICK_BLOCK))
					{
						addBlock(player, block);
						double cost = 0.0;
						for(Block b : smears.get(player))
						{
							cost += dyeCost(b.getType());
						}
						player.sendMessage("Selected "+smears.get(player).size()+" blocks to scrape/smear ("+cost+" dye)");
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
