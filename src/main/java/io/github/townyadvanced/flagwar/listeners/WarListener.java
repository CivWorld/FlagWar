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
import io.github.townyadvanced.flagwar.FlagWar;
import io.github.townyadvanced.flagwar.WarManager;
import io.github.townyadvanced.flagwar.chunkManipulation.CopyChunk;
import io.github.townyadvanced.flagwar.chunkManipulation.PasteChunk;
import io.github.townyadvanced.flagwar.events.*;
import io.github.townyadvanced.flagwar.objects.FlagInfo;
import io.github.townyadvanced.flagwar.objects.PersistentRunnable;
import io.github.townyadvanced.flagwar.objects.WarInfo;
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

public class WarListener implements Listener {
    Server server = Bukkit.getServer();
    WarManager warManager;

    public WarListener(WarManager warManager)
    {
        this.warManager = warManager;
    }


    @EventHandler
    public void onWarStart(WarStartEvent e) {
        server.broadcastMessage(ChatColor.BLUE + "[" + ChatColor.YELLOW + "FLAGWAR" + ChatColor.BLUE + "] " + ChatColor.WHITE + e.getAttackingNation().getName() + " has initiated a battle on " + e.getDefendingNation().getName() + " at " + e.getAttackedTown().getName() + "!");

        warManager.startWar(e.getAttackedTown(), e.getAttackingNation(), e.getDefendingNation(), e.getAttackedTown().getMayor(), WarInfo.FlagState.preFlag, true);
    }

    @EventHandler
    public void onWarEnd(WarEndEvent e) {
        WarInfo warInfo = warManager.getWarInfoOrNull(e.getAttackedTown().getUUID());

        server.broadcastMessage(ChatColor.BLUE + "[" + ChatColor.YELLOW + "FLAGWAR" + ChatColor.BLUE + "] " + ChatColor.WHITE + "The battle between " + e.getAttackingNation().getName() + " and " + e.getDefendingNation().getName() + " at " + e.getAttackedTown().getName() + " has ended!");

        if (e.getWarEndReason() == WarEndEvent.WarEndReason.timerRanOut) {

            warManager.winDefense(warInfo);
            server.broadcastMessage(ChatColor.BLUE + "[" + ChatColor.YELLOW + "FLAGWAR" + ChatColor.BLUE + "] " + ChatColor.WHITE + e.getDefendingNation().getName() + " has successfully defended " + e.getAttackedTown().getName() + " from " + e.getAttackingNation().getName() + "!");


        } else if (e.getWarEndReason() == WarEndEvent.WarEndReason.homeBlockCellWon) {
            warManager.loseDefense(warInfo);
            server.broadcastMessage(ChatColor.BLUE + "[" + ChatColor.YELLOW + "FLAGWAR" + ChatColor.BLUE + "] " + ChatColor.WHITE + e.getAttackingNation().getName() + " has successfully conquered " + e.getAttackedTown().getName() + " from " + e.getDefendingNation().getName() + "! The attacker now has free-range over the town!");
        }
    }

    @EventHandler
    public void onCellAttack(CellAttackEvent e) {
        TownBlock tb = TownyAPI.getInstance().getTownBlock(e.getFlagBlock().getLocation());
        Block flagBlock = e.getFlagBlock().getWorld().getBlockAt(e.getFlagBlock().getLocation().toVector().add(new Vector(0,1,0)).toLocation(e.getFlagBlock().getWorld()));
        warManager.addFlagToWar(warManager.getWarInfoOrNull(tb.getTownOrNull()), new FlagInfo(TownyAPI.getInstance().getResident(e.getPlayer().getUniqueId()), tb, flagBlock, e.getData()));
    }

    @EventHandler
    public void onCellWon(CellWonEvent e)
    {
        TownBlock tb = TownyAPI.getInstance().getTownBlock(e.getCellUnderAttack().getFlagBaseBlock().getLocation());
        String flagPlacer = e.getCellUnderAttack().getNameOfFlagOwner();
        WarInfo currentWar = warManager.getWarInfoOrNull(tb.getTownOrNull());
        warManager.removeFlagFromWar(currentWar, flagPlacer);
    }

    @EventHandler
    public void onCellDefended(CellDefendedEvent e)
    {
        WarInfo warInfo = warManager.getWarInfoOrNull(TownyAPI.getInstance().getTownBlock(e.getCell().getAttackData().getFlagBaseBlock().getLocation()).getTownOrNull());
        String flagPlacer = e.getCell().getAttackData().getNameOfFlagOwner();

        if (warInfo != null) warManager.removeFlagFromWar(warInfo, flagPlacer);
    }

    // this is the ugliest function in the entire codebase
    // i really think ill just keep it like that if it works.

    @EventHandler
    public void onPotentialFlagLiveIncrease(PlayerInteractEvent e)
    {
        if (e.getAction().isRightClick() && e.getHand() == EquipmentSlot.HAND && e.getClickedBlock() != null)
        {
            Block potentialFlagBlock = e.getClickedBlock();
            TownBlock tb = TownyAPI.getInstance().getTownBlock(potentialFlagBlock.getLocation());
            if (tb.getTownOrNull() == null || !warManager.hasActiveWar(tb.getTownOrNull())) return;

            WarInfo warInfo = warManager.getWarInfoOrNull(tb.getTownOrNull());
            FlagInfo currentFlag = WarManager.getFlagInfoOrNull(warInfo, potentialFlagBlock.getLocation());
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
        warManager.getWarInfoOrNull(e.getAttackedTown()).setCurrentFlagState(WarInfo.FlagState.flag);
        server.broadcastMessage(e.getAttackedTown().getName() + "'s flag state is now ON. You may now flag!");
    }
}
