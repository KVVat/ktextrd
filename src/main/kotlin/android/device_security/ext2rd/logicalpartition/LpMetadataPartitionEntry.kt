package android.device_security.ext2rd.logicalpartition

import android.device_security.ext2rd.get32le
import android.device_security.ext2rd.reader.IReadWriter
import android.device_security.ext2rd.ubytestostr

@OptIn(ExperimentalUnsignedTypes::class, ExperimentalStdlibApi::class)
data class LpMetadataPartitionEntry(
    var name: String = "",                 // Name of this partition (max 36 ASCII characters).
    var attributes: UInt = 0u,             // Attributes for the partition (LP_PARTITION_ATTR_* flags).
    var firstExtentIndex: UInt = 0u,       // Index of the first extent owned by this partition.
    var numExtents: UInt = 0u,             // Number of extents in the partition.
    var groupIndex: UInt = 0u              // Index of the group this partition belongs to.
){
    companion object {
        const val NAME_MAX_LENGTH = 36
        const val SIZE_BYTES = NAME_MAX_LENGTH + (UInt.SIZE_BYTES * 4)
        const val READONLY = 0x00000001u
        var r: IReadWriter? = null;
        @OptIn(ExperimentalUnsignedTypes::class)
        fun parse(fp: IReadWriter): LpMetadataPartitionEntry {
            r = fp
            val size = SIZE_BYTES
            val buf = ByteArray(SIZE_BYTES) //Actual Size of Geometry
            fp.read(buf, size.toLong())
            return parse(buf.toUByteArray());
        }

        @OptIn(ExperimentalUnsignedTypes::class)
        fun parse(b: UByteArray): LpMetadataPartitionEntry {
            var p = 0;
            val entry = LpMetadataPartitionEntry()

            entry.name = ubytestostr(b, p, p + NAME_MAX_LENGTH);p+=NAME_MAX_LENGTH
            entry.attributes = b.get32le(p); p += 4;    // 0000;
            entry.firstExtentIndex = b.get32le(p); p += 4;    // 0004;
            entry.numExtents = b.get32le(p); p += 4;    // 0008;
            entry.groupIndex = b.get32le(p); p += 4;    // 000c;

            return entry
        }
    }

    fun isReadonly(): Boolean {
        return (attributes and READONLY) != 0u
    }
    fun dump() {

        println("partition entry name->"+this.name)
        println("attributes->"+this.attributes.toHexString(HexFormat.Default))
        println("firstExtentIndex->"+this.firstExtentIndex.toHexString(HexFormat.Default))
        println("numExtents->"+this.numExtents.toHexString(HexFormat.Default))
        println("groupIndex->"+this.groupIndex.toHexString(HexFormat.Default))
    }
}

