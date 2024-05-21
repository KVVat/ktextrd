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

package android.device_security.debug

class HexDump {
    //Simple BSD Style debug.HexDump by kotlin
    companion object {
        const val NON_PRINTABLE = '\u00B7';//'\u25a1'
        val HEX = arrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')

        fun hexdump(bytes:ByteArray,totalOffset:Int=0):String
        {
            if(bytes.isEmpty())
                return "empty"

            return buildString { ->
                var offset = 0
                while (offset < bytes.size) {
                    appendLine(bytes, offset, this,totalOffset)
                   // println(offset)
                    offset += 16
                }
            }
        }
        fun appendLine(bytes:ByteArray, first:Int, sbOut:StringBuilder,totalOffset: Int) {
            val firstBlockEnd: Int = Math.min(bytes.size, first+ 8)
            val secondBlockEnd: Int = Math.min(bytes.size, first + 16)

            appendOffset(first+totalOffset, sbOut);
            sbOut.append(' ').append(' ')
            appendHexBytes(bytes,first,firstBlockEnd,sbOut)
            sbOut.append(' ')//spacer
            appendHexBytes(bytes,first+8,secondBlockEnd,sbOut)
            padMissingBytes(first, secondBlockEnd, sbOut);
            sbOut.append(' ');
            appendDisplayChars(bytes,first,secondBlockEnd,sbOut)

            sbOut.append('\n');
        }
        fun appendHexBytes(bytes:ByteArray, first:Int, limit:Int,sbOut:StringBuilder)
        {
            for (i in first  until limit) {
                appendHexChars(bytes[i], sbOut)
                sbOut.append(' ')
            }
        }
        fun appendDisplayChars(bytes:ByteArray, first:Int, limit:Int,sbOut:StringBuilder)
        {
            for (i in first  until limit) {
                appendDisplayChar(bytes[i], sbOut)
            }
        }

        fun padMissingBytes(first:Int, last:Int,sbOut:StringBuilder)
        {
            val charsPerByte = 3
            val maxBytesPerLine = 16
            val bytesWritten: Int = last - first

            val charsMissing = charsPerByte * (maxBytesPerLine - bytesWritten)

            sbOut.append(" ".repeat(charsMissing))
            /*for (i in 0  until charMissing) {
               sbOut.append(' ')
            }*/
        }

        fun appendDisplayChar(bb: Byte, out: StringBuilder) {
            val b = bb.toInt()
            when(b){
                0x20->{out.append('\u2423')}
                0x09->{out.append('\u2192')}
                0x0a->{out.append('\u00b6')}
                0x0d->{out.append('\u00a4')}
                else->{out.append(if(32<=b && b<=126) b.toChar() else NON_PRINTABLE)}
            }
        }
        fun appendHexChars(b: Byte, out: StringBuilder) {
            out.append(HEX[b.toInt() shr 4 and 0x0F]) // 4 high bits
            out.append(HEX[b.toInt() and 0x0F]) // 4 low bits
        }
        fun appendOffset(offset_:Int,sbOut:StringBuilder)
        {
            val offset = offset_.toUInt()
            appendHexChars((offset and 0xFF000000u shr 24).toByte(), sbOut)
            appendHexChars((offset and 0x00FF0000u shr 16).toByte(), sbOut)
            appendHexChars((offset and 0x0000FF00u shr 8).toByte(), sbOut)
            appendHexChars((offset and 0x000000FFu shr 0).toByte(), sbOut)
        }
    }
}