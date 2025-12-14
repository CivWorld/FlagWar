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

package io.github.townyadvanced.flagwar.objects;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.TownBlock;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.Collection;

public class ChunkCoordPair
{
    int x;
    int z;

    public ChunkCoordPair(int x, int z)
    {
        this.x = x;
        this.z = z;
    }

    public static Collection<ChunkCoordPair> of(Collection<TownBlock> townBlocks)
    {
        ArrayList<ChunkCoordPair> collection = new ArrayList<>();

        for (var item : townBlocks)
        {
            collection.add(new ChunkCoordPair(item.getX(), item.getZ()));
        }

        return collection;
    }

    public static ChunkCoordPair of(String s, String delimiter)
    {
        String[] a = s.split(delimiter);
        return new ChunkCoordPair(Integer.parseInt(a[0]), Integer.parseInt(a[1]));
    }

    public int getX() {return x;}
    public int getZ() {return z;}

    public String getStringCoords(String delimiter) {return x+delimiter+z;}

    public static String getStringCoordsOfCollection(String coordDelimiter, String TBDelimiter, Collection<ChunkCoordPair> storableTownBlockCollection)
    {
        StringBuilder builder = new StringBuilder();
        for (var item : storableTownBlockCollection)
        {
            builder.append(item.getStringCoords(coordDelimiter));
            builder.append(TBDelimiter);
        }
        return builder.toString();
    }

    public static Collection<TownBlock> getTownBlocks(Collection<ChunkCoordPair> cPairs, World world)
    {
        ArrayList<TownBlock> townBlocks = new ArrayList<>();
        for (var c : cPairs)
        {
            world.getChunkAt(c.getX(), c.getZ());
            townBlocks.add(TownyAPI.getInstance().getTownBlock(new Location(world, c.getX()*16,100,  c.getZ()*16)));
        }
        return townBlocks;
    }

    public static ArrayList<ChunkCoordPair> getListOfChunkCoords(String coordDelimiter, String TBDelimiter, String s) {

        ArrayList<ChunkCoordPair> chunkCoordPairs = new ArrayList<>();
        String[] coord = s.split(TBDelimiter);

        for (var item : coord) {
            chunkCoordPairs.add(ChunkCoordPair.of(item, coordDelimiter));
        }
        return chunkCoordPairs;
    }
}
