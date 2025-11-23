package io.github.townyadvanced.flagwar.chunkManipulation;

import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;


public class pasteChunk {

    JavaPlugin plugin = JavaPlugin.getProvidingPlugin(this.getClass());

    class PendingChunk {
        private String[] materials;
        private String[] blockDatas;
        private int x;
        private int z;

        PendingChunk(String[] mats, String[] bds, int X, int Z) {
            materials = mats;
            blockDatas = bds;
            x = X;
            z = Z;
        }

        PendingChunk(int X, int Z) {
            materials = null;
            blockDatas = null;
            x = X;
            z = Z;
        }


        public PendingChunk() {

        }

        public int getX() {
            return x;
        }

        public int getZ() {
            return z;
        }

        public String[] getMaterials() {
            return materials;
        }

        public String[] getBlockDatas() {
            return blockDatas;
        }

        public void setX(int X) {
            x = X;
        }

        public void setZ(int Z) {
            z = Z;
        }

        public void setMaterials(String[] mats) {
            materials = mats;
        }

        public void setBlockDatas(String[] bds) {
            blockDatas = bds;
        }
    }

    private void pasteBlocks(World world, ArrayList<PendingChunk> pendingChunks) {
        System.out.println("Placing blocks now.");

        //if (busy) return;
        //busy = true;

        for (var pendingChunk : pendingChunks) {
            System.out.println("Pasting chunk.");
            Bukkit.getServer().broadcastMessage(pendingChunk.getX() + ", " + pendingChunk.getZ());
            Chunk thisChunk = world.getChunkAt(pendingChunk.getX(), pendingChunk.getZ());

            for (int i = 0; i < pendingChunk.blockDatas.length; i++) {
                int x = i % 16;
                int z = (i / 16) % 16;
                int y = (i) / 256;
                int ny = y - 64;

                Block thisBlock = thisChunk.getBlock(x, ny, z);
                if (pendingChunk.materials[i] == null) {
                    thisBlock.setType(Material.AIR);
                } else {
                    if (pendingChunk.getMaterials()[i] != null)
                    {
                        thisBlock.setType(Material.getMaterial(pendingChunk.getMaterials()[i]));
                        thisBlock.setBlockData(Bukkit.createBlockData(pendingChunk.getBlockDatas()[i]));
                    }
                }
            }
            for (var entity : thisChunk.getEntities()) if (entity instanceof Item) entity.remove();
            Bukkit.getServer().broadcastMessage("Chunk pasted!");
        }
    }

    public void pasteChunks(World world, Town town) {

        ArrayList<PendingChunk> pendingChunksToRead = new ArrayList<>();
        ArrayList<PendingChunk> pendingChunksToPlace = new ArrayList<>();
        int length = 0;

        for (var tb : town.getTownBlocks()) {
            pendingChunksToRead.add(new PendingChunk(tb.getX(), tb.getZ()));
            length++;
        }

        Bukkit.getServer().broadcastMessage(town.getTownBlocks().size()+"");

        int activeTasks = (Math.min(town.getTownBlocks().size(), 12));

        for (int i = 0; i < activeTasks; i++) {
            System.out.println("Beginning process");

            new BukkitRunnable() {
                @Override
                public void run() {
                    while (!pendingChunksToRead.isEmpty()) {
                        System.out.println("opening file");
                        PendingChunk pendingChunk = pendingChunksToRead.get(pendingChunksToRead.size()-1);
                        pendingChunksToRead.remove(pendingChunk);
                        File chunkFile = new File(plugin.getDataFolder(), "chunks/" + pendingChunk.getX() + "_" + pendingChunk.getZ());

                        if (!chunkFile.exists()) {
                            System.out.println("Error: Chunk file doesn't exist!");
                            continue;
                        }

                        try (FileInputStream fis = new FileInputStream(chunkFile); ObjectInputStream ois = new ObjectInputStream(fis)) {
                            pendingChunk.setMaterials((String[]) ois.readObject());
                            pendingChunk.setBlockDatas((String[]) ois.readObject());
                            pendingChunksToPlace.add(pendingChunk);
                            System.out.println("Chunk read. " + pendingChunk.getX() + " " + pendingChunk.getZ());
                            System.out.println(pendingChunksToPlace);
                            Bukkit.getServer().broadcastMessage(chunkFile.getPath());

                            Path path = Path.of(chunkFile.getPath());
                            // add some code to delete

                        } catch (IOException | ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    }

                    //cancel();
                }
            }.runTaskLaterAsynchronously(plugin, i);
        }

        int finalLength = length;
        new BukkitRunnable() {
            @Override
            public void run() {
                if (pendingChunksToRead.isEmpty() && pendingChunksToPlace.size() == finalLength) {
                    System.out.println("Task completed, now paste chunks.");
                    System.out.println(pendingChunksToPlace);
                    pasteBlocks(world, pendingChunksToPlace);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }
}
