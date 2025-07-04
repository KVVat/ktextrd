package android.device_security.ext2rd.logicalpartition

import android.device_security.ext2rd.get16le
import android.device_security.ext2rd.get32le
import android.device_security.ext2rd.reader.IReadWriter
import android.device_security.ext2rd.toHex

data class LpMetadataHeader @OptIn(ExperimentalUnsignedTypes::class) constructor(
    var magic: UInt = 0u,
    var majorVersion: UShort = 0u,
    var minorVersion: UShort = 0u,
    var headerSize: UInt = 0u,      // Size of this LpMetadataHeader struct itself
    var headerChecksum: UByteArray = UByteArray(CHECKSUM_SIZE), // crc32 of this header (with this field zeroed during calculation)
    var tablesSize: UInt = 0u,      // Total size of all metadata tables (partitions, extents, etc.)
    var tablesChecksum: UByteArray = UByteArray(CHECKSUM_SIZE),  // crc32 of all table data

    var partitions: LpMetadataTableDescriptor = LpMetadataTableDescriptor(),
    var extents: LpMetadataTableDescriptor = LpMetadataTableDescriptor(),
    var groups: LpMetadataTableDescriptor = LpMetadataTableDescriptor(),
    var blockDevices: LpMetadataTableDescriptor = LpMetadataTableDescriptor(),

    // Fields for minor_version >= 1 (conditionally parsed)
    var flags: UInt = 0u, // Only valid if header_size indicates their presence
    var reserved: UByteArray = UByteArray(12) // Only valid if header_size indicates their presence
){
    companion object {
        var r: IReadWriter? = null;
        const val CHECKSUM_SIZE = 32
        /** Minimum size for a version 0.0 header in bytes. */
        val BASE_SIZE_BYTES = (UInt.SIZE_BYTES * 3) +  // magic, headerSize, headerChecksum, tablesSize, tablesChecksum (5 UInts, but headerChecksum is one of them)
                (UShort.SIZE_BYTES * 2) + // majorVersion, minorVersion
                (32*2)+
                (LpMetadataTableDescriptor.SIZE_BYTES * 4) // partitions, extents, groups, blockDevices
        /** Size of additional fields introduced in minor_version >= 1, in bytes. */
        val V0_1_ADDITIONAL_SIZE_BYTES = UInt.SIZE_BYTES + 12 // flags + reserved array size

        @OptIn(ExperimentalUnsignedTypes::class)
        fun parse(fp: IReadWriter): LpMetadataHeader {
            r = fp
            val size = BASE_SIZE_BYTES+ V0_1_ADDITIONAL_SIZE_BYTES
            val buf = ByteArray(size) //Actual Size of Geometry
            fp.read(buf, size.toLong())
            return parse(buf.toUByteArray());
        }

        @OptIn(ExperimentalUnsignedTypes::class)
        fun parse(b: UByteArray): LpMetadataHeader {
            var p = 0;
            val header = LpMetadataHeader()
            header.magic = b.get32le(p); p += 4    // 0000;
            header.majorVersion = b.get16le(p); p+=2
            header.minorVersion = b.get16le(p); p+=2
            header.headerSize = b.get32le(p); p+=4
            header.headerChecksum = b.copyOfRange(p, p + 32); p += 32
            header.tablesSize = b.get32le(p); p+=4
            header.tablesChecksum = b.copyOfRange(p, p + 32); p += 32
            header.partitions.offset = b.get32le(p); p+=4
            header.partitions.numEntries = b.get32le(p); p+=4
            header.partitions.entrySize = b.get32le(p); p+=4
            header.extents.offset = b.get32le(p); p+=4
            header.extents.numEntries = b.get32le(p); p+=4
            header.extents.entrySize = b.get32le(p); p+=4
            header.groups.offset=b.get32le(p); p+=4
            header.groups.numEntries=b.get32le(p); p+=4
            header.groups.entrySize=b.get32le(p); p+=4
            header.blockDevices.offset=b.get32le(p); p+=4
            header.blockDevices.numEntries=b.get32le(p); p+=4
            header.blockDevices.entrySize=b.get32le(p); p+=4
            header.flags = b.get32le(p); p += 4

            return header;
        }
    }
    @OptIn(ExperimentalStdlibApi::class, ExperimentalUnsignedTypes::class)
    fun dump() {
        println("magic->"+this.magic.toHexString(HexFormat.Default))
        println("majorVersion->"+this.majorVersion.toHexString(HexFormat.Default))
        println("minorVersion->"+this.minorVersion.toHexString(HexFormat.Default))
        println("headerSize->"+this.headerSize.toHexString(HexFormat.Default))
        println("headerChecksum->"+this.headerChecksum.toHex())
        println("tablesSize->"+this.tablesSize.toHexString(HexFormat.Default))
        println("tablesChecksum->"+this.tablesChecksum.toHex())
        println("partition->")
        this.partitions.dump()
        println("extents->")
        this.extents.dump()
        println("groups->")
        this.groups.dump()
        println("blockDevices->")
        this.blockDevices.dump()
        println("flags(v1.1)->"+this.flags.toHexString(HexFormat.Default))
    }
}