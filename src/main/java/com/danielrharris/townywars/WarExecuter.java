package com.danielrharris.townywars;

import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginManager;


class WarExecutor implements CommandExecutor
{
  private TownyWars plugin;
 
   public WarExecutor(TownyWars aThis)
  {
    this.plugin = aThis;
  }
  
  public boolean onCommand(CommandSender cs, Command cmnd, String string, String[] strings)
  {
    if (strings.length == 0)
    {
      cs.sendMessage(ChatColor.GREEN + "Towny Wars Configuration Information:");
      cs.sendMessage(ChatColor.BLUE + "Defense Points Calculation: " + ChatColor.AQUA + "Per Player: " + ChatColor.YELLOW + TownyWars.pPlayer + ChatColor.AQUA + " || Per Chunk: " + ChatColor.YELLOW + TownyWars.pPlayer);
      cs.sendMessage(ChatColor.BLUE + "Costs: " + ChatColor.AQUA + "Per Death: " + ChatColor.YELLOW + TownyWars.pKill + ChatColor.AQUA + " || Declare Cost: " + ChatColor.YELLOW + TownyWars.declareCost + ChatColor.AQUA + " || End Cost: " + ChatColor.YELLOW + TownyWars.endCost);
      cs.sendMessage(ChatColor.GREEN + "For help with TownyWars, type /twar help");
      return true;
    }
    String farg = strings[0];
    if (farg.equals("reload"))
    {
      if (!cs.hasPermission("townywars.admin")) {
        return false;
      }
      cs.sendMessage(ChatColor.GREEN + "Reloading plugin...");
      PluginManager pm = Bukkit.getServer().getPluginManager();
      pm.disablePlugin(this.plugin);
      pm.enablePlugin(this.plugin);
      cs.sendMessage(ChatColor.GREEN + "Plugin reloaded!");
    }
    if (farg.equals("help"))
    {
      cs.sendMessage(ChatColor.GREEN + "Towny Wars Help:");
      cs.sendMessage(ChatColor.AQUA + "/twar" + ChatColor.YELLOW + "Displays the TownyWars configuration information");
      cs.sendMessage(ChatColor.AQUA + "/twar help - " + ChatColor.YELLOW + "Displays the TownyWars help page");
      cs.sendMessage(ChatColor.AQUA + "/twar status - " + ChatColor.YELLOW + "Displays a list of on-going wars");
      cs.sendMessage(ChatColor.AQUA + "/twar status [nation] - " + ChatColor.YELLOW + "Displays a list of the nation's towns and their defense points");
      cs.sendMessage(ChatColor.AQUA + "/twar declare [nation] - " + ChatColor.YELLOW + "Starts a war with another nation (REQUIRES YOU TO BE A KING/ASSISTANT)");
      cs.sendMessage(ChatColor.AQUA + "/twar end - " + ChatColor.YELLOW + "Request from enemy nations king to end the ongoing war. (REQUIRES YOU TO BE A KING/ASSISTANT)");
      if (cs.hasPermission("townywars.admin"))
      {
        cs.sendMessage(ChatColor.AQUA + "/twar reload - " + ChatColor.YELLOW + "Reload the plugin");
        cs.sendMessage(ChatColor.AQUA + "/twar astart [nation] [nation] - " + ChatColor.YELLOW + "Forces two nations to go to war");
        cs.sendMessage(ChatColor.AQUA + "/twar aend [nation] [nation] - " + ChatColor.YELLOW + "Forces two nations to stop a war");
      }
    }
    War w;
    if (farg.equals("status"))
    {
      if (strings.length == 1)
      {
        cs.sendMessage(ChatColor.GREEN + "List of on-going wars:");
        for (War war : WarManager.getWars())
        {
          String first = null;
          String second = null;
          for (String st : war.getNations()) {
            if (first == null) {
              first = st;
            } else {
              second = st;
            }
          }
          cs.sendMessage(ChatColor.GREEN + first + " " + war.getNationPoints(first) + " vs. " + second + " " + war.getNationPoints(second));
        }
        return true;
      }
      String onation = strings[1];
      Nation t;
      try
      {
        t = TownyUniverse.getDataSource().getNation(onation);
      }
      catch (NotRegisteredException ex)
      {
        cs.sendMessage(ChatColor.GOLD + "No nation called " + onation + " could be found!");
        return true;
      }
      w = WarManager.getWarForNation(onation);
      if (w == null)
      {
        cs.sendMessage(ChatColor.RED + "That nation isn't in a war!");
        return true;
      }
      cs.sendMessage(t.getName() + " war info:");
      for (Town tt : t.getTowns()) {
        cs.sendMessage(ChatColor.GREEN + tt.getName() + ": " + w.getTownPoints(tt.getName()) + " points");
      }
    }
    if (farg.equals("neutral"))
    {
      if (!cs.hasPermission("townywars.neutral"))
      {
        cs.sendMessage(ChatColor.RED + "You are not allowed to do this!");
        return true;
      }
      Nation csNation;
      try
      {
        Town csTown = TownyUniverse.getDataSource().getResident(cs.getName()).getTown();
        csNation = TownyUniverse.getDataSource().getTown(csTown.toString()).getNation();
      }
      catch (NotRegisteredException ex)
      {
        cs.sendMessage(ChatColor.RED + "You are not not part of a town, or your town is not part of a nation!");
        Logger.getLogger(WarExecutor.class.getName()).log(Level.SEVERE, null, ex);
        return true;
      }
      if ((!cs.isOp()) && (!csNation.toString().equals(strings[1])))
      {
        cs.sendMessage(ChatColor.RED + "You may only set your own nation to neutral, not others.");
        return true;
      }
      if (strings.length == 0) {
        cs.sendMessage(ChatColor.RED + "You must specify a nation to toggle neutrality for (eg. /twar neutral [nation]");
      }
      if (strings.length == 1)
      {
        String onation = strings[1];
        Nation t;
        try
        {
          t = TownyUniverse.getDataSource().getNation(onation);
        }
        catch (NotRegisteredException ex)
        {
          cs.sendMessage(ChatColor.GOLD + "The nation called " + onation + " could be found!");
          return true;
        }
        War.MutableInteger mi = new War.MutableInteger(0);
        War.neutral.put(t.toString(), mi);
      }
    }
    if (farg.equals("astart"))
    {
      if (!cs.hasPermission("townywars.admin"))
      {
        cs.sendMessage(ChatColor.RED + "You are not allowed to do this!");
        return true;
      }
      return declareWar(cs, strings, true);
    }
    if (farg.equals("declare")) {
      return declareWar(cs, strings, false);
    }
    if (farg.equals("end")) {
      return declareEnd(cs, strings, false);
    }
    if (farg.equals("aend"))
    {
      if (!cs.hasPermission("warexecutor.admin"))
      {
        cs.sendMessage(ChatColor.RED + "You are not allowed to do this!");
        return true;
      }
      return declareEnd(cs, strings, true);
    }
    return true;
  }
  
  private boolean declareEnd(CommandSender cs, String[] strings, boolean admin)
  {
    if ((admin) && (strings.length <= 2))
    {
      cs.sendMessage(ChatColor.RED + "You need to specify two nations!");
      return true;
    }
    String sonat = "";
    if (admin) {
      sonat = strings[1];
    }
    Resident res = null;
    Nation nat;
    try
    {
      if (admin)
      {
        nat = TownyUniverse.getDataSource().getNation(strings[2]);
      }
      else
      {
        res = TownyUniverse.getDataSource().getResident(cs.getName());
        nat = res.getTown().getNation();
      }
    }
    catch (Exception ex)
    {
      cs.sendMessage(ChatColor.RED + "You are not in a town, or your town isn't part of a nation!");
      return true;
    }
    if ((!admin) && ((!res.isKing()) || (!nat.hasAssistant(res))))
    {
      cs.sendMessage(ChatColor.RED + "You are not powerful enough in your nation to do that!");
      return true;
    }
    if (!admin)
    {
      War w = WarManager.getWarForNation(nat.getName());
      if (w == null)
      {
        cs.sendMessage(ChatColor.RED + nat.getName() + " is not at war!");
        return true;
      }
      sonat = w.getEnemy(nat.getName());
    }
    Nation onat;
    try
    {
      onat = TownyUniverse.getDataSource().getNation(sonat);
    }
    catch (NotRegisteredException ex)
    {
      cs.sendMessage(ChatColor.RED + "That nation doesn't exist!");
      return true;
    }
    if (WarManager.requestPeace(nat, onat, admin)) {
      return true;
    }
    if (admin) {
      cs.sendMessage(ChatColor.GREEN + "Forced peace!");
    } else {
      cs.sendMessage(ChatColor.GREEN + "Requested peace!");
    }
    return true;
  }
  
  private boolean declareWar(CommandSender cs, String[] strings, boolean admin)
  {
    if ((strings.length == 2) && (admin))
    {
      cs.sendMessage(ChatColor.RED + "You need to specify two nations!");
      return true;
    }
    if (strings.length == 1)
    {
      cs.sendMessage(ChatColor.RED + "You need to specify a nation!");
      return true;
    }
    String sonat = strings[1];
    Resident res;
    Nation nat;
    try
    {
      if (admin)
      {
        res = null;
        nat = TownyUniverse.getDataSource().getNation(strings[2]);
      }
      else
      {
        res = TownyUniverse.getDataSource().getResident(cs.getName());
        nat = res.getTown().getNation();
      }
    }
    catch (Exception ex)
    {
      cs.sendMessage(ChatColor.RED + "You are not in a town, or your town isn't part of a nation!");
      return true;
    }
    if (WarManager.getWarForNation(nat.getName()) != null)
    {
      cs.sendMessage(ChatColor.RED + "Your nation is already at war!");
      return true;
    }
    if ((!admin) && (!nat.isKing(res)) && (!nat.hasAssistant(res)))
    {
      cs.sendMessage(ChatColor.RED + "You are not powerful enough in your nation to do that!");
      return true;
    }
    Nation onat;
    try
    {
      onat = TownyUniverse.getDataSource().getNation(sonat);
    }
    catch (NotRegisteredException ex)
    {
      cs.sendMessage(ChatColor.RED + "That nation doesn't exist!");
      return true;
    }
    if (War.neutral.containsKey(onat))
    {
      cs.sendMessage(ChatColor.RED + "That nation is neutral and cannot enter in a war!");
      return true;
    }
    if (War.neutral.containsKey(nat))
    {
      cs.sendMessage(ChatColor.RED + "You are in a neutral nation and cannot declare war on others!");
      return true;
    }
    if (WarManager.getWarForNation(onat.getName()) != null)
    {
      cs.sendMessage(ChatColor.RED + "That nation is already at war!");
      return true;
    }
    if (nat.getName().equals(onat.getName()))
    {
      cs.sendMessage(ChatColor.RED + "A nation can't be at war with itself!");
      return true;
    }
    WarManager.createWar(nat, onat, cs);
    try
    {
      nat.collect(TownyWars.declareCost);
    }
    catch (EconomyException ex)
    {
      Logger.getLogger(WarExecutor.class.getName()).log(Level.SEVERE, null, ex);
    }
    cs.sendMessage(ChatColor.GREEN + "Declared war on " + onat.getName() + "!");
    return true;
  }
}
