package io.github.townyadvanced.flagwar.chunkManipulation;

import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import org.bukkit.*;
import org.bukkit.block.data.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;

public class CopyChunk {
    JavaPlugin plugin = JavaPlugin.getProvidingPlugin(this.getClass());


    public void initiateCopy(World world, Collection<TownBlock> townBlocks) {

        ArrayList<ChunkSnapshot> snapshots = new ArrayList<>();

        for (var tb : townBlocks) {
            snapshots.add(world.getChunkAt(tb.getX(), tb.getZ()).getChunkSnapshot());
        }

        for (var thisSnap : snapshots) {

            String[] materials = new String[16 * 16 * 384];
            String[] blockDatas = new String[16 * 16 * 384];

            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = -64; y < 320; y++) {
                        int ny = y + 64; // it's going to otherwise be out of bounds because y is negative

                        BlockData d = thisSnap.getBlockData(x,y,z);
                        if (thisSnap.getBlockType(x,y,z) != Material.AIR)
                        {
                            materials[x + (z*16) + (ny*16*16)] = thisSnap.getBlockType(x,y,z).toString();

                            if (   d instanceof Directional
                                || d instanceof Ageable
                                || d instanceof Waterlogged
                                || d instanceof Powerable
                                || d instanceof Openable
                                || d instanceof Bisected
                                || d instanceof Lightable
                                || d instanceof Levelled
                                || d instanceof Rotatable
                                || d instanceof MultipleFacing) // if the BlockData is actually useful and not just material
                                    blockDatas[x + (z*16) + (ny*16*16)] = thisSnap.getBlockData(x,y,z).getAsString();                    }
                        }
                }
            }

            new BukkitRunnable() {
                @Override
                public void run() {
                    File chunkFile = new File(plugin.getDataFolder(), "chunks/" + thisSnap.getX() + "_" + thisSnap.getZ());
                    chunkFile.getParentFile().mkdirs();

                    try (FileOutputStream fos = new FileOutputStream(chunkFile);
                         ObjectOutputStream oos = new ObjectOutputStream(fos)) {
                        oos.writeObject(materials);
                        oos.writeObject(blockDatas);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }.runTaskAsynchronously(plugin);
        }
    }
}
