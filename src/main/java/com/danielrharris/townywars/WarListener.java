package com.danielrharris.townywars;

import com.palmergames.bukkit.towny.event.NationAddTownEvent;
import com.palmergames.bukkit.towny.event.NationRemoveTownEvent;
import com.palmergames.bukkit.towny.event.TownAddResidentEvent;
import com.palmergames.bukkit.towny.event.TownRemoveResidentEvent;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class WarListener
  implements Listener
{
  private static Town townadd;
  
  WarListener(TownyWars aThis) {}
  
  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event)
  {
    Player player = event.getPlayer();
    try
    {
      Resident re = TownyUniverse.getDataSource().getResident(player.getName());
      Nation nation = re.getTown().getNation();
      Player plr = Bukkit.getPlayer(re.getName());
      
      War ww = WarManager.getWarForNation(nation.getName());
      if (ww != null)
      {
        player.sendMessage(ChatColor.RED + "Warning: Your nation is at war with " + ww.getEnemy(nation.getName()));
        if ((WarManager.hasBeenOffered(ww, nation)) && ((nation.hasAssistant(re)) || (re.isKing()))) {
          player.sendMessage(ChatColor.GREEN + "The other nation has offered peace!");
        }
      }
    }
    catch (Exception ex) {}
  }
  
  @EventHandler
  public void onResidentLeave(TownRemoveResidentEvent event)
  {
    Nation n;
    try
    {
      n = event.getTown().getNation();
    }
    catch (NotRegisteredException ex)
    {
      return;
    }
    War war = WarManager.getWarForNation(n.getName());
    if (war == null) {
      return;
    }
    war.chargeTownPoints(n.getName(), event.getTown().getName(), TownyWars.pPlayer);
  }
  
  @EventHandler
  public void onResidentAdd(TownAddResidentEvent event)
  {
    Nation n;
    try
    {
      n = event.getTown().getNation();
    }
    catch (NotRegisteredException ex)
    {
      return;
    }
    War war = WarManager.getWarForNation(n.getName());
    if (war == null) {
      return;
    }
    war.chargeTownPoints(n.getName(), event.getTown().getName(), -TownyWars.pPlayer);
  }
  
  @EventHandler
  public void onNationAdd(NationAddTownEvent event)
  {
    War war = WarManager.getWarForNation(event.getNation().getName());
    if (war == null) {
      return;
    }
    if (event.getTown() != townadd)
    {
      war.addNation(event.getNation(), event.getTown());
      townadd = null;
    }
  }
  
  @EventHandler
  public void onNationRemove(NationRemoveTownEvent event)
  {
    if (event.getTown() != War.townremove)
    {
      War war = WarManager.getWarForNation(event.getNation().getName());
      if (war == null) {
        return;
      }
      townadd = event.getTown();
      try
      {
        event.getNation().addTown(event.getTown());
      }
      catch (AlreadyRegisteredException ex)
      {
        Logger.getLogger(WarListener.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
    War.townremove = null;
  }
  
  @EventHandler
  public void onPlayerDeath(PlayerDeathEvent event)
  {
    Player plr = event.getEntity();
    EntityDamageEvent edc = event.getEntity().getLastDamageCause();
    if (!(edc instanceof EntityDamageByEntityEvent)) {
      return;
    }
    EntityDamageByEntityEvent edbee = (EntityDamageByEntityEvent)edc;
    if (!(edbee.getDamager() instanceof Player)) {
      return;
    }
    Player damager = (Player)edbee.getDamager();
    Player damaged = (Player)edbee.getEntity();
    try
    {
      Town tdamagerr = TownyUniverse.getDataSource().getResident(damager.getName()).getTown();
      Nation damagerr = tdamagerr.getNation();
      

      Town tdamagedd = TownyUniverse.getDataSource().getResident(damaged.getName()).getTown();
      Nation damagedd = tdamagedd.getNation();
      
      War war = WarManager.getWarForNation(damagerr.getName());
      if ((war.hasNation(damagedd.getName())) && (!damagerr.getName().equals(damagedd.getName())))
      {
        tdamagedd.pay(TownyWars.pKill, "Death cost");
        tdamagerr.collect(TownyWars.pKill);
      }
      if ((damagedd.getNumTowns() > 1) && (damagedd.isCapital(tdamagedd))) {
        return;
      }
      if ((war.hasNation(damagedd.getName())) && (!damagerr.getName().equals(damagedd.getName()))) {
        try
        {
          war.chargeTownPoints(damagerr.getName(), tdamagerr.getName(), -1.0D);
          war.chargeTownPoints(damagedd.getName(), tdamagedd.getName(), 1.0D);
          int lP = war.getTownPoints(tdamagedd.getName()).intValue();
          if (lP <= 10) {
            plr.sendMessage(ChatColor.RED + "Be careful! Your town only has a " + lP + " points left!");
          }
        }
        catch (Exception ex)
        {
          plr.sendMessage(ChatColor.RED + "An error occured, check the console!");
          ex.printStackTrace();
        }
      }
    }
    catch (Exception ex) {}
  }
}
