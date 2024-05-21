/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(ExperimentalUnsignedTypes::class)

package android.device_security.ext2rd

import android.device_security.ext2rd.reader.IReadWriter

//a.k.a Volume
class Ext2FileSystem {

    var superblk: SuperBlock = SuperBlock()
    var bgdescs: MutableList<BlockGroupDescriptor> = mutableListOf()
    var groups: MutableList<BlockGroup> = mutableListOf()
    var sb_offset: ULong = 0u
    var rootdir_in: ULong = 0u

    fun parse(fp: IReadWriter) {
        fp.seek(this.sb_offset.toLong())
        superblk.parse(fp)
        if(superblk.s_magic.toUInt() != 0xef53u)
            throw RuntimeException("not an ext2 fs")
        //println(fp._off)
        parseGroupDescs(fp)
        //println(fp._off)
        //Block Groups : contains inode

        var off: ULong = 0u
        for(i in 0..superblk.ngroups().toInt()-1){
            val bg = BlockGroup()
            bg.parse(off,fp,superblk,bgdescs[i],i)
            groups.add(bg)
            if(!superblk.flex_bg_support){
                off += superblk.bytespergroup();
            } else {
                //if the images support flex_bg feature:
                //(bgd.bg_inode_table%superblk.s_blocks_per_group)*superblk.blocksize()
                //point the offset of inodes. so we can skip adding offsets
            }
        }
    }

    fun parseGroupDescs(fp: IReadWriter) {
        var bgdescopos:ULong
        // When the blocksize == 1024, the superblock fills block 1
        // and the block group descriptors start with block 2.
        // For larger sizes the superblock fits inside block 0
        // and the block group descriptors start with block 1.
        if(superblk.blocksize().toInt() == 1024){
            bgdescopos = 2048u
        } else {
            bgdescopos = superblk.blocksize()
        }
        fp.seek(bgdescopos.toLong())

        val ngs_ = superblk.ngroups().toInt()
        val buf = ByteArray(ngs_ * 32)

        fp.read(buf,buf.size.toLong())
        //println(HexDump.hexdump(buf))
        for(i in 0..ngs_-1){
            val bgd = BlockGroupDescriptor()
            bgd.parse(buf.toUByteArray(),32*i)
            bgdescs.add(bgd)
        }
    }

    fun dump()
    {
        superblk.dump()
        var inodebase = 1u
        //println("group size="+groups.size+",")
        for(i in 0 until groups.size){
            bgdescs[i].dump()
            groups[i].dump(superblk, inodebase)//b0?
            inodebase+=superblk.s_inodes_per_group
        }
    }

    fun enuminodes(cb:(a:UInt,b: Inode)->Unit){
        var inodebase = 1u
        for(i in 0 until groups.size){
            groups[i].enuminodes(inodebase,cb)
            inodebase+=superblk.s_inodes_per_group
        }
    }

    fun getinode(nnr:UInt): Inode {

        var nr = nnr-1u;
        if(nr>=superblk.s_inodes_count) {
            nr=0u
        }
        val idx:Int = (nr/superblk.s_inodes_per_group).toInt()

        return groups[idx].getinode(nr%superblk.s_inodes_per_group)
    }
}