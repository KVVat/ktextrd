@file:OptIn(ExperimentalUnsignedTypes::class)

package android.device_security.ext2rd.extent
import android.device_security.ext2rd.SuperBlock

abstract class ExtentNode {
    abstract fun parse(buf:UByteArray,first:Int)
    abstract fun dump()
    abstract fun enumblocks(super_: SuperBlock, cb:(ub:ByteArray)->Boolean):Boolean
}