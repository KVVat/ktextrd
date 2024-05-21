@file:OptIn(ExperimentalUnsignedTypes::class)
package android.device_security.ext2rd.extent

import android.device_security.debug.HexDump
import android.device_security.ext2rd.SuperBlock
import android.device_security.ext2rd.toHex

class Extent {
    val eh: ExtentHeader = ExtentHeader()
    val extents: MutableList<ExtentNode> = arrayListOf()
    fun parse(buf:UByteArray,first: Int){
        var p:Int= first
        HexDump.hexdump(buf.toByteArray())
        eh.parse(buf,p);p+=12;
        if(eh.eh_magic.toUInt()!= 0xf30au){
            println(String.format("invalid ehmagic=%04x - %s", eh.eh_magic.toInt(),
                buf.copyOfRange(0,12).toHex()))
            //println(HexDump.hexdump(buf.toByteArray()))

            //eh.dump()
            //throw "invalid extent hdr magic";
            return
        }/* else {
            //println(HexDump.hexdump(buf.toByteArray()))
            //eh.dump()
        }*/
        for (i in 0 until eh.eh_entries.toInt()) {
            if (eh.eh_depth.toInt() == 0){
                extents.add(ExtentLeaf(buf,p))
            } else {
                extents.add(ExtentInternal(buf,p))
            }
            p += 12
        }
    }
    fun enumblocks(super_: SuperBlock, cb:(ub:ByteArray)->Boolean):Boolean{

        for(i in 0 until eh.eh_entries.toInt()){
            if(!extents[i].enumblocks(super_,cb)) {
                //println("extent enumblocks $i")
                return false
            }
        }
        return true
    }
    fun dump()
    {
        var i=0;
        eh.dump();
        extents.forEach {e->
            print(String.format("EXT %d: ",i))
            e.dump()
            i++;
        }
    }
}