package io.github.townyadvanced.flagwar.chunkManipulation;

import com.palmergames.bukkit.towny.TownyAPI;
import io.github.townyadvanced.flagwar.config.FlagWarConfig;
import io.github.townyadvanced.flagwar.objects.ChunkCoordPair;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class PasteChunk {

    JavaPlugin plugin = JavaPlugin.getProvidingPlugin(this.getClass());
    int initialChunksToPaste;
    Stack<ChunkCoordPair> globalCCPs = new Stack<>();
    int batchesLeft;
    int sizeOfBatch;
    String townName;

    public void initiatePaste(Collection<ChunkCoordPair> CCPs, int sizeOfBatch, World world) {

        initialChunksToPaste = CCPs.size();
        for (var ccp : CCPs)
            globalCCPs.push(ccp);
        townName = TownyAPI.getInstance().getTownName(new Location(world, globalCCPs.peek().getX()*16, 0, globalCCPs.peek().getZ()*16));

        this.sizeOfBatch = sizeOfBatch;
        // there can always be one batch of 0.
        batchesLeft = Math.ceilDiv(globalCCPs.size(), sizeOfBatch);

        processNextBatch(world);
    }

    void processNextBatch(World world)
    {
        if (batchesLeft == 0)
        {
            Bukkit.getServer().broadcastMessage(ChatColor.BLUE + "[" + ChatColor.YELLOW + "FLAGWAR" + ChatColor.BLUE + "] " + ChatColor.WHITE + townName + " has been restored!");
            System.out.println("Successfully pasted and deleted " + initialChunksToPaste + " chunk(s)!");
            return;
        }

        Stack<ChunkCoordPair> CCPSection = new Stack<>();

        int trueSizeOfBatch = Math.min(sizeOfBatch, globalCCPs.size());

        for (int i = 0; i < trueSizeOfBatch; i++)
            CCPSection.push(globalCCPs.pop());

        readChunks(world, CCPSection);
    }


    void readChunks(World world, Stack<ChunkCoordPair> CCPs) {

        Stack<PendingChunk> pendingChunks = new Stack<>();

        int initialChunksToPaste = CCPs.size();
        int loadPerTask = (initialChunksToPaste / FlagWarConfig.getActiveTasksPerBatchRead()) + 1;
        int activeTasks = Math.ceilDiv(CCPs.size(), loadPerTask);

        for (int n = 0; n < activeTasks; n++) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        ChunkCoordPair ccp = CCPs.pop();
                        File chunkFile = new File(plugin.getDataFolder(), "chunks/" + ccp.getX() + "_" + ccp.getZ());

                        if (!chunkFile.exists())
                            System.out.println("Error: Chunk file doesn't exist!");

                        try (FileInputStream fis = new FileInputStream(chunkFile); ObjectInputStream ois = new ObjectInputStream(fis)) {

                            PendingChunk pendingChunk = new PendingChunk(ccp.getX(), ccp.getZ());

                            pendingChunk.setMaterials((String[]) ois.readObject());
                            pendingChunk.setBlockDatas((String[]) ois.readObject());

                            pendingChunks.push(pendingChunk);

                            ois.close();
                            fis.close();

                            Files.deleteIfExists(Path.of(chunkFile.getPath()));

                        } catch (IOException | ClassNotFoundException e) {
                            e.printStackTrace();
                        }

                    } catch (EmptyStackException e) {
                        cancel();
                    }
                }
            }.runTaskTimerAsynchronously(plugin, 1, 1);
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                if (CCPs.isEmpty() && pendingChunks.size() == initialChunksToPaste) {
                    pasteChunks(world, pendingChunks);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 5, 2);
    }

    void pasteChunks(World world, Stack<PendingChunk> pendingChunks) {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (int n = 0; n < FlagWarConfig.getChunksPastedPerTick(); n++) {

                    PendingChunk pendingChunk;

                    try {pendingChunk = pendingChunks.pop();}

                    catch (EmptyStackException e)
                    {
                        batchesLeft--;
                        processNextBatch(world);
                        cancel();
                        break;
                    }

                    Chunk thisChunk = world.getChunkAt(pendingChunk.getX(), pendingChunk.getZ());

                    for (int i = 0; i < pendingChunk.getBlockDatas().length; i++) {
                        int x = i % 16;
                        int z = (i / 16) % 16;
                        int y = (i) / 256;
                        int ny = y - 64;

                        Block thisBlock = thisChunk.getBlock(x, ny, z);

                        if (pendingChunk.getMaterials()[i] == null) thisBlock.setType(Material.AIR);

                        else {
                            thisBlock.setType(Material.getMaterial(pendingChunk.getMaterials()[i]));

                            if (pendingChunk.getBlockDatas()[i] != null)
                                thisBlock.setBlockData(Bukkit.createBlockData(pendingChunk.getBlockDatas()[i]));
                        }
                    }

                    for (var entity : thisChunk.getEntities())
                        if (entity instanceof Item) entity.remove();
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }
}
