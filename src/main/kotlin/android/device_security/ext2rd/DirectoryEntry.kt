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

class DirectoryEntry {
    var inode:UInt = 0u
    var filetype:UByte = 0u; // 1=file, 2=dir, 3=chardev, 4=blockdev, 5=fifo, 6=sock, 7=symlink
    var name:String=""

    fun parse(buf:UByteArray,p_:UInt):UShort
    {
        var p = p_.toInt();
        //handle first blocks
        inode = buf.get32le(p);    p+=4;
        val rec_len:UShort = buf.get16le(p); p+=2;
        val name_len = buf.get8(p);p+=1;
        filetype = buf.get8(p);p+=1;
        name = ubytestostr(buf,p,p+name_len.toInt());
        //buf.toByteArray().copyOfRange(p, p + name_len.toInt()).decodeToString().trim(Char(0))

        return rec_len;
    }
    fun dump()
    {
        println(String.format("%8d %c '%s'",inode.toInt(),"0-dcbpsl"[(filetype and 7u).toInt()],name))
    }
}