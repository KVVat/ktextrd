package android.device_security.ext2rd.logicalpartition

import android.device_security.ext2rd.get32le
import android.device_security.ext2rd.get64le
import android.device_security.ext2rd.reader.IReadWriter

@OptIn(ExperimentalUnsignedTypes::class, ExperimentalStdlibApi::class)
data class LpMetadataExtent(
    var numSectors: ULong = 0uL,       // Length of this extent, in 512-byte sectors.
    var targetType: UInt = 0u,         // Target type for device-mapper (LpTargetType constants).
    var targetData: ULong = 0uL,       // Meaning depends on targetType.
    var targetSource: UInt = 0u        // Meaning depends on targetType.
) {
    companion object {
        // Total size: 8 (num_sectors) + 4 (target_type) + 8 (target_data) + 4 (target_source) = 24 bytes
        const val SIZE_BYTES =
            ULong.SIZE_BYTES + UInt.SIZE_BYTES + ULong.SIZE_BYTES + UInt.SIZE_BYTES
        var r: IReadWriter? = null;
        @OptIn(ExperimentalUnsignedTypes::class)
        fun parse(fp: IReadWriter): LpMetadataExtent {
            r = fp
            val size = SIZE_BYTES
            val buf = ByteArray(SIZE_BYTES) //
            fp.read(buf, size.toLong())
            return parse(buf.toUByteArray());
        }
        fun parse(b: UByteArray): LpMetadataExtent {
            var p = 0;
            val extent = LpMetadataExtent()
            extent.numSectors = b.get64le(p); p += 8
            extent.targetType = b.get32le(p); p += 4
            extent.targetData = b.get64le(p); p += 8
            extent.targetSource = b.get32le(p); p += 4

            return extent
        }
    }

    fun dump(){
        println("numSectors->"+this.numSectors)
        println("targetType->"+this.targetType.toHexString(HexFormat.Default))
        println("targetData->"+this.targetData.toHexString(HexFormat.Default))
        println("targetSource->"+this.targetSource.toHexString(HexFormat.Default))
    }
}