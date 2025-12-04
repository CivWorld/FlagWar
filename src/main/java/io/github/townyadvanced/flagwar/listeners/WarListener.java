/*
 * Copyright (c) 2025 TownyAdvanced
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.github.townyadvanced.flagwar.listeners;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.*;
import com.palmergames.bukkit.towny.utils.TownRuinUtil;
import io.github.townyadvanced.flagwar.chunkManipulation.CopyChunk;
import io.github.townyadvanced.flagwar.chunkManipulation.PasteChunk;
import io.github.townyadvanced.flagwar.events.*;
import io.github.townyadvanced.flagwar.objects.CellUnderAttack;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.UUID;

public class WarListener implements Listener {

    enum FlagState
    {
        preFlag,
        flag,
        postFlag // in case the game needs to store more information after a war, such as whether the town is invincible or ruined
    }
    public class FlagInfo {
        private Resident flagPlacer;
        private int potentialExtraLives = 2; // if 2 then 4 gold for an extra life, if 1 then 16, if 0 you can't do shit.
        private int actualExtraLives = 0;
        private TownBlock currentTownBlock;
        private Block flagBlock;
        private boolean livesFrozen = false;
        CellUnderAttack attackData;
        int extraTicks = 0;

        public FlagInfo(Resident flagPlacer, TownBlock currentTownBlock, Block flagBlock, CellUnderAttack attackData) {
            this.flagPlacer = flagPlacer;
            this.currentTownBlock = currentTownBlock;
            this.flagBlock = flagBlock;
            this.attackData = attackData;
        }

        public Resident getFlagPlacer() {return flagPlacer;}
        public int getPotentialExtraLives() {return potentialExtraLives;}
        public void setPotentialExtraLives(int potentialExtraLives) {this.potentialExtraLives = potentialExtraLives;}
        public int getActualExtraLives() {return actualExtraLives;}
        public void setActualExtraLives(int actualExtraLives) {this.actualExtraLives = actualExtraLives;}
        public Block getFlagBlock() {return flagBlock;}
        public boolean isLivesFrozen() {return livesFrozen;}
        public void setLivesFrozen(boolean livesFrozen) {this.livesFrozen = livesFrozen;}
        public TownBlock getCurrentTownBlock() {return currentTownBlock;}
        public CellUnderAttack getAttackData() {return attackData;}
        public void setAttackData(CellUnderAttack attackData) {this.attackData = attackData;}
        public int getExtraTicks() {return extraTicks;}
        public void setExtraTicks(int extraTicks) {this.extraTicks = extraTicks;}

        public void reset() // resets the flag information such that there is no active cell.
        {
            currentTownBlock = null;
            flagBlock = null;
            flagPlacer = null;
            potentialExtraLives = 2;
            actualExtraLives = 0;
            livesFrozen = false;
        }
    }

    public class WarInfo {
        private final Town attackedTown;
        private final Nation attackingNation;
        private final Nation defendingNation;
        private final Resident initialMayor;
        private FlagInfo currentFlag;
        FlagState currentFlagState;


        public WarInfo(Town attackedTown, Nation attackingNation, Nation defendingNation) {
            this.attackedTown = attackedTown;
            this.attackingNation = attackingNation;
            this.defendingNation = defendingNation;
            this.initialMayor = attackedTown.getMayor();
            currentFlagState = FlagState.preFlag;
            currentFlag = null;
        }

        public Resident getInitialMayor() {return initialMayor;}

        public Town getAttackedTown() {return attackedTown;}
        public Nation getAttackingNation() {return attackingNation;}
        public Nation getDefendingNation() {return defendingNation;}
        public FlagInfo getCurrentFlag() {return currentFlag;}
        public void setCurrentFlag(FlagInfo currentFlag) {this.currentFlag = currentFlag;}
        public FlagState getCurrentFlagState() {return currentFlagState;}
        public void setCurrentFlagState(FlagState currentFlagState) {this.currentFlagState = currentFlagState;}
    }

    // uses the UUID of the town being attacked, as a town can only be attacked once at a time.
    public static HashMap<UUID, WarInfo> warInfos = new HashMap<>();

    CopyChunk copyChunk = new CopyChunk();
    PasteChunk pasteChunk = new PasteChunk(); // consider making these static.
    Server server = Bukkit.getServer();
    JavaPlugin plugin = JavaPlugin.getProvidingPlugin(this.getClass());

    @EventHandler
    public void onWarStart(WarStartEvent e) {
        Town attackedTown = e.getAttackedTown();
        server.broadcastMessage(ChatColor.BLUE + "[" + ChatColor.YELLOW + "FLAGWAR" + ChatColor.BLUE + "] " + ChatColor.WHITE + e.getAttackingNation().getName() + " has initiated a battle on " + e.getDefendingNation().getName() + " at " + e.getAttackedTown().getName() + "!");

        // plugin.getConfig().set("activeWars", attackedTown);

        copyChunk.copyChunks(attackedTown.getWorld(), attackedTown);
        WarInfo w = new WarInfo(attackedTown, e.getAttackingNation(), e.getDefendingNation());
        warInfos.put(attackedTown.getUUID(), w);
    }

    @EventHandler
    public void onWarEnd(WarEndEvent e) {
        WarInfo warInfo = warInfos.get(e.getAttackedTown().getUUID());
        // warInfos.get(e.getAttackedTown().getUUID()).getCurrentFlag().getAttackData().destroyFlag();
        server.broadcastMessage(ChatColor.BLUE + "[" + ChatColor.YELLOW + "FLAGWAR" + ChatColor.BLUE + "] " + ChatColor.WHITE + "The battle between " + e.getAttackingNation().getName() + " and " + e.getDefendingNation().getName() + " at " + e.getAttackedTown().getName() + " has ended!");
        if (e.getWarEndReason() == WarEndEvent.WarEndReason.timerRanOut) {

            server.broadcastMessage(ChatColor.BLUE + "[" + ChatColor.YELLOW + "FLAGWAR" + ChatColor.BLUE + "] " + ChatColor.WHITE + e.getDefendingNation().getName() + " has successfully defended " + e.getAttackedTown().getName() + " from " + e.getAttackingNation().getName() + "!");
            pasteChunk.pasteChunks(e.getAttackedTown().getWorld(), e.getAttackedTown());
            warInfo.setCurrentFlagState(FlagState.postFlag);

            new BukkitRunnable()
            {
                @Override
                public void run()
                {
                    warInfos.remove(e.getAttackedTown().getUUID());
                }
            }.runTaskLater(plugin, 400);

        } else if (e.getWarEndReason() == WarEndEvent.WarEndReason.homeBlockCellWon) {

            server.broadcastMessage(ChatColor.BLUE + "[" + ChatColor.YELLOW + "FLAGWAR" + ChatColor.BLUE + "] " + ChatColor.WHITE + e.getAttackingNation().getName() + " has successfully conquered " + e.getAttackedTown().getName() + " from " + e.getDefendingNation().getName() + "! The attacker now has free-range over the town!");
            Bukkit.getServer().broadcastMessage(String.valueOf(e.getAttackedTown().getPermissions()));

            TownRuinUtil.putTownIntoRuinedState(e.getAttackedTown());
            warInfo.setCurrentFlagState(FlagState.postFlag);

            new BukkitRunnable()
            {
                @Override
                public void run()
                {
                    Bukkit.getServer().broadcastMessage("now put all perms back");
                    TownRuinUtil.reclaimTown(warInfo.getInitialMayor(), e.getAttackedTown());
                    warInfos.remove(e.getAttackedTown().getUUID());
                    pasteChunk.pasteChunks(e.getAttackedTown().getWorld(), e.getAttackedTown());
                }
            }.runTaskLater(plugin, 400);
        }
    }

    @EventHandler
    public void onCellAttack(CellAttackEvent e) {
        TownBlock tb = TownyAPI.getInstance().getTownBlock(e.getFlagBlock().getLocation());
        Block flagBlock = e.getFlagBlock().getWorld().getBlockAt(e.getFlagBlock().getLocation().toVector().add(new Vector(0,1,0)).toLocation(e.getFlagBlock().getWorld()));
        warInfos.get(tb.getTownOrNull().getUUID()).setCurrentFlag(new FlagInfo(TownyAPI.getInstance().getResident(e.getPlayer().getUniqueId()), tb, flagBlock, e.getData()));
    }

    @EventHandler
    public void onCellWon(CellWonEvent e)
    {
        TownBlock tb = TownyAPI.getInstance().getTownBlock(e.getCellUnderAttack().getFlagBaseBlock().getLocation());

        FlagInfo flagInfo = warInfos.get(tb.getTownOrNull().getUUID()).getCurrentFlag();
        flagInfo.reset();

        warInfos.get(tb.getTownOrNull().getUUID()).setCurrentFlag(flagInfo);
    }

    @EventHandler
    public void onCellDefended(CellDefendedEvent e)
    {
        TownBlock tb = TownyAPI.getInstance().getTownBlock(e.getCell().getAttackData().getFlagBaseBlock().getLocation());

        FlagInfo flagInfo = warInfos.get(tb.getTownOrNull().getUUID()).getCurrentFlag();
        flagInfo.reset();

        warInfos.get(tb.getTownOrNull().getUUID()).setCurrentFlag(flagInfo);
    }

    // now for the flag lives logic... finally

    @EventHandler
    public void onPotentialFlagLiveIncrease(PlayerInteractEvent e)
    {
        if (e.getAction().isRightClick() && e.getHand() == EquipmentSlot.HAND && e.getClickedBlock() != null)
        {
            Block potentialFlagBlock = e.getClickedBlock();
            TownBlock tb = TownyAPI.getInstance().getTownBlock(potentialFlagBlock.getLocation());
            if (warInfos.getOrDefault(tb.getTownOrNull().getUUID(), null) == null) return;
            FlagInfo currentFlag = warInfos.get(tb.getTownOrNull().getUUID()).getCurrentFlag();
            if (currentFlag != null && currentFlag.getFlagBlock() != null && currentFlag.getFlagBlock().equals(potentialFlagBlock))
            {
                ItemStack itemHeld = e.getPlayer().getInventory().getItemInMainHand();
                Player p = e.getPlayer();
                Resident r = TownyAPI.getInstance().getResident(p.getUniqueId());
                if (itemHeld.getType() == Material.GOLD_INGOT
                    && (r.getNationOrNull().isAlliedWith(currentFlag.getFlagPlacer().getNationOrNull()) || r.getNationOrNull().equals(currentFlag.getFlagPlacer().getNationOrNull()))
                    && r.getNationRanks() != null)
                {
                    if (currentFlag.getPotentialExtraLives() == 2 && itemHeld.getAmount() >= 4)
                    {
                        itemHeld.setAmount(itemHeld.getAmount()-4);
                        p.getInventory().setItemInMainHand(itemHeld); // itemHeld is a copy, so we have to update it.
                        p.sendMessage("You have exchanged 4 gold ingots for 1 extra life!");
                        currentFlag.setPotentialExtraLives(1);
                        currentFlag.setActualExtraLives(currentFlag.getActualExtraLives()+1);
                        currentFlag.setExtraTicks(currentFlag.getExtraTicks()+300);
                        new BukkitRunnable()
                        {
                            @Override
                            public void run()
                            {
                                currentFlag.setLivesFrozen(true);
                            }
                        }.runTaskLater(JavaPlugin.getProvidingPlugin(this.getClass()), 200);
                    }
                    else if (currentFlag.getPotentialExtraLives() == 1 && itemHeld.getAmount() >= 16 && !currentFlag.isLivesFrozen())
                    {
                        itemHeld.setAmount(itemHeld.getAmount()-16);
                        p.getInventory().setItemInMainHand(itemHeld); // itemHeld is a copy, so we have to update it.
                        p.sendMessage("You have exchanged 16 gold ingots for 1 extra life!");
                        currentFlag.setPotentialExtraLives(0);
                        currentFlag.setActualExtraLives(currentFlag.getActualExtraLives()+1);
                        currentFlag.setExtraTicks(currentFlag.getExtraTicks()+300);
                    }
                }
            }
        }
    }


    @EventHandler
    public void onEligibleToFlag(EligibleToFlagEvent e)
    {
        warInfos.get(e.getAttackedTown().getUUID()).setCurrentFlagState(FlagState.flag);
    }
}
