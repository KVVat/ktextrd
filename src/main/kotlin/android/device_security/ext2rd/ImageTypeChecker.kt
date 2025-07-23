package android.device_security.ext2rd

import android.device_security.ext2rd.logicalpartition.LogicalPartition
import android.device_security.ext2rd.reader.IReadWriter
import android.device_security.ext2rd.reader.SparseReader

class ImageTypeChecker {

    companion object {
        @OptIn(ExperimentalStdlibApi::class)
        fun parse(fp: IReadWriter, sb_offset: ULong, lpp:LogicalPartition): ImageType {
            fp.seek(sb_offset.toLong())
            val sb = SuperBlock()
            sb.parse(fp)
            //println(sb.s_magic.toUInt().toHexString(HexFormat.Default))
            if(sb.s_magic.toUInt() != 0xef53u) {
                if(lpp.isValid(fp)){
                    return ImageType.SUPER
                }
                if(fp is SparseReader){
                    return ImageType.SPARSE
                } else {
                    return ImageType.UNKNOWN
                }
            } else {
                // 1. Determine if it's EXT4 by checking incompatible features first.
                if ((sb.s_feature_incompat and Constants.EXT4_FEATURE_INCOMPAT_EXTENTS) != 0u ||
                    (sb.s_feature_incompat and Constants.EXT4_FEATURE_INCOMPAT_64BIT) != 0u ||
                    (sb.s_feature_incompat and Constants.EXT4_FEATURE_INCOMPAT_FLEX_BG) != 0u ||
                    (sb.s_feature_incompat and Constants.EXT4_FEATURE_INCOMPAT_FILETYPE) != 0u) {
                    return ImageType.EXT4
                }

                // 2. If not clearly EXT4, check for journaling to identify EXT3.
                if ((sb.s_feature_compat and Constants.EXT3_FEATURE_COMPAT_HAS_JOURNAL) != 0u) {
                    return ImageType.EXT3
                }

                // 3. If none of the above, it's considered EXT2.
                return ImageType.EXT2
            }
        }
    }
}