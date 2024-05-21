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

package android.device_security.ext2rd
//Notice : GroupDescriptor has 64 pattern
class BlockGroupDescriptor{
    var bg_block_bitmap: UInt = 0u
    var bg_inode_bitmap: UInt = 0u
    var bg_inode_table: UInt = 0u
    var bg_free_blocks_count: UShort = 0u
    var bg_free_inodes_count: UShort = 0u
    var bg_used_dirs_count: UShort = 0u
    var bg_pad: UShort = 0u

    @OptIn(ExperimentalStdlibApi::class, ExperimentalUnsignedTypes::class)
    fun parse(b: UByteArray, offset:Int) {
        //println("BGD:"+b.toHexString(HexFormat.Default))
        var p= offset
        bg_block_bitmap = b.get32le(p); p += 4
        bg_inode_bitmap = b.get32le(p); p += 4
        bg_inode_table  = b.get32le(p); p += 4
        bg_free_blocks_count = b.get16le(p); p+=2
        bg_free_inodes_count = b.get16le(p); p+=2
        bg_used_dirs_count   = b.get16le(p); p+=2
        bg_pad = b.get16le(p); p+=2
        if(p-offset != 20)
            println(String.format("bgdesc size error: %ld",p-offset))

    }
    fun dump(){
        print(String.format("block group desc B:%d, I:%d, T:%d, free:B=%d, I=%d, used:D=%d\n",
            bg_block_bitmap.toInt(),bg_inode_bitmap.toInt(),bg_inode_table.toInt(),
            bg_free_blocks_count.toInt(),bg_free_inodes_count.toInt(),bg_used_dirs_count.toInt()
        ))
    }
}