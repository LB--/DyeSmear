package com.lb_stuff.dyesmear;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

public class SmearCommand implements CommandExecutor
{
	private DyeSmear inst;

	public SmearCommand(DyeSmear instance)
	{
		inst = instance;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		if(cmd.getName().equalsIgnoreCase("smear"))
		{
			if(sender instanceof Player)
			{
				if(args.length == 1)
				{
					Player p = (Player)sender;
					if(args[0].equalsIgnoreCase("cancel"))
					{
						inst.cancelSmear(p);
						p.sendMessage("Smear selection cleared");
						return true;
					}
				}
			}
		}
		return false;
	}
}
