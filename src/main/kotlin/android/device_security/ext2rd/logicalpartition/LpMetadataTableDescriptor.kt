package android.device_security.ext2rd.logicalpartition

/**
 * Describes a table within the metadata (e.g., partitions, extents).
 */
data class LpMetadataTableDescriptor(
    var offset: UInt = 0u,    // Offset from the end of the LpMetadataHeader to the start of this table.
    var numEntries: UInt = 0u,
    var entrySize: UInt = 0u  // Size of each entry in this table.
) {
    companion object {
        const val SIZE_BYTES = 12 // 3 * UInt.SIZE_BYTES
    }
    @OptIn(ExperimentalStdlibApi::class)
    fun dump() {
        println("Table Descriptor Vailidity:"+this.isValid())
        println("\toffset->"+this.offset.toHexString(HexFormat.Default))
        println("\tnumEntries->"+this.numEntries.toHexString(HexFormat.Default))
        println("\tentrySize->"+this.entrySize.toHexString(HexFormat.Default))
    }
    fun isValid(): Boolean {
        // A basic check: if there are entries, entry size should be non-zero.
        // More specific checks might be needed depending on the table type.
        return if (numEntries > 0u) entrySize > 0u else true
    }
}