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
import io.github.townyadvanced.flagwar.WarManager;
import io.github.townyadvanced.flagwar.config.FlagWarConfig;
import io.github.townyadvanced.flagwar.events.*;
import io.github.townyadvanced.flagwar.objects.FlagInfo;
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
        server.broadcastMessage("The war will last " + (FlagWarConfig.getSecondsOfFlag()*20L+FlagWarConfig.getSecondsOfPreFlag()*20L)/20 + " seconds!");

    }

    @EventHandler
    public void onWarEnd(WarEndEvent e) {

        server.broadcastMessage(ChatColor.BLUE + "[" + ChatColor.YELLOW + "FLAGWAR" + ChatColor.BLUE + "] " + ChatColor.WHITE + "The battle between " + e.getAttackingNation().getName() + " and " + e.getDefendingNation().getName() + " at " + e.getAttackedTown().getName() + " has ended!");

        if (e.getWarEndReason() == WarEndEvent.WarEndReason.timerRanOut) {
            server.broadcastMessage(ChatColor.BLUE + "[" + ChatColor.YELLOW + "FLAGWAR" + ChatColor.BLUE + "] " + ChatColor.WHITE + e.getDefendingNation().getName() + " has successfully defended " + e.getAttackedTown().getName() + " from " + e.getAttackingNation().getName() + "!");

        } else if (e.getWarEndReason() == WarEndEvent.WarEndReason.homeBlockCellWon) {
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
        WarInfo warInfo = warManager.getWarInfoOrNull(tb.getTownOrNull());
        warManager.removeFlagFromWar(warInfo, flagPlacer);
    }

    @EventHandler
    public void onCellDefended(CellDefendedEvent e)
    {
        WarInfo warInfo = warManager.getWarInfoOrNull(TownyAPI.getInstance().getTownBlock(e.getCell().getAttackData().getFlagBaseBlock().getLocation()).getTownOrNull());
        String flagPlacer = e.getCell().getAttackData().getNameOfFlagOwner();

        if (warInfo != null) warManager.removeFlagFromWar(warInfo, flagPlacer);
    }

    // this is the ugliest function in the entire codebase
    // i really think i'll just keep it like that if it works.

    @EventHandler
    public void onPotentialFlagLiveIncrease(PlayerInteractEvent e)
    {
        if (e.getAction().isRightClick() && e.getHand() == EquipmentSlot.HAND && e.getClickedBlock() != null)
        {
            Block potentialFlagBlock = e.getClickedBlock();
            TownBlock tb = TownyAPI.getInstance().getTownBlock(potentialFlagBlock.getLocation());

            if (   tb == null
                || tb.getTownOrNull() == null
                || !warManager.hasActiveWar(tb.getTownOrNull())) return;


            WarInfo warInfo = warManager.getWarInfoOrNull(tb.getTownOrNull());
            FlagInfo currentFlag = WarManager.getFlagInfoOrNull(warInfo, potentialFlagBlock.getLocation());

            if (currentFlag != null && currentFlag.getFlagBlock() != null && currentFlag.getFlagBlock().equals(potentialFlagBlock))
            {
                ItemStack itemHeld = e.getPlayer().getInventory().getItemInMainHand();
                Player p = e.getPlayer();
                Resident r = TownyAPI.getInstance().getResident(p.getUniqueId());

                Material requiredMaterial = Material.valueOf(FlagWarConfig.getItemOfPaymentForFlagLives());

                if (itemHeld.getType() == requiredMaterial
                    && (r.getNationOrNull().isAlliedWith(currentFlag.getFlagPlacer().getNationOrNull()) || r.getNationOrNull().equals(currentFlag.getFlagPlacer().getNationOrNull()))
                    && r.getNationRanks() != null)
                {
                    if (currentFlag.getPotentialExtraLives() == 3 && itemHeld.getAmount() >= FlagWarConfig.getPriceToIncreaseFlagLives(1))
                    {
                        int extraTimeTicks = FlagWarConfig.getExtraTimeSecondsPerFlagLife()*20;
                        int price = FlagWarConfig.getPriceToIncreaseFlagLives(1);

                        itemHeld.setAmount(itemHeld.getAmount()-price);
                        p.getInventory().setItemInMainHand(itemHeld); // itemHeld is a copy, so we have to update it.
                        p.sendMessage("You have exchanged " + price + " " + requiredMaterial.name().toLowerCase() + "s for 1 extra life!");

                        warManager.addExtraFlagLife(currentFlag, extraTimeTicks);

                        long delay = FlagWarConfig.getSecondsUntilLockedFlagLives()* 20L;
                        System.out.println(delay);
                        new BukkitRunnable() {@Override public void run() {currentFlag.setLivesFrozen(true);}}.runTaskLater(JavaPlugin.getProvidingPlugin(this.getClass()), delay);

                    }

                    else if (currentFlag.getPotentialExtraLives() == 2 && itemHeld.getAmount() >= FlagWarConfig.getPriceToIncreaseFlagLives(2) && !currentFlag.isLivesFrozen())
                    {
                        int price = FlagWarConfig.getPriceToIncreaseFlagLives(2);
                        int extraTimeTicks = FlagWarConfig.getExtraTimeSecondsPerFlagLife()*20;

                        itemHeld.setAmount(itemHeld.getAmount()-price);
                        p.getInventory().setItemInMainHand(itemHeld); // itemHeld is a copy, so we have to update it.
                        p.sendMessage("You have exchanged " + price + " " + requiredMaterial.name().toLowerCase() + "s for 1 extra life!");

                        warManager.addExtraFlagLife(currentFlag, extraTimeTicks);
                    }

                    else if (currentFlag.getPotentialExtraLives() == 1 && itemHeld.getAmount() >= FlagWarConfig.getPriceToIncreaseFlagLives(3) && !currentFlag.isLivesFrozen())
                    {
                        int price = FlagWarConfig.getPriceToIncreaseFlagLives(3);
                        int extraTimeTicks = FlagWarConfig.getExtraTimeSecondsPerFlagLife()*20;

                        itemHeld.setAmount(itemHeld.getAmount()-price);
                        p.getInventory().setItemInMainHand(itemHeld); // itemHeld is a copy, so we have to update it.
                        p.sendMessage("You have exchanged " + price + " " + requiredMaterial.name().toLowerCase() + "s for 1 extra life!");

                        warManager.addExtraFlagLife(currentFlag, extraTimeTicks);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onEligibleToFlag(EligibleToFlagEvent e)
    {
        server.broadcastMessage(e.getAttackedTown().getName() + "'s flag state is now ON. You may now flag!");
    }
}
