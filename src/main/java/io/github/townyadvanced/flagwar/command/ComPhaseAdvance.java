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

package io.github.townyadvanced.flagwar.command;

import com.palmergames.bukkit.towny.TownyAPI;
import io.github.townyadvanced.flagwar.FlagWar;
import io.github.townyadvanced.flagwar.WarManager;
import io.github.townyadvanced.flagwar.objects.PersistentRunnable;
import io.github.townyadvanced.flagwar.objects.WarInfo;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class ComPhaseAdvance implements CommandExecutor
{
    WarManager warManager = JavaPlugin.getPlugin(FlagWar.class).getWarManager();
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {

        if (strings.length != 1) return false;
        if (commandSender instanceof Player p && !p.isOp())
        {
            p.sendMessage(ChatColor.RED + "You do not have permission to execute this command.");
            return true;
        }

        WarInfo warInfo = warManager.getWarInfoOrNull(TownyAPI.getInstance().getTown(strings[0]));

        if (warInfo == null) {
            System.out.println("ERROR: " + strings[0] + " is not in a war or does not exist!");
            return true;
        }

        PersistentRunnable currentRunnable = warInfo.getCurrentRunnable();
        switch (warInfo.getCurrentFlagState())
        {
            case preFlag: if (currentRunnable != null) currentRunnable.cancel(); warManager.makeEligibleToFlag(warInfo); break;
            case flag: if (currentRunnable != null) currentRunnable.cancel(); warManager.winDefense(warInfo); break;
            case ruined, defended: if (currentRunnable != null) currentRunnable.cancel(); warManager.fullyEndWar(warInfo); break;
        }
        return true;
    }
}
