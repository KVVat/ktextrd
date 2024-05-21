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

class BlockGroup {

    var itableoffset: ULong = 0uL
    var ninodes: ULong = 0uL
    var inodesize: ULong = 0uL
    var fp: IReadWriter? = null
    var idx=0;

    fun parse(first:ULong, fp_: IReadWriter, superblk: SuperBlock, bgd: BlockGroupDescriptor,i:Int) {
        this.fp = fp_ //reserve file pointer to read inode
        //In some case first was too large to reference
        itableoffset = first+((bgd.bg_inode_table%superblk.s_blocks_per_group)*superblk.blocksize())
        ninodes = superblk.s_inodes_per_group.toULong()
        inodesize = superblk.s_inode_size.toULong()
        idx = i;

    }
    fun dump(superblk: SuperBlock, inodebase: UInt)
    {
        println(String.format("block group (%d) offset=0x%x num=%d insize=%d",idx,
            itableoffset.toInt(),ninodes.toLong(),inodesize.toLong()))

        enuminodes(inodebase){ nr,ino ->
            print(String.format("INO %d: ", nr.toInt()));
            ino.dump()
            if((ino.i_mode and 0xf000u) == Constants.EXT4_S_IFDIR.toUShort()){
                print("dir node:\n")
                ino.enumblocks(superblk){ br ->
                    if(br[0].toUInt() == 0u){
                        //println("invalid blocknr")
                        return@enumblocks true
                    }
                    dumpdirblock(br.toUByteArray(),superblk.blocksize())
                    return@enumblocks true
                }
            }
        }
    }
    fun enuminodes(inodebase:UInt, cb:(a:UInt,b: Inode)->Unit){
        for(i in 0..(ninodes- 1u).toInt()){
            val ino  = getinode(i.toUInt())
            if(!ino._empty)
                cb(inodebase+i.toUInt(),ino)
        }
    }

    companion object {
        fun dumpdirblock(br:UByteArray,blocksize:ULong){
            var p = 0u;
            while(p<blocksize){
                val ent = DirectoryEntry()
                val n:UShort = ent.parse(br,p);
                if(n.toUInt() == 0u)
                    break;
                ent.dump()
                p+=n;
            }
        }
    }

    fun getinode(nr: UInt): Inode {
        val insize = inodesize.toInt();//there's 256 byte inode
        val buf = ByteArray(insize)

        fp?.seek((itableoffset+(inodesize*nr)).toLong())
        fp?.read(buf,insize.toLong())
        val ino = Inode()
//        println(String.format("inode ptr =%x %x",itableoffset.toInt(),
//            (itableoffset+(inodesize*nr)).toInt()))
        ino.parse(buf.toUByteArray())

        return ino
    }
}