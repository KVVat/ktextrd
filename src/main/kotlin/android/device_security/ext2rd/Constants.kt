/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.device_security.ext2rd

class Constants {
    companion object {
        const val ROOTDIRINODE=2
        // see linux/include/uapi/linux/stat.h
        const val EXT4_S_IFIFO = 0x1000
        const val EXT4_S_IFCHR = 0x2000
        const val EXT4_S_IFDIR = 0x4000
        const val EXT4_S_IFBLK = 0x6000
        const val EXT4_S_IFREG = 0x8000
        const val EXT4_S_IFLNK = 0xa000
        const val EXT4_S_IFSOCK = 0xc000

        //simple enumeration in /fs/ext4/ext4.h
        const val EXT4_FT_UNKNOWN = 0x0
        const val EXT4_FT_REG_FILE = 0x1
        const val EXT4_FT_DIR = 0x2
        const val EXT4_FT_CHRDEV = 0x3
        const val EXT4_FT_BLKDEV = 0x4
        const val EXT4_FT_FIFO = 0x5
        const val EXT4_FT_SOCK = 0x6
        const val EXT4_FT_SYMLINK = 0x7
        const val EXT4_FT_MAX = 0x8
        const val EXT4_FT_DIR_CSUM = 0xDE

        // see linux/fs/ext4/ext4.h
        // Additional constants from the provided code snippet
        const val EXT4_SECRM_FL = 0x00000001u /* Secure deletion */
        const val EXT4_UNRM_FL = 0x00000002u /* Undelete */
        const val EXT4_COMPR_FL = 0x00000004u /* Compress file */
        const val EXT4_SYNC_FL = 0x00000008u /* Synchronous updates */
        const val EXT4_IMMUTABLE_FL = 0x00000010u /* Immutable file */
        const val EXT4_APPEND_FL = 0x00000020u /* writes to file may only append */
        const val EXT4_NODUMP_FL = 0x00000040u /* do not dump file */
        const val EXT4_NOATIME_FL = 0x00000080u /* do not update atime */
        const val EXT4_DIRTY_FL = 0x00000100u
        const val EXT4_COMPRBLK_FL = 0x00000200u /* One or more compressed clusters */
        const val EXT4_NOCOMPR_FL = 0x00000400u /* Don't compress */
        const val EXT4_ECOMPR_FL = 0x00000800u /* Compression error */
        const val EXT4_INDEX_FL = 0x00001000u /* hash-indexed directory */
        const val EXT4_IMAGIC_FL = 0x00002000u /* AFS directory */
        const val EXT4_JOURNAL_DATA_FL = 0x00004000u /* file data should be journaled */
        const val EXT4_NOTAIL_FL = 0x00008000u /* file tail should not be merged */
        const val EXT4_DIRSYNC_FL = 0x00010000u /* dirsync behaviour (directories only) */
        const val EXT4_TOPDIR_FL = 0x00020000u /* Top of directory hierarchies*/
        const val EXT4_HUGE_FILE_FL = 0x00040000u /* Set to each huge file */
        const val EXT4_EXTENTS_FL = 0x00080000u /* Inode uses extents */
        const val EXT4_EA_INODE_FL = 0x00200000u /* Inode used for large EA */
        const val EXT4_EOFBLOCKS_FL = 0x00400000u /* Blocks allocated beyond EOF */
        const val EXT4_INLINE_DATA_FL = 0x10000000u /* Inode has inline data. */
        const val EXT4_RESERVED_FL = 0x80000000u /* reserved for ext4 lib */

        const val EXT4_FEATURE_COMPAT_DIR_PREALLOC = 0x0001 /* Directory preallocation */
        const val EXT4_FEATURE_COMPAT_IMAGIC_INODES = 0x0002 /* Inodes with imagic filesystem IDs */
        const val EXT4_FEATURE_COMPAT_HAS_JOURNAL = 0x0004 /* Journaling support enabled */
        const val EXT4_FEATURE_COMPAT_EXT_ATTR = 0x0008 /* Extended attribute support enabled */
        const val EXT4_FEATURE_COMPAT_RESIZE_INODE = 0x0010 /* Inode size can be resized */
        const val EXT4_FEATURE_COMPAT_DIR_INDEX = 0x0020 /* Indexed directories */

        // Read-only compatible feature constants
        const val EXT4_FEATURE_RO_COMPAT_SPARSE_SUPER = 0x0001 /* Sparse superblocks */
        const val EXT4_FEATURE_RO_COMPAT_LARGE_FILE = 0x0002 /* Large files support */
        const val EXT4_FEATURE_RO_COMPAT_BTREE_DIR = 0x0004 /* B-tree directories */
        const val EXT4_FEATURE_RO_COMPAT_HUGE_FILE = 0x0008 /* Huge files support */
        const val EXT4_FEATURE_RO_COMPAT_GDT_CSUM = 0x0010 /* Group descriptor checksums */
        const val EXT4_FEATURE_RO_COMPAT_DIR_NLINK = 0x0020 /* Directories have nlinks counts */
        const val EXT4_FEATURE_RO_COMPAT_EXTRA_ISIZE = 0x0040 /* Extra inode size */
        const val EXT4_FEATURE_RO_COMPAT_QUOTA = 0x0100 /* Quota support */
        const val EXT4_FEATURE_RO_COMPAT_BIGALLOC = 0x0200 /* Bigalloc support */
        /*
         * METADATA_CSUM also enables group descriptor checksums (GDT_CSUM).  When
         * METADATA_CSUM is set, group descriptor checksums use the same algorithm as
         * all other data structures' checksums.  However, the METADATA_CSUM and
         * GDT_CSUM bits are mutually exclusive.
        */
        const val EXT4_FEATURE_RO_COMPAT_METADATA_CSUM = 0x0400 /* Metadata checksums */

        // Incompatible feature constants
        const val EXT4_FEATURE_INCOMPAT_COMPRESSION = 0x0001 /* Compression support */
        const val EXT4_FEATURE_INCOMPAT_FILETYPE = 0x0002 /* Filetype support */
        const val EXT4_FEATURE_INCOMPAT_RECOVER = 0x0004 /* Recover support */
        const val EXT4_FEATURE_INCOMPAT_JOURNAL_DEV = 0x0008 /* Journal device */
        const val EXT4_FEATURE_INCOMPAT_META_BG = 0x0010 /* Metadata bitmaps in group descriptors */
        const val EXT4_FEATURE_INCOMPAT_EXTENTS = 0x0040 /* Extents support */
        const val EXT4_FEATURE_INCOMPAT_64BIT = 0x0080 /* 64-bit support */
        const val EXT4_FEATURE_INCOMPAT_MMP = 0x0100 /* Multiple mount protection */
        const val EXT4_FEATURE_INCOMPAT_FLEX_BG = 0x0200 /* Flexible block groups */
        const val EXT4_FEATURE_INCOMPAT_EA_INODE = 0x0400 /* Extended attributes in inode */
        const val EXT4_FEATURE_INCOMPAT_DIRDATA = 0x1000 /* Inline directory data */
        const val EXT4_FEATURE_INCOMPAT_BG_USE_META_CSUM = 0x2000 /* Block group checksums use metadata_csum */
        const val EXT4_FEATURE_INCOMPAT_LARGEDIR = 0x4000 /* Large directories support */
        const val EXT4_FEATURE_INCOMPAT_INLINE_DATA = 0x8000 /* Inline data support */
    }
}