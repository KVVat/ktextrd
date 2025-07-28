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

import android.device_security.debug.HexDump
import android.device_security.ext2rd.extent.Extent

class Inode {
    var i_mode: UShort = 0u //  000
    var i_uid: UShort = 0u //  002
    var i_size: UInt = 0u //  004
    var i_atime: UInt = 0u //  008
    var i_ctime: UInt = 0u //  00c
    var i_mtime: UInt = 0u //  010
    var i_dtime: UInt = 0u //  014
    var i_gid: UShort = 0u //  018
    var i_links_count: UShort = 0u //  01a
    var i_blocks: ULong = 0u //  01c// 512 byte blocks
    var i_flags: UInt = 0u //  020
    var i_osd1: UInt = 0u //  024

    var i_block: Array<UInt> = Array(15) { 0u } //  028

    var symlink: String = ""
    var e: Extent = Extent(this) //  064

    var i_generation: UInt = 0u //  064
    var i_file_acl: UInt = 0u //  068
    var i_dir_acl: UInt = 0u //  06c
    var i_faddr: UInt = 0u //  070

    var i_osd2: UByteArray = UByteArray(12) { 0u } //  074

    var _empty:Boolean=true

    fun parse(buf:UByteArray){
        var p= 0
        _empty  = buf.find { it.toUInt() != 0x0u } == null
        //handle first blocks
        i_mode  = buf.get16le(p);    p+=2;
        i_uid   = buf.get16le(p);    p += 2
        i_size  = buf.get32le(p);    p += 4
        i_atime = buf.get32le(p);    p += 4
        i_ctime = buf.get32le(p);    p += 4
        i_mtime = buf.get32le(p);    p += 4
        i_dtime = buf.get32le(p);    p += 4
        i_gid   = buf.get16le(p);    p += 2
        i_links_count = buf.get16le(p);    p += 2
        i_blocks = buf.get32le(p).toULong();    p += 4
        i_flags  = buf.get32le(p);    p += 4
        i_osd1   = buf.get32le(p);    p += 4

        //block and extent handling
        if(issymlink()){
            symlink = ubytestostr(buf,p,p+60);
            p+=60
        } else if(i_flags and Constants.EXT4_EXTENTS_FL >0u){
            e.parse(buf,p)
            p+=60
        } else {
            for(i in 0..<15){
                i_block[i] = buf.get32le(p);p+=4
            }
        }
        //handle after blocks
        i_generation = buf.get32le(p);    p += 4
        i_file_acl   = buf.get32le(p);    p += 4
        i_dir_acl    = buf.get32le(p);    p += 4
        i_faddr      = buf.get32le(p);    p += 4
        //handle osd2 blocks
        i_osd2       = buf.copyOfRange(p, p + 12);p+=12

        //if(!_empty) println(HexDump.hexdump(buf.toByteArray()))
    }
    fun issymlink():Boolean {
        return ((i_mode and 0xf000u).toInt() == Constants.EXT4_S_IFLNK && i_size < 60u)
    }
    fun datasize():ULong {
        return i_size.toULong();
    }

