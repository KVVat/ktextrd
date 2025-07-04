package android.device_security.ext2rd

/**
 * Represents the determined type of the filesystem.
 */
enum class ImageType {
    EXT2,
    EXT3,
    EXT4,
    SPARSE,
    UNKNOWN
}