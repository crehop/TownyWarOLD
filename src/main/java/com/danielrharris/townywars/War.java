package com.danielrharris.townywars;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class War
{
  public static Map<String, MutableInteger> nations = new HashMap();
  public static Map<String, MutableInteger> towns = new HashMap();
  public static Map<String, MutableInteger> neutral = new HashMap();
  public static Town townremove;
  
  public War(Nation nat, Nation onat)
  {
    recalculatePoints(nat);
    recalculatePoints(onat);
  }
  
  private War() {}
  
  public void save(DataOutputStream dos)
    throws Exception
  {
    System.out.println("Saving: " + nations.size());
    dos.writeInt(nations.size());
    for (Map.Entry<String, MutableInteger> ff : nations.entrySet())
    {
      System.out.println("Writing " + (String)ff.getKey() + " " + ((MutableInteger)ff.getValue()).value);
      dos.writeUTF((String)ff.getKey());
      dos.writeInt(((MutableInteger)ff.getValue()).value);
    }
    System.out.println("Writing: " + towns.size());
    dos.writeInt(towns.size());
    for (Map.Entry<String, MutableInteger> ff : towns.entrySet())
    {
      System.out.println("Writing: " + (String)ff.getKey() + " " + ((MutableInteger)ff.getValue()).value);
      dos.writeUTF((String)ff.getKey());
      dos.writeInt(((MutableInteger)ff.getValue()).value);
    }
    System.out.println("Writing: " + neutral.size());
    dos.writeInt(neutral.size());
    for (Map.Entry<String, MutableInteger> ff : neutral.entrySet())
    {
      System.out.println("Writing: " + (String)ff.getKey() + " " + ((MutableInteger)ff.getValue()).value);
      dos.writeUTF((String)ff.getKey());
      dos.writeInt(((MutableInteger)ff.getValue()).value);
    }
  }
  
  static War load(DataInputStream dis)
    throws Exception
  {
    War toRet = new War();
    int nNations = dis.readInt();
    System.out.println("Reading nations: " + nNations);
    for (int i = 1; i <= nNations; i++)
    {
      String nname = dis.readUTF();
      MutableInteger mi = new MutableInteger(dis.readInt());
      System.out.println("Read " + nname + " " + mi.value);
      nations.put(nname, mi);
    }
    int nTowns = dis.readInt();
    System.out.println("Reading towns: " + nTowns);
    for (int i = 1; i <= nTowns; i++)
    {
      String str = dis.readUTF();
      MutableInteger mi = new MutableInteger(dis.readInt());
      System.out.println("Reading " + str + " " + mi.value);
      towns.put(str, mi);
    }
    int nNeutral = dis.readInt();
    System.out.println("Reading neutral nations: " + nNeutral);
    for (int i = 1; i <= nNeutral; i++)
    {
      String nname = dis.readUTF();
      MutableInteger mi = new MutableInteger(dis.readInt());
      System.out.println("Read " + nname + " " + mi.value);
      neutral.put(nname, mi);
    }
    return toRet;
  }
  
  public Set<String> getNations()
  {
    return nations.keySet();
  }
  
  public Integer getNationPoints(String nation)
  {
    return Integer.valueOf(((MutableInteger)nations.get(nation)).value);
  }
  
  public Integer getTownPoints(String town)
  {
    return Integer.valueOf(((MutableInteger)towns.get(town)).value);
  }
  
  public final void recalculatePoints(Nation nat)
  {
    nations.put(nat.getName(), new MutableInteger(nat.getNumTowns()));
    for (Town town : nat.getTowns()) {
      towns.put(town.getName(), new MutableInteger((int)(town.getNumResidents() * TownyWars.pPlayer + TownyWars.pPlot * town.getTownBlocks().size())));
    }
  }
  
  boolean hasNation(String onation)
  {
    return nations.containsKey(onation);
  }
  
  public String getEnemy(String onation)
  {
    for (String s : nations.keySet()) {
      if (!s.equals(onation)) {
        return s;
      }
    }
    return "Void";
  }
  
  public void chargeTownPoints(String nname, String name, double i)
  {
    MutableInteger tt = (MutableInteger)towns.get(name); MutableInteger 
      tmp16_14 = tt;tmp16_14.value = ((int)(tmp16_14.value - i));
    if (tt.value <= 0) {
      try
      {
        towns.remove(name);
        ((MutableInteger)nations.get(nname)).value -= 1;
        Town town = TownyUniverse.getDataSource().getTown(name);
        Nation nation = TownyUniverse.getDataSource().getNation(getEnemy(nname));
        Nation nnation = TownyUniverse.getDataSource().getNation(nname);
        try
        {
          townremove = town;
          nnation.removeTown(town);
        }
        catch (Exception ex) {}
        nation.addTown(town);
        int mr = nnation.getNumTowns() + 1;
        if (mr != 0)
        {
          mr = (int)(nnation.getHoldingBalance() / mr);
          nnation.pay(mr, "War issues");
          nation.collect(mr);
        }
        TownyUniverse.getDataSource().saveNation(nation);
        TownyUniverse.getDataSource().saveNation(nnation);
        broadcast(nation, ChatColor.GREEN + town.getName() + " has been conquered and joined your nation in the war!");
      }
      catch (Exception ex)
      {
        Logger.getLogger(War.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
    if (((MutableInteger)nations.get(nname)).value == 0) {
      try
      {
        Nation winner = TownyUniverse.getDataSource().getNation(getEnemy(nname));
        Nation looser = TownyUniverse.getDataSource().getNation(nname);
        winner.collect(looser.getHoldingBalance());
        looser.pay(looser.getHoldingBalance(), "Lost the freakin war");
        WarManager.endWar(winner, looser, false);
      }
      catch (Exception ex)
      {
        Logger.getLogger(War.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }
  
  public static void removeNation(Nation nation)
  {
    ((MutableInteger)nations.get(nation.getName())).value -= 1;
  }
  
  public void addNation(Nation nation, Town town)
  {
    ((MutableInteger)nations.get(nation.getName())).value += 1;
    towns.put(town.getName(), new MutableInteger((int)(town.getNumResidents() * TownyWars.pPlayer + TownyWars.pPlot * town.getTownBlocks().size())));
  }
  
  public static void broadcast(Nation n, String message)
  {
    for (Resident re : n.getResidents())
    {
      Player plr = Bukkit.getPlayer(re.getName());
      if (plr != null) {
        plr.sendMessage(message);
      }
    }
  }
  
  public static class MutableInteger
  {
    public int value;
    
    public MutableInteger(int v)
    {
      this.value = v;
    }
  }
}
