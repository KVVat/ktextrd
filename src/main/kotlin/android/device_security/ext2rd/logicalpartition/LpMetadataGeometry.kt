package android.device_security.ext2rd.logicalpartition

import android.device_security.ext2rd.get32le
import android.device_security.ext2rd.reader.IReadWriter
import android.device_security.ext2rd.toHex

//const val LP_METADATA_GEOMETRY_SIZE =
data class LpMetadataGeometry @OptIn(ExperimentalUnsignedTypes::class) constructor(
    var magic: UInt = 0u,
    var structSize: UInt = 0u,
    var checksum: UByteArray = UByteArray(CHECKSUM_SIZE), // sha256
    var metadataMaxSize: UInt = 0u,
    var metadataSlotCount: UInt = 0u,
    var logicalBlockSize: UInt = 0u
    //32*5 = 160 bytes
    //8*32 = 256 bytes
    //256
) {
    companion object {
        const val LP_METADATA_GEOMETRY_SIZE = 4096
        const val CHECKSUM_SIZE = 32
        const val LP_METADATA_GEOMETRY_MAGIC = 0x616c4467u

        var r: IReadWriter? = null;
        @OptIn(ExperimentalUnsignedTypes::class)
        fun parse(fp: IReadWriter):LpMetadataGeometry {

            r = fp
            val buf = ByteArray(LP_METADATA_GEOMETRY_SIZE) //Actual Size of Geometry
            fp.read(buf, LP_METADATA_GEOMETRY_SIZE.toLong())
            return parse(buf.toUByteArray());

        }
        @OptIn(ExperimentalUnsignedTypes::class)
        fun parse(b: UByteArray):LpMetadataGeometry{
            var p= 0;
            val geo = LpMetadataGeometry()

            geo.magic = b.get32le(p); p += 4;    // 0000;
            geo.structSize = b.get32le(p); p += 4;    // 0004;
            geo.checksum = b.copyOfRange(p, p + 32); p += 32
            geo.metadataMaxSize = b.get32le(p); p += 4;    // 003c;
            geo.logicalBlockSize = b.get32le(p); p += 4;    // 0040;

            return geo;
        }
    }
    fun isValid(): Boolean {
        return this.magic == LP_METADATA_GEOMETRY_MAGIC

    }
    @OptIn(ExperimentalStdlibApi::class)
    fun dump() {
        println("magic->"+this.magic.toHexString(HexFormat.Default))
        println("structSize->"+this.structSize.toHexString(HexFormat.Default))
        println("checksum->"+this.checksum.toHex())
        println("metadataMaxSize->"+this.metadataMaxSize.toHexString(HexFormat.Default))
        println("logicalBlockSize->"+this.logicalBlockSize.toHexString(HexFormat.Default))
    }
}