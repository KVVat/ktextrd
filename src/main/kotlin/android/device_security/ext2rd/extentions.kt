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

import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Date

//------------------------------------
// Little Endian Helpers
//------------------------------------
fun RandomAccessFile.read32le():UInt{
    val l:UShort = read16le()
    val u:UShort = read16le()
    val r:UInt = (u.toUInt() shl 16)+l
    return r
}

fun RandomAccessFile.read16le():UShort{
    val l:UByte = readByte().toUByte()
    val u:UByte = readByte().toUByte()
    val r:UInt = (u.toUInt() shl 8)+l
    return r.toUShort()
}

fun UByteArray.get64le(p:Int):ULong{
    val u:UInt = get32le(p+4)
    val l:UInt = get32le(p)
    val r:ULong = (u.toULong() shl 32)+l
    return r
}

fun UByteArray.get32le(p:Int):UInt{
    val u:UShort = get16le(p+2)
    val l:UShort = get16le(p)
    val r:UInt = (u.toUInt() shl 16)+l
    return r
}

fun UByteArray.get16le(p:Int):UShort{
    val u:UByte = get8(p+1)
    val l:UByte = get8(p)
    val r:UInt = (u.toUInt() shl 8)+l
    return r.toUShort()
}

fun UByteArray.get8(p:Int):UByte{
    return this[p]
}
fun UByteArray.copy32leOfRange(from:Int,to:Int):UIntArray
{
    val n = to-from
    if(n%4 != 0)
        throw RuntimeException("32le buffer can't divide by 4")
    val results = UIntArray(n/4)
    for(i in 0..n/4-1){
        results[i] = this.get32le(from+(i*4))
    }
    return results
}
fun ByteArray.memfill32le(val_: UInt,from:Int,to:Int) {
    val bytes = ByteBuffer.allocate(Int.SIZE_BYTES)
        .order(ByteOrder.LITTLE_ENDIAN).putInt(val_.toInt()).array()
    var n = to-from
    while(n-->0)
        this[n+from] = bytes[n and 3]
}

//------------------------------------
// String Helpers
//------------------------------------
fun timestr(t32:UInt):String
{
    val date: Date = Date(t32.toLong() * 1000)
    val format = SimpleDateFormat("yyyy-MM-dd HH:mm")
    return format.format(date)
}

fun ubytestostr(ub:UByteArray):String{
    return String(ub.toByteArray()).trim(Char(0))
}

fun ubytestostr(ub:UByteArray,first:Int,second:Int):String{
    val ba = ub.copyOfRange(first,second).toByteArray();
    return String(ba).trim(Char(0))
}

fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x ".format(eachByte) }
fun UByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x ".format(eachByte.toInt()) }
