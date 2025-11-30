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
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import io.github.townyadvanced.flagwar.chunkManipulation.CopyChunk;
import io.github.townyadvanced.flagwar.chunkManipulation.PasteChunk;
import io.github.townyadvanced.flagwar.events.*;
import org.bukkit.*;
import org.bukkit.block.Block;
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

    public class FlagInfo {
        private final Resident flagPlacer;
        private final Town currentTown;
        private int potentialLivesLeft = 2; // if 2 then 4 gold for an extra life, if 1 then 16, if 0 you can't do shit.
        private final Nation attackingNation;
        private final Nation defendingNation;
        private final TownBlock currentTownBlock;
        private final Block flagBlock;
        private boolean forceFielded = false;

        public FlagInfo(Resident flagPlacer, TownBlock currentTownBlock, Block flagBlock) {
            this.flagPlacer = flagPlacer;
            this.currentTownBlock = currentTownBlock;
            this.currentTown = this.currentTownBlock.getTownOrNull();
            this.attackingNation = flagPlacer.getNationOrNull();
            this.defendingNation = currentTown.getNationOrNull();
            this.flagBlock = flagBlock;
        }

        public Resident getFlagPlacer() {
            return flagPlacer;
        }

        public Town getTown() {
            return currentTown;
        }

        public int getPotentialLivesLeft() {
            return potentialLivesLeft;
        }

        public void setPotentialLivesLeft(int potentialLivesLeft) {
            this.potentialLivesLeft = potentialLivesLeft;
        }

        public Nation getAttackingNation() {
            return attackingNation;
        }

        public Block getFlagBlock() {
            return flagBlock;
        }

        public Nation getDefendingNation() {
            return defendingNation;
        }

        public TownBlock getCurrentTownBlock() {
            return currentTownBlock;
        }

        public boolean isForceFielded()
        {
            return forceFielded;
        }

        public void setForceFielded(boolean forceFielded)
        {
            this.forceFielded = forceFielded;
        }
    }

    public class WarInfo {
        private final Town attackedTown;
        private final Nation attackingNation;
        private final Nation defendingNation;
        private FlagInfo currentFlag;

        public WarInfo(Town attackedTown, Nation attackingNation, Nation defendingNation) {
            this.attackedTown = attackedTown;
            this.attackingNation = attackingNation;
            this.defendingNation = defendingNation;
            currentFlag = null;
        }

        public Town getAttackedTown() {
            return attackedTown;
        }

        public Nation getAttackingNation() {
            return attackingNation;
        }

        public Nation getDefendingNation() {
            return defendingNation;
        }

        public FlagInfo getCurrentFlag() {
            return currentFlag;
        }

        public void setCurrentFlag(FlagInfo currentFlag) {
            this.currentFlag = currentFlag;
        }
    }

    // uses the UUID of the town being attacked, as a town can only be attacked once at a time.
    public static HashMap<UUID, WarInfo> warInfos = new HashMap<>();

    CopyChunk copyChunk = new CopyChunk();
    PasteChunk pasteChunk = new PasteChunk();
    Server server = Bukkit.getServer();
    JavaPlugin plugin = JavaPlugin.getProvidingPlugin(this.getClass());

    @EventHandler
    public void onWarStart(WarStartEvent e) {
        Town attackedTown = e.getAttackedTown();
        System.out.println("warStartEvent");
        server.broadcastMessage(ChatColor.BLUE + "[" + ChatColor.YELLOW + "FLAGWAR" + ChatColor.BLUE + "] " + ChatColor.WHITE + e.getAttackingNation().getName() + " has initiated a battle on " + e.getDefendingNation().getName() + " at " + e.getAttackedTown().getName() + "!");

        // plugin.getConfig().set("activeWars", attackedTown);

        copyChunk.copyChunks(attackedTown.getWorld(), attackedTown);
        WarInfo w = new WarInfo(attackedTown, e.getAttackingNation(), e.getDefendingNation());
        warInfos.put(attackedTown.getUUID(), w);
    }

    @EventHandler
    public void onWarEnd(WarEndEvent e) {
        warInfos.remove(e.getAttackedTown().getUUID());
        server.broadcastMessage(ChatColor.BLUE + "[" + ChatColor.YELLOW + "FLAGWAR" + ChatColor.BLUE + "] " + ChatColor.WHITE + "The battle between " + e.getAttackingNation().getName() + " and " + e.getDefendingNation().getName() + " at " + e.getAttackedTown().getName() + " has ended!");
        if (e.getWarEndReason() == WarEndEvent.WarEndReason.timerRanOut) {
            server.broadcastMessage(ChatColor.BLUE + "[" + ChatColor.YELLOW + "FLAGWAR" + ChatColor.BLUE + "] " + ChatColor.WHITE + e.getDefendingNation().getName() + " has successfully defended " + e.getAttackedTown().getName() + " from " + e.getAttackingNation().getName() + "!");
            pasteChunk.pasteChunks(e.getAttackedTown().getWorld(), e.getAttackedTown());
        } else if (e.getWarEndReason() == WarEndEvent.WarEndReason.homeBlockCellWon) {
            server.broadcastMessage(ChatColor.BLUE + "[" + ChatColor.YELLOW + "FLAGWAR" + ChatColor.BLUE + "] " + ChatColor.WHITE + e.getAttackingNation().getName() + " has successfully conquered " + e.getAttackedTown().getName() + " from " + e.getDefendingNation().getName() + "!");
            new BukkitRunnable() {
                @Override
                public void run() {
                    pasteChunk.pasteChunks(e.getAttackedTown().getWorld(), e.getAttackedTown());
                }
            }.runTaskLater(plugin, 200);
        }
    }

    @EventHandler
    public void onCellAttack(CellAttackEvent e) {
        TownBlock tb = TownyAPI.getInstance().getTownBlock(e.getFlagBlock().getLocation());
        Block flagBlock = e.getFlagBlock().getWorld().getBlockAt(e.getFlagBlock().getLocation().toVector().add(new Vector(0,1,0)).toLocation(e.getFlagBlock().getWorld()));
        warInfos.get(tb.getTownOrNull().getUUID()).setCurrentFlag(new FlagInfo(TownyAPI.getInstance().getResident(e.getPlayer().getUniqueId()), tb, flagBlock));
    }

    @EventHandler
    public void onCellWon(CellWonEvent e)
    {
        TownBlock tb = TownyAPI.getInstance().getTownBlock(e.getCellUnderAttack().getFlagBaseBlock().getLocation());
        warInfos.get(tb.getTownOrNull().getUUID()).setCurrentFlag(null);
    }

    @EventHandler
    public void onCellDefended(CellDefendedEvent e)
    {
        TownBlock tb = TownyAPI.getInstance().getTownBlock(e.getCell().getAttackData().getFlagBaseBlock().getLocation());

        FlagInfo flagInfo = warInfos.get(tb.getTownOrNull().getUUID()).getCurrentFlag();

        warInfos.get(tb.getTownOrNull().getUUID()).setCurrentFlag(null);
    }

    // now for the flag lives logic... finally

    @EventHandler
    public void onPotentialFlagLiveIncrease(PlayerInteractEvent e)
    {
        if (e.getAction().isRightClick() && e.getHand() == EquipmentSlot.HAND)
        {
            Block potentialFlagBlock = e.getClickedBlock();
            TownBlock tb = TownyAPI.getInstance().getTownBlock(potentialFlagBlock.getLocation());
            FlagInfo currentFlag = warInfos.get(tb.getTownOrNull().getUUID()).getCurrentFlag();
            if (currentFlag != null && currentFlag.getFlagBlock().equals(potentialFlagBlock))
            {
                ItemStack itemHeld = e.getPlayer().getInventory().getItemInMainHand();
                if (itemHeld.getType() == Material.GOLD_INGOT) {

                    if (currentFlag.getPotentialLivesLeft() == 2 && itemHeld.getAmount() >= 4)
                    {
                        itemHeld.setAmount(itemHeld.getAmount()-4);
                        e.getPlayer().getInventory().setItemInMainHand(itemHeld); // itemHeld is a copy, so we have to update it.
                        e.getPlayer().sendMessage("You have exchanged 4 gold ingots for 1 extra life!");
                        currentFlag.setPotentialLivesLeft(1);
                        currentFlag.setForceFielded(true);
                    }
                    else if (currentFlag.getPotentialLivesLeft() == 1 && itemHeld.getAmount() >= 16)
                    {
                        itemHeld.setAmount(itemHeld.getAmount()-16);
                        e.getPlayer().getInventory().setItemInMainHand(itemHeld); // itemHeld is a copy, so we have to update it.
                        e.getPlayer().sendMessage("You have exchanged 16 gold ingots for 1 extra life!");
                        currentFlag.setPotentialLivesLeft(0);
                        currentFlag.setForceFielded(true);
                    }
                }
            }

        }
    }

}
