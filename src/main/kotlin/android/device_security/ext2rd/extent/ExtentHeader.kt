@file:OptIn(ExperimentalUnsignedTypes::class)

package android.device_security.ext2rd.extent

import android.device_security.ext2rd.get16le
import android.device_security.ext2rd.get32le

class ExtentHeader {
    var eh_magic: UShort = 0u
    var eh_entries: UShort = 0u
    var eh_max: UShort = 0u
    var eh_depth: UShort = 0u
    var eh_generation: UInt = 0u

    fun parse(buf:UByteArray,first: Int){
        var p= first
        eh_magic	= buf.get16le(p); p+=2;
        eh_entries	= buf.get16le(p); p+=2;
        eh_max		= buf.get16le(p); p+=2;
        eh_depth	= buf.get16le(p); p+=2;
        eh_generation= buf.get32le(p);p+=4;
    }
    fun dump()
    {
        println(String.format("EXTHEADER M:%04x, N:%d,max:%d, d:%d, g:%d", eh_magic.toInt(),
            eh_entries.toInt(), eh_max.toInt(), eh_depth.toInt(), eh_generation.toInt()))
    }
}