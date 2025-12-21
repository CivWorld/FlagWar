package io.github.townyadvanced.flagwar.objects;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import io.github.townyadvanced.flagwar.events.WarEndEvent;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class WarInfo {
    private final Town attackedTown;
    private final Nation attackingNation;
    private final Nation defendingNation;
    private final Resident initialMayor;
    private final ArrayList<FlagInfo> activeFlags;
    private FlagState currentFlagState;
    private PersistentRunnable currentRunnable;
    private final Collection<ChunkCoordPair> storableTownBlocks;
    private WarEndEvent.WarEndReason warEndReason; // if null, the war hasn't ended yet.

    public WarInfo(Town attackedTown, Nation attackingNation, Nation defendingNation, Resident initialMayor, FlagState flagState, PersistentRunnable currentRunnable, Collection<ChunkCoordPair> chunkCoordPairs)
    {
        this.attackedTown = attackedTown;
        this.attackingNation = attackingNation;
        this.defendingNation = defendingNation;
        this.initialMayor = initialMayor;
        currentFlagState = flagState;
        activeFlags = new ArrayList<>();
        this.currentRunnable = currentRunnable;
        this.storableTownBlocks = chunkCoordPairs;
    }

    public WarInfo(Town attackedTown, Nation attackingNation, Nation defendingNation, Resident initialMayor, FlagState flagState, PersistentRunnable currentRunnable, boolean writeToYML) {
        this.attackedTown = attackedTown;
        this.attackingNation = attackingNation;
        this.defendingNation = defendingNation;
        this.initialMayor = initialMayor;
        currentFlagState = flagState;
        activeFlags = new ArrayList<>();
        this.currentRunnable = currentRunnable;
        this.storableTownBlocks = ChunkCoordPair.of(attackedTown.getTownBlocks());

        if (writeToYML)
        {
            File warInfoFile = new File(JavaPlugin.getProvidingPlugin(this.getClass()).getDataFolder(), "ActiveWars.yml");

            try {
                String key = attackedTown.getUUID().toString();
                YamlConfiguration warInfoConfig = YamlConfiguration.loadConfiguration(warInfoFile);

                warInfoConfig.set(key + ".attackingNation", attackingNation.getName());
                warInfoConfig.set(key + ".defendingNation", defendingNation.getName());
                warInfoConfig.set(key + ".initialMayor", attackedTown.getMayor().getUUID().toString());
                warInfoConfig.set(key + ".flagState", flagState.toString());
                warInfoConfig.set(key + ".currentRunnable", currentRunnable.getPathAsString());
                warInfoConfig.set(key + ".townBlocks", ChunkCoordPair.getStringCoordsOfCollection(",", ";", storableTownBlocks));

                warInfoConfig.save(warInfoFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public Resident getInitialMayor() {return initialMayor;}
    public Town getAttackedTown() {return attackedTown;}
    public Nation getAttackingNation() {return attackingNation;}
    public Nation getDefendingNation() {return defendingNation;}
    public ArrayList<FlagInfo> getCurrentFlags() {return activeFlags;}
    public FlagState getCurrentFlagState() {return currentFlagState;}
    public PersistentRunnable getCurrentRunnable() {return this.currentRunnable;}
    public void setWarEndReason(WarEndEvent.WarEndReason warEndReason) {this.warEndReason = warEndReason;}
    public WarEndEvent.WarEndReason getWarEndReason() {return warEndReason;}

    public void setCurrentRunnable(PersistentRunnable currentRunnable)
    {
        this.currentRunnable = currentRunnable;
        File warInfos = new File(JavaPlugin.getProvidingPlugin(this.getClass()).getDataFolder(), "ActiveWars.yml");
        if (warInfos.exists())
        {
            String path = currentRunnable != null ? currentRunnable.getPathAsString() : null;
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(warInfos);
                config.set(attackedTown.getUUID() + ".currentRunnable", path);
                config.save(warInfos);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }
    public Collection<ChunkCoordPair> getStorableTownBlocks() {return storableTownBlocks;}

    public void setCurrentFlagState(FlagState currentFlagState)
    {
        this.currentFlagState = currentFlagState;
        File warInfos = new File(JavaPlugin.getProvidingPlugin(this.getClass()).getDataFolder(), "ActiveWars.yml");
        if (warInfos.exists())
        {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(warInfos);
                config.set(attackedTown.getUUID() + ".flagState", currentFlagState.toString());
                config.save(warInfos);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
