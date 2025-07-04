package android.device_security.ext2rd.logicalpartition

import android.device_security.ext2rd.get32le
import android.device_security.ext2rd.get64le
import android.device_security.ext2rd.reader.IReadWriter
import android.device_security.ext2rd.ubytestostr

@OptIn(ExperimentalUnsignedTypes::class, ExperimentalStdlibApi::class)
data class LpMetadataPartitionGroup(
    var name: String = "",          // Name of this group (max 36 ASCII characters).
    var flags: UInt = 0u,           // Flags for the group (LP_GROUP_* flags).
    var maximumSize: ULong = 0uL    // Maximum size in bytes. 0 means no maximum size.
) {
    companion object {
        const val NAME_MAX_LENGTH = 36
        // Total size: 36 (name) + 4 (flags) + 8 (maximum_size) = 48 bytes
        const val SIZE_BYTES = NAME_MAX_LENGTH + UInt.SIZE_BYTES + ULong.SIZE_BYTES

        var r: IReadWriter? = null;
        @OptIn(ExperimentalUnsignedTypes::class)
        fun parse(fp: IReadWriter): LpMetadataPartitionGroup{
            r = fp
            val size = SIZE_BYTES
            val buf = ByteArray(SIZE_BYTES) //Actual Size of Geometry
            fp.read(buf, size.toLong())
            return parse(buf.toUByteArray());
        }

        @OptIn(ExperimentalUnsignedTypes::class)
        fun parse(b: UByteArray): LpMetadataPartitionGroup {
            var p = 0;
            val entry = LpMetadataPartitionGroup()

            entry.name = ubytestostr(b, p, p + NAME_MAX_LENGTH);p+= NAME_MAX_LENGTH
            entry.flags = b.get32le(p); p += 4;    // 0000;
            entry.maximumSize = b.get64le(p); p += 8;    // 0004;

            return entry
        }

    }
    fun dump() {
        println("partition group name->"+this.name)
        println("flags->"+this.flags.toHexString(HexFormat.Default))
        println("maximumSize->"+this.maximumSize.toHexString(HexFormat.Default))
    }
}