    fun dump(){
        print(String.format("m:%06o(%02x) %4d o[%5d %5d] t[%10d %10d %10d %10d]  %d [b:%8d] F:%05x(%s) X:%08x %s\n",
            i_mode.toInt(),i_mode.toInt() ,i_links_count.toInt(), i_gid.toInt(), i_uid.toInt(), i_atime.toInt(), i_ctime.toInt(), i_mtime.toInt(), i_dtime.toInt(),
            datasize().toInt(), i_blocks.toLong(), i_flags.toInt(), fl2str(i_flags), i_file_acl.toInt(),
            i_osd2.toHex()))

        if (issymlink()) {
            print(String.format("symlink: %s\n",symlink))
        } else if (i_flags and Constants.EXT4_EXTENTS_FL >0u) {
            e.dump()
        } else {
            for (i in 0..11) print(String.format(" %08x", i_block[i].toInt()))
            print(String.format("  i1:%08x, i2:%08x, i3:%08x\n",
                i_block[12].toInt(), i_block[13].toInt(), i_block[14].toInt()))
        }
    }
    fun fl2str(fl:UInt):String
    {
        val l = mutableListOf<String>()

        val all = Constants.EXT4_SECRM_FL or Constants.EXT4_UNRM_FL or Constants.EXT4_COMPR_FL or
                Constants.EXT4_SYNC_FL or Constants.EXT4_IMMUTABLE_FL or Constants.EXT4_APPEND_FL or
                Constants.EXT4_NODUMP_FL or Constants.EXT4_NOATIME_FL or Constants.EXT4_DIRTY_FL or
                Constants.EXT4_COMPRBLK_FL or Constants.EXT4_NOCOMPR_FL or Constants.EXT4_ECOMPR_FL or
                Constants.EXT4_INDEX_FL or Constants.EXT4_IMAGIC_FL or Constants.EXT4_JOURNAL_DATA_FL or
                Constants.EXT4_NOTAIL_FL or Constants.EXT4_DIRSYNC_FL or Constants.EXT4_TOPDIR_FL or
                Constants.EXT4_HUGE_FILE_FL or Constants.EXT4_EXTENTS_FL or Constants.EXT4_EA_INODE_FL or
                Constants.EXT4_EOFBLOCKS_FL or Constants.EXT4_INLINE_DATA_FL or Constants.EXT4_RESERVED_FL

        if (fl and Constants.EXT4_SECRM_FL != 0u) l.add("SECRM")
        if (fl and Constants.EXT4_UNRM_FL != 0u) l.add("UNRM")
        if (fl and Constants.EXT4_COMPR_FL != 0u) l.add("COMPR")
        if (fl and Constants.EXT4_SYNC_FL != 0u) l.add("SYNC")
        if (fl and Constants.EXT4_IMMUTABLE_FL != 0u) l.add("IMMUTABLE")
        if (fl and Constants.EXT4_APPEND_FL != 0u) l.add("APPEND")
        if (fl and Constants.EXT4_NODUMP_FL != 0u) l.add("NODUMP")
        if (fl and Constants.EXT4_NOATIME_FL != 0u) l.add("NOATIME")
        if (fl and Constants.EXT4_DIRTY_FL != 0u) l.add("DIRTY")
        if (fl and Constants.EXT4_COMPRBLK_FL != 0u) l.add("COMPRBLK")
        if (fl and Constants.EXT4_NOCOMPR_FL != 0u) l.add("NOCOMPR")
        if (fl and Constants.EXT4_ECOMPR_FL != 0u) l.add("ECOMPR")
        if (fl and Constants.EXT4_INDEX_FL != 0u) l.add("INDEX")
        if (fl and Constants.EXT4_IMAGIC_FL != 0u) l.add("IMAGIC")
        if (fl and Constants.EXT4_JOURNAL_DATA_FL != 0u) l.add("JOURNAL_DATA")
        if (fl and Constants.EXT4_NOTAIL_FL != 0u) l.add("NOTAIL")
        if (fl and Constants.EXT4_DIRSYNC_FL != 0u) l.add("DIRSYNC")
        if (fl and Constants.EXT4_TOPDIR_FL != 0u) l.add("TOPDIR")
        if (fl and Constants.EXT4_HUGE_FILE_FL != 0u) l.add("HUGE_FILE")
        if (fl and Constants.EXT4_EXTENTS_FL != 0u) l.add("EXTENTS")
        if (fl and Constants.EXT4_EA_INODE_FL != 0u) l.add("EA_INODE")
        if (fl and Constants.EXT4_EOFBLOCKS_FL != 0u) l.add("EOFBLOCKS")
        if (fl and Constants.EXT4_INLINE_DATA_FL != 0u) l.add("INLINE_DATA")
        if (fl and Constants.EXT4_RESERVED_FL != 0u) l.add("RESERVED")

        if ((fl and all.inv()).toInt() != 0) l.add(String.format("unk_%x", (fl and all.inv()).toInt()))

        return l.joinToString (",")
    }

