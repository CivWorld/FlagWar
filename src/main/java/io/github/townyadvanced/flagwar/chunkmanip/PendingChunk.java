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

package io.github.townyadvanced.flagwar.chunkManipulation;

// this class contains enough informaton to fully reconstruct a chunk.
// this is without storing nbt data.

public class PendingChunk
{
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

    public PendingChunk(){}
    public int getX() {return x;}
    public int getZ() {return z;}
    public String[] getMaterials() {return materials;}
    public String[] getBlockDatas() {return blockDatas;}
    public void setX(int X) {x = X;}
    public void setZ(int Z) {z = Z;}
    public void setMaterials(String[] mats) {materials = mats;}
    public void setBlockDatas(String[] bds) {blockDatas = bds;}
}
