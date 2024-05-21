@file:OptIn(ExperimentalUnsignedTypes::class)

package android.device_security.ext2rd.extent

import android.device_security.ext2rd.SuperBlock
import android.device_security.ext2rd.get16le
import android.device_security.ext2rd.get32le

class ExtentLeaf(buf:UByteArray,first:Int) : ExtentNode() {
    var ee_block: UInt=0u
    var ee_len: UShort=0u
    var ee_start_hi: UShort=0u
    var ee_start_lo: UInt=0u

    init {
        parse(buf,first)
    }
    override fun parse(buf:UByteArray, first:Int)
    {
        var p = first;
        ee_block        = buf.get32le(p); p+=4;
        ee_len          = buf.get16le(p); p+=2;
        ee_start_hi     = buf.get16le(p); p+=2;
        ee_start_lo     = buf.get32le(p); p+=4;
    }

    override fun dump() {
        println(String.format("blk:%08x, l=%d %d", ee_block.toInt(),
            ee_len.toInt(),startblock().toLong()));
    }
    fun startblock():ULong
    {
        return (ee_start_hi.toULong() shl 32) or ee_start_lo.toULong()
    }
    override fun enumblocks(super_: SuperBlock, cb: (ub: ByteArray) -> Boolean):Boolean {
        //println("!!extent leaf enum")
        var blk:ULong= startblock();
        for(i in 0 until ee_len.toInt()){

            val ub = super_.getblock(blk.toUInt()).toUByteArray()
            if(!cb(ub.toByteArray()))
                return false
            blk++;
        }
        return true
    }
}