@file:OptIn(ExperimentalUnsignedTypes::class)
package android.device_security.ext2rd.extent

import android.device_security.ext2rd.SuperBlock
import android.device_security.ext2rd.get16le
import android.device_security.ext2rd.get32le

class ExtentInternal(buf:UByteArray,first:Int) : ExtentNode() {

    var ei_block: UInt=0u
    var ei_leaf_lo: UInt=0u
    var ei_leaf_hi: UShort=0u
    var ei_unused: UShort=0u

    init {
        parse(buf,first)
    }
    override fun parse(buf:UByteArray, first:Int)
    {
        var p = first;
        ei_block        = buf.get32le(p); p+=4;
        ei_leaf_lo      = buf.get32le(p); p+=4;
        ei_leaf_hi      = buf.get16le(p); p+=2;
        ei_unused       = buf.get16le(p); p+=2;
    }

    override fun dump() {
        println(String.format("blk:%08x, [%d] %d", ei_block.toInt(),
            ei_unused.toInt(), leafnode().toLong()));
    }

    fun leafnode():ULong
    {
        return (ei_leaf_hi.toULong() shl 32) or ei_leaf_lo.toULong()
    }

    override fun enumblocks(super_: SuperBlock, cb: (ub: ByteArray) -> Boolean):Boolean {
        val e = Extent();
        val ub = super_.getblock(leafnode().toUInt()).toUByteArray()
        e.parse(ub,0);
        return e.enumblocks(super_, cb);
    }

}