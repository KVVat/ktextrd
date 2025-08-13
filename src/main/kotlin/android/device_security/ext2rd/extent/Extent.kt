@file:OptIn(ExperimentalUnsignedTypes::class)
package android.device_security.ext2rd.extent

import android.device_security.debug.HexDump
import android.device_security.ext2rd.Inode
import android.device_security.ext2rd.SuperBlock
import android.device_security.ext2rd.toHex

class Extent(private val inode: Inode) {
    val eh: ExtentHeader = ExtentHeader()
    val extents: MutableList<ExtentNode> = arrayListOf()
    fun parse(buf:UByteArray,first: Int){
        var p:Int= first
        HexDump.hexdump(buf.toByteArray())
        eh.parse(buf,p);p+=12;
        if(eh.eh_magic.toUInt()!= 0xf30au){
            println(String.format("invalid ehmagic=%04x - %s", eh.eh_magic.toInt(),
                buf.copyOfRange(0,12).toHex()))
            return
        }
        for (i in 0 until eh.eh_entries.toInt()) {
            if (eh.eh_depth.toInt() == 0){
                extents.add(ExtentLeaf(buf,p,inode))
            } else {
                extents.add(ExtentInternal(buf,p,inode))
            }
            p += 12
        }
        if (eh.eh_depth.toInt() == 0 && extents.isNotEmpty()) {
            extents.sortBy { (it as ExtentLeaf).ee_block }
        }
    }
    fun enumblocks(super_: SuperBlock, cb:(ub:ByteArray)->Boolean):Int{

        var logicalBlock = 0U
        for(i in 0 until eh.eh_entries.toInt()){
            val extentLeaf = extents.getOrNull(i) as? ExtentLeaf
            if(extentLeaf !== null) {

                val logicalBlockNew = extentLeaf.ee_block
                var blkProcessed=0
                if (logicalBlock == logicalBlockNew) {
                    blkProcessed = extents[i].enumblocks(super_, cb)
                    if (blkProcessed <= 0) {
                        return -1
                    }
                } else if(logicalBlock<logicalBlockNew){
                    val until = logicalBlockNew-logicalBlock
                    for(i in 0 until until.toInt()){
                        cb(super_.blankblock())
                        logicalBlock += 1U
                    }
                    val blkProcessed_ = extents[i].enumblocks(super_, cb)
                    if (blkProcessed_<=0) {
                        return -1
                    }
                    blkProcessed += blkProcessed_
                }
                logicalBlock += blkProcessed.toUInt()
            } else {
                val extentInternal = extents.getOrNull(i) as? ExtentInternal
                if(extentInternal !== null) {
                    //Basically ExtentInternal used for keep more Leafs
                    if (extents[i].enumblocks(super_, cb)<=0) {
                        return -1
                    }
                }
            }
        }
        return 1
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