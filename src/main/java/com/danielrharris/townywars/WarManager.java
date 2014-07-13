package com.danielrharris.townywars;

import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WarManager
{
  private static Set<War> activeWars = new HashSet();
  private static Set<String> requestedPeace = new HashSet();
  private static final int SAVING_VERSION = 1;
  
  public static void save(File dataFolder)
    throws Exception
  {
    if (!dataFolder.exists()) {
      dataFolder.mkdir();
    }
    File f = new File(dataFolder, "activeWars.dat");
    if (f.exists()) {
      f.delete();
    }
    f.createNewFile();
    DataOutputStream dos = new DataOutputStream(new FileOutputStream(f));
    dos.writeInt(1);
    dos.writeInt(activeWars.size());
    for (Iterator i$ = activeWars.iterator(); i$.hasNext();)
    {
      War war = (War)i$.next();
      war.save(dos);
      for (String nation : war.getNations()) {
        System.out.println("Saved: " + nation + " " + war.getNationPoints(nation));
      }
    }
    War war;
    dos.flush();
    dos.close();
  }
  
  public static void load(File dataFolder)
    throws Exception
  {
    if (!dataFolder.exists()) {
      return;
    }
    File f = new File(dataFolder, "activeWars.dat");
    if (!f.exists()) {
      return;
    }
    DataInputStream dis = new DataInputStream(new FileInputStream(f));
    int ver = dis.readInt();
    int tWars = dis.readInt();
    War ww;
    for (int i = 1; i <= tWars; i++)
    {
      ww = War.load(dis);
      activeWars.add(ww);
      for (String nation : ww.getNations()) {
        System.out.println("Loaded: " + nation + " " + ww.getNationPoints(nation));
      }
    }
    dis.close();
  }
  
  public static Set<War> getWars()
  {
    return activeWars;
  }
  
  public static War getWarForNation(String onation)
  {
    for (War w : activeWars) {
      if (w.hasNation(onation)) {
        return w;
      }
    }
    return null;
  }
  
  public static void createWar(Nation nat, Nation onat, CommandSender cs)
  {
    if ((getWarForNation(nat.getName()) != null) || (getWarForNation(onat.getName()) != null))
    {
      cs.sendMessage(ChatColor.RED + "Your nation is already at war with another nation!");
    }
    else
    {
      try
      {
        try
        {
          TownyUniverse.getDataSource().getNation(nat.getName()).addEnemy(onat);
          TownyUniverse.getDataSource().getNation(onat.getName()).addEnemy(nat);
        }
        catch (AlreadyRegisteredException ex)
        {
          Logger.getLogger(WarManager.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
      catch (NotRegisteredException ex)
      {
        Logger.getLogger(WarManager.class.getName()).log(Level.SEVERE, null, ex);
      }
      War war = new War(nat, onat);
      activeWars.add(war);
      for (Resident re : nat.getResidents())
      {
        Player plr = Bukkit.getPlayer(re.getName());
        if (plr != null) {
          plr.sendMessage(ChatColor.RED + "Your nation is now at war with " + onat.getName() + "!");
        }
      }
      for (Resident re : onat.getResidents())
      {
        Player plr = Bukkit.getPlayer(re.getName());
        if (plr != null) {
          plr.sendMessage(ChatColor.RED + "Your nation is now at war with " + nat.getName() + "!");
        }
      }
      for (Town t : nat.getTowns()) {
        t.setAdminEnabledPVP(true);
      }
      for (Town t : onat.getTowns()) {
        t.setAdminEnabledPVP(true);
      }
    }
  }
  
  public static boolean requestPeace(Nation nat, Nation onat, boolean admin)
  {
    if ((admin) || (requestedPeace.contains(onat.getName())))
    {
      endWar(nat, onat, true);
      try
      {
        nat.collect(TownyWars.endCost);
        onat.collect(TownyWars.endCost);
      }
      catch (EconomyException ex)
      {
        Logger.getLogger(WarManager.class.getName()).log(Level.SEVERE, null, ex);
      }
      return true;
    }
    if (admin)
    {
      endWar(nat, onat, true);
      return true;
    }
    requestedPeace.add(nat.getName());
    for (Resident re : onat.getResidents()) {
      if ((re.isKing()) || (onat.hasAssistant(re)))
      {
        Player plr = Bukkit.getPlayer(re.getName());
        if (plr != null) {
          plr.sendMessage(ChatColor.GREEN + nat.getName() + " has requested peace!");
        }
      }
    }
    return false;
  }
  
  public static void endWar(Nation winner, Nation looser, boolean peace)
  {
    try
    {
      TownyUniverse.getDataSource().getNation(winner.getName()).removeEnemy(looser);
      TownyUniverse.getDataSource().getNation(looser.getName()).removeEnemy(winner);
    }
    catch (NotRegisteredException ex)
    {
      Logger.getLogger(WarManager.class.getName()).log(Level.SEVERE, null, ex);
    }
    activeWars.remove(getWarForNation(winner.getName()));
    requestedPeace.remove(looser.getName());
    War.broadcast(winner, ChatColor.GREEN + "You are now at peace!");
    War.broadcast(looser, ChatColor.GREEN + "You are now at peace!");
    for (Town t : winner.getTowns()) {
      t.setAdminEnabledPVP(false);
    }
    for (Town t : looser.getTowns())
    {
      if (!peace) {
        try
        {
          War.townremove = t;
          looser.removeTown(t);
          winner.addTown(t);
        }
        catch (Exception ex)
        {
          Logger.getLogger(WarManager.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
      t.setAdminEnabledPVP(false);
    }
    if (!peace)
    {
      TownyUniverse.getDataSource().removeNation(looser);
      looser.clear();
      TownyWars.tUniverse.getNationsMap().remove(looser.getName());
    }
  }
  
  public static boolean hasBeenOffered(War ww, Nation nation)
  {
    return requestedPeace.contains(ww.getEnemy(nation.getName()));
  }
}
