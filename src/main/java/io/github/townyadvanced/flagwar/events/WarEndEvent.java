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

package io.github.townyadvanced.flagwar.events;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class WarEndEvent extends Event implements Cancellable {
    private static final HandlerList h = new HandlerList();
    @Override
    public @NotNull HandlerList getHandlers() {return h;}
    public static @NotNull HandlerList getHandlerList() {return h;}

    Town attackedTown;
    Nation attackingNation;
    Nation defendingNation;
    boolean cancelled = false;
    public enum WarEndReason {homeBlockCellWon, timerRanOut}
    WarEndReason warEndReason;

    public WarEndEvent(Town attackedTown, Nation attackingNation, Nation defendingNation, WarEndReason warEndReason) {this.attackedTown = attackedTown; this.attackingNation = attackingNation; this.defendingNation =  defendingNation; this.warEndReason = warEndReason;}

    public Town getAttackedTown() {return attackedTown;}
    public Nation getAttackingNation() {return attackingNation;}
    public Nation getDefendingNation() {return defendingNation;}
    public WarEndReason getWarEndReason() {return warEndReason;}

    public void setCancelled(final boolean cancelled) {this.cancelled = cancelled;}
    public boolean isCancelled(){return cancelled;}
}