    fun enumblocks(super_: SuperBlock, cb:(ub:ByteArray)->Boolean):Boolean{
        if(issymlink()){
            //noblocks
        } else if(i_flags and Constants.EXT4_EXTENTS_FL > 0u){
            //println("******ext4 extents flag is set******")
            return enumextents(super_,cb)>0
        }

        var bytes:ULong=0u;
        for(i in 0..11){
            if(bytes>i_size) {
                break
            } else if(i_block[i]>0u){
                if(!cb(super_.getblock(i_block[i])))
                    return false;
                bytes+=super_.blocksize()
            }
        }

        //for referring big data blocks ext2 keep inode ptr in indirect data blocks
        //pass value by an array to use pass-by-reference
        if(i_block[12]>0u){
            println("!!iblock12 data!! ${i_block[12]}")
            if(!enumi1block(super_,arrayOf(bytes),super_.getblock(i_block[12]).toUByteArray(),cb)){
                return false;
            }
        }
        if(i_block[13]>0u){
            println("!!iblock13 data in use!! ${i_block[13]}")
            if(!enumi2block(super_,arrayOf(bytes),super_.getblock(i_block[13]).toUByteArray(),cb)){
                return false;
            }
        }
        if(i_block[14]>0u){
            println("!!iblock14 data in use!!")
            if(!enumi3block(super_,arrayOf(bytes),super_.getblock(i_block[14]).toUByteArray(),cb)){
                return false;
            }
        }
        //process blocks//
        return true
    }
    fun enumi1block(super_: SuperBlock, bytes:Array<ULong>, pblock:UByteArray, cb:(ub:ByteArray)->Boolean):Boolean{
        if(pblock.size == 0){
            return true
        }
        var p = 0;
        val last:Int = super_.blocksize().toInt();//.toUInt()
        while(p<last && bytes[0]<i_size){
            val blocknr:UInt = pblock.get32le(p)
            if(blocknr>0u){
                if(!cb(super_.getblock(blocknr)))
                    return false
            }
            p+=4
            bytes[0]+=super_.blocksize();
        }
        return true
    }
    fun enumi2block(super_: SuperBlock, bytes:Array<ULong>, pblock:UByteArray, cb:(ub:ByteArray)->Boolean):Boolean{
        if(pblock.size == 0){
            return true
        }
        var p = 0u;
        val last:UInt = super_.blocksize().toUInt()
        while(p<last && bytes[0]<i_size){
            if(!enumi1block(super_,bytes,super_.getblock(p++).toUByteArray(),cb))
                return false
        }
        return true
    }
    fun enumi3block(super_: SuperBlock, bytes:Array<ULong>, pblock:UByteArray, cb:(ub:ByteArray)->Boolean):Boolean{
        if(pblock.size == 0){
            return true
        }
        var p = 0u;
        val last:UInt = super_.blocksize().toUInt()
        while(p<last && bytes[0]<i_size){
            if(!enumi2block(super_,bytes,super_.getblock(p++).toUByteArray(),cb))
                return false
        }
        return true
    }
    fun enumextents(super_: SuperBlock, cb:(ub:ByteArray)->Boolean):Int{
        return e.enumblocks(super_,cb)
    }
    fun modestr():String{
        var result = CharArray(10){' '}
        val typechar = "?pc?d?b?-?l?s???".toCharArray()
        val imode = i_mode.toInt()
        result[0] = typechar[imode shr 12]
        rwx(result,1,(imode shr 6) and 7, (imode shr 11) and 1,'s')
        rwx(result,4,(imode shr 3) and 7, (imode shr 10) and 1,'s')
        rwx(result,7,(imode)       and 7, (imode shr 9 ) and 1,'t')
        return result.concatToString()
    }
    companion object {
        fun rwx(ar:CharArray,ptr:Int, bits:Int, extra:Int, xchar:Char){
            ar[ptr+0] = if((bits and 4)>0) 'r' else '-'
            ar[ptr+1] = if((bits and 2)>0) 'w' else '-'
            if(extra>0){
                val xc = xchar.code
                //~0x20 => 0xdf
                ar[ptr+2] =
                    if((bits and 1)>0) (xc and 0xdf).toChar() else (xc or 0x20).toChar()
            } else {
                ar[ptr+2] = if((bits and 1)>0) 'x' else '-'
            }
        }
    }

}