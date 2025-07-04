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

@file:OptIn(ExperimentalUnsignedTypes::class)

package android.device_security.ext2rd
import android.device_security.debug.HexDump
import android.device_security.ext2rd.reader.IReadWriter

class SuperBlock {

    var s_inodes_count: UInt = 0u
    var s_blocks_count: UInt = 0u
    var s_r_blocks_count: UInt = 0u
    var s_free_blocks_count: UInt = 0u
    var s_free_inodes_count: UInt = 0u
    var s_first_data_block: UInt = 0u // 0 or 1
    var s_log_block_size: UInt = 0u // blocksize = 1024<<s_log_block_size
    var s_log_frag_size: Int = 0 // fragsize = 1024<<s_log_frag_size
    var s_blocks_per_group: UInt = 0u
    var s_frags_per_group: UInt = 0u
    var s_inodes_per_group: UInt = 0u
    var s_mtime: UInt = 0u
    var s_wtime: UInt = 0u
    var s_mnt_count: UShort = 0u
    var s_max_mnt_count: UShort = 0u
    var s_magic: UShort = 0u            // ef53
    var s_state: UShort = 0u
    var s_errors: UShort = 0u
    var s_minor_rev_level: UShort = 0u
    var s_lastcheck: UInt = 0u
    var s_checkinterval: UInt = 0u
    var s_creator_os: UInt = 0u
    var s_rev_level: UInt = 0u
    var s_def_resuid: UShort = 0u
    var s_def_resgid: UShort = 0u
    var s_first_ino: UInt = 0u
    var s_inode_size: UShort = 0u
    var s_block_group_nr: UShort = 0u
    var s_feature_compat: UInt = 0u
    var s_feature_incompat: UInt = 0u
    var s_feature_ro_compat: UInt = 0u
    var s_uuid: UByteArray = UByteArray(16)
    var s_volume_name: UByteArray = UByteArray(16)
    var s_last_mounted: UByteArray = UByteArray(64)
    var s_algo_bitmap: UInt = 0u

    var r: IReadWriter? = null;
    fun parse(fp: IReadWriter) {
        r = fp
        val buf = ByteArray(0x400) //0xCC=204,0x400=1024
        fp.read(buf,0x400)

        parse(buf.toUByteArray())
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun parse(b: UByteArray) {
        var p= 0;
        s_inodes_count = b.get32le(p); p += 4;    // 0000;
        s_blocks_count = b.get32le(p); p += 4;    // 0004;
        s_r_blocks_count = b.get32le(p); p += 4;    // 0008;
        s_free_blocks_count = b.get32le(p); p += 4;    // 000c;
        s_free_inodes_count = b.get32le(p); p += 4;    // 0010;
        s_first_data_block = b.get32le(p); p += 4;    // 0014;
        s_log_block_size = b.get32le(p); p += 4;    // 0018;
        s_log_frag_size = b.get32le(p).toInt();p += 4;    // 001c;
        s_blocks_per_group = b.get32le(p); p += 4;    // 0020;
        s_frags_per_group = b.get32le(p); p += 4;    // 0024;
        s_inodes_per_group = b.get32le(p); p += 4;    // 0028;
        //s_inodes_per_group = 8176u;
        s_mtime = b.get32le(p); p += 4;    // 002c;
        s_wtime = b.get32le(p); p += 4;    // 0030;
        s_mnt_count = b.get16le(p); p += 2;    // 0034;
        s_max_mnt_count = b.get16le(p); p += 2;    // 0036;
        s_magic = b.get16le(p); p += 2;    // 0038;
        s_state = b.get16le(p); p += 2;    // 003a;
        s_errors = b.get16le(p); p += 2;    // 003c;
        s_minor_rev_level = b.get16le(p); p += 2;    // 003e;
        s_lastcheck = b.get32le(p); p += 4;    // 0040;
        s_checkinterval = b.get32le(p); p += 4;    // 0044;
        s_creator_os = b.get32le(p); p += 4;    // 0048;
        s_rev_level = b.get32le(p); p += 4;    // 004c; 0=good old 1=dynamic_rev
        s_def_resuid = b.get16le(p); p += 2;    // 0050;
        s_def_resgid = b.get16le(p); p += 2;    // 0052;

        // The below is present only if (`HasExtended()` == true).
        /* These fields are for EXT2_DYNAMIC_REV superblocks only. */
        /* Note: the difference between "good old" and dynamic rev is that dynamic */
        /*       revs have variable inode sizes and a few other features. */
        s_first_ino = b.get32le(p); p += 4;    // 0054;
        s_inode_size = b.get16le(p); p += 2;    // 0058;
        s_block_group_nr = b.get16le(p); p += 2;    // 005a;
        s_feature_compat = b.get32le(p); p += 4;    // 005c;
        s_feature_incompat = b.get32le(p); p += 4;    // 0060;
        s_feature_ro_compat = b.get32le(p); p += 4;    // 0064;
        s_uuid = b.copyOfRange(p, p + 16); p += 16
        s_volume_name = b.copyOfRange(p, p + 16);p += 16
        s_last_mounted = b.copyOfRange(p, p + 64);p += 64
        s_algo_bitmap = b.get32le(p); p += 4 //00CB

        //behaviour important flags
        flex_bg_support = (s_feature_incompat and Constants.EXT4_FEATURE_INCOMPAT_FLEX_BG) != 0u
        bit64_support   = (s_feature_incompat and Constants.EXT4_FEATURE_INCOMPAT_64BIT) != 0u

        //println("flex bg!->"+flex_bg_support);
        //println("64bit support!->"+bit64_support);
        /*
         * Performance hints.  Directory preallocation should only
         * happen if the EXT2_COMPAT_PREALLOC flag is on.
         */
        if(bit64_support) {
            /*
             * Performance hints.  Directory preallocation should only
             * happen if the EXT2_COMPAT_PREALLOC flag is on.
             */
            s_prealloc_blocks = b.get8(p);p += 1
            s_prealloc_dir_blocks = b.get8(p);p += 1
            s_reserved_gdt_blocks = b.get16le(p);p += 2
            //EXT4_FEATURE_COMAPT_HAS_JOURNAL
            /*
    　　　　　　* Journaling support valid if EXT3_FEATURE_COMPAT_HAS_JOURNAL set.
    　　　　　*/


            s_jounal_uuid = b.copyOfRange(p, p + 16);p += 16
            s_journal_inum = b.get32le(p);p += 4
            s_journal_dev = b.get32le(p);p += 4
            s_last_orphan = b.get32le(p);p += 4
            s_hash_seed_0 = b.get32le(p);p += 4
            s_hash_seed_1 = b.get32le(p);p += 4
            s_hash_seed_2 = b.get32le(p);p += 4
            s_hash_seed_3 = b.get32le(p);p += 4
            s_def_hash_version = b.get8(p);p += 1
            s_jnl_backup_type = b.get8(p);p += 1
            s_desc_size = b.get16le(p);p += 2
            s_default_mount_opts = b.get32le(p);p += 4
            //
            s_first_meta_bg = b.get32le(p);p +=4
            s_mkfs_time = b.get32le(p);p+=4
            s_jnl_blocks = b.copy32leOfRange(p,p+68)// 68 div 4 = 17
            //0x150
            //println("sum'o p="+p+","+p.toHexString(HexFormat.Default))
            s_blocks_coun_hi= b.get32le(p);p+=4
            s_r_blocks_count_hi= b.get32le(p);p+=4
            s_free_blocks_count_hi= b.get32le(p);p+=4
            s_min_extra_isize =  b.get16le(p);p += 2
            s_want_extra_isize =  b.get16le(p);p += 2
            s_flags= b.get32le(p);p+=4
            s_raid_stride=  b.get16le(p);p += 2
            s_mmp_inter=  b.get16le(p);p += 2
            s_mmp_block= b.get64le(p);p+=8
            s_raid_stripe_width= b.get32le(p);p+=4
            s_log_groups_per_flex=b.get8(p);p += 1
            s_checksum_type=b.get8(p);p += 1
            s_encryption_level = b.get8(p);p += 1
            s_reserved_pad = b.get8(p);p += 1
            s_kbytes_writtten = b.get64le(p);p+=8
            //
            s_snapshot_inum= b.get32le(p);p+=4
            s_snapshot_id= b.get32le(p);p+=4
            s_snapshot_rblocks_count= b.get64le(p);p+=8
            s_snapshot_list= b.get32le(p);p+=4
            //
            s_error_count= b.get32le(p);p+=4
            s_fistr_error_time= b.get32le(p);p+=4
            s_first_error_inum= b.get32le(p);p+=4
            s_first_error_block = b.get32le(p);p+=4
            s_first_error_func = b.copyOfRange(p,p+32);p+=32
            s_first_error_line = b.get32le(p);p+=4
            s_last_error_time= b.get32le(p);p+=4
            s_last_error_ino= b.get32le(p);p+=4
            s_last_error_line= b.get32le(p);p+=4
            s_last_error_block = b.get64le(p);p+=8
            s_last_error_func = b.copyOfRange(p,p+32);p+=32
            //
            s_mount_ops = b.copyOfRange(p,p+64);p+=64
            s_usr_quota_inum= b.get32le(p);p+=4
            s_grp_quota_inum= b.get32le(p);p+=4
            s_overhead_clusters= b.get32le(p);p+=4
            s_backup_bgs = b.copy32leOfRange(p,p+8);p+=8
            s_encrypt_algos = b.copyOfRange(p,p+4);p+=4
            s_encrypt_pw_salt = b.copyOfRange(p,p+16);p+=16
            s_lpf_ino= b.get32le(p);p+=4
            s_prj_quota_inum= b.get32le(p);p+=4
            s_checksum_seed = b.get32le(p);p+=4
            s_wtime_hi = b.get8(p);p += 1
            s_mtime_hi = b.get8(p);p += 1
            s_mkfs_time_hi = b.get8(p);p += 1
            s_lastcheck_hi = b.get8(p);p += 1
            s_first_error_time_hi = b.get8(p);p += 1
            s_last_error_time_hi = b.get8(p);p += 1
            s_pad = b.copyOfRange(p,p+2);p+=2
            /*s_reserved = */p+=384
            s_check_sum = b.get32le(p);p+=4
            //println("sum'o p="+p+","+p.toHexString(HexFormat.Default))
        }
    }

    fun blockCount():ULong {
        if(bit64_support){
            return (s_blocks_coun_hi.toULong() shl 32) or s_blocks_count.toULong()
            //return (ULong(s_blocks_count_hi) shl 32) or s_blocks_count
        } else {
            return s_blocks_count.toULong()
        }
    }

    var flex_bg_support:Boolean =false
    var bit64_support:Boolean =false

    var s_prealloc_blocks:UByte = 0u
    var s_prealloc_dir_blocks:UByte = 0u
    var s_reserved_gdt_blocks :UShort = 0u;
    //EXT4_FEATURE_COMAPT_HAS_JOURNAL
    var s_jounal_uuid = UByteArray(16)
    var s_journal_inum:UInt = 0u;
    var s_journal_dev:UInt = 0u;
    var s_last_orphan:UInt = 0u;
    var s_hash_seed_0:UInt = 0u;
    var s_hash_seed_1:UInt = 0u;
    var s_hash_seed_2:UInt = 0u;
    var s_hash_seed_3:UInt = 0u;
    var s_def_hash_version:UByte = 0u;
    var s_jnl_backup_type:UByte = 0u;
    var s_desc_size :UShort = 0u;
    var s_default_mount_opts:UInt = 0u;
    //
    var s_first_meta_bg:UInt = 0u;
    var s_mkfs_time:UInt = 0u;
    var s_jnl_blocks = UIntArray(17)
    //0x150
    var s_blocks_coun_hi:UInt = 0u;
    var s_r_blocks_count_hi:UInt = 0u;
    var s_free_blocks_count_hi:UInt = 0u;
    var s_min_extra_isize:UShort = 0u;
    var s_want_extra_isize:UShort = 0u;
    var s_flags:UInt = 0u;
    var s_raid_stride:UShort = 0u;
    var s_mmp_inter:UShort = 0u;
    var s_mmp_block:ULong = 0u;
    var s_raid_stripe_width:UInt = 0u;
    var s_log_groups_per_flex:UByte=0u;
    var s_checksum_type:UByte=0u;
    var s_encryption_level:UByte = 0u;
    var s_reserved_pad:UByte = 0u;
    var s_kbytes_writtten:ULong = 0u;
    //
    var s_snapshot_inum:UInt = 0u;
    var s_snapshot_id:UInt = 0u;
    var s_snapshot_rblocks_count:ULong = 0u
    var s_snapshot_list:UInt = 0u;
    //
    var s_error_count:UInt = 0u;
    var s_fistr_error_time:UInt = 0u;
    var s_first_error_inum:UInt = 0u;
    var s_first_error_block:UInt = 0u;
    var s_first_error_func = UByteArray(32);
    var s_first_error_line:UInt = 0u;
    var s_last_error_time:UInt = 0u;
    var s_last_error_ino:UInt = 0u;
    var s_last_error_line:UInt = 0u;
    var s_last_error_block:ULong = 0u
    var s_last_error_func = UByteArray(32)
    //
    var s_mount_ops = UByteArray(64)
    var s_usr_quota_inum:UInt = 0u;
    var s_grp_quota_inum:UInt = 0u;
    var s_overhead_clusters:UInt = 0u;
    var s_backup_bgs = UIntArray(2)
    var s_encrypt_algos = UByteArray(4)
    var s_encrypt_pw_salt = UByteArray(16)
    var s_lpf_ino:UInt = 0u;
    var s_prj_quota_inum:UInt = 0u;
    var s_checksum_seed:UInt = 0u;
    var s_wtime_hi:UByte = 0u
    var s_mtime_hi:UByte = 0u
    var s_mkfs_time_hi:UByte = 0u
    var s_lastcheck_hi:UByte = 0u
    var s_first_error_time_hi:UByte = 0u
    var s_last_error_time_hi:UByte = 0u
    var s_pad = UByteArray(2)
    var s_reserved = UIntArray(96)
    var s_check_sum:UInt = 0u;
    
    fun blocksize(): ULong {
        return (1024u shl this.s_log_block_size.toInt()).toULong()
    }

    fun fragsize(): ULong {
        if (this.s_log_frag_size < 0) {
            return (1024u shr (-this.s_log_block_size.toInt())).toULong()
        } else {
            return (1024u shl this.s_log_frag_size).toULong()
        }
    }

    fun bytespergroup(): ULong {
        return s_blocks_per_group * blocksize()
    }

    fun ngroups(): UInt {
        return s_inodes_count / s_inodes_per_group
    }

    fun dump() {
        print(String.format("s_inodes_count=0x%08x\n", s_inodes_count.toInt()));
        print(String.format("s_blocks_count=0x%08x\n", s_blocks_count.toInt()))
        print(String.format("s_r_blocks_count=0x%08x\n", s_r_blocks_count.toInt()))
        print(String.format("s_free_blocks_count=0x%08x\n", s_free_blocks_count.toInt()))
        print(String.format("s_free_inodes_count=0x%08x\n", s_free_inodes_count.toInt()))
        print(String.format("s_first_data_block=0x%08x\n", s_first_data_block.toInt()))
        print(String.format("s_log_block_size=%d\n", s_log_block_size.toInt()))
        print(String.format("s_log_frag_size=%d\n", s_log_frag_size))
        print(String.format("s_blocks_per_group=0x%08x\n", s_blocks_per_group.toInt()))
        print(String.format("s_frags_per_group=0x%08x\n", s_frags_per_group.toInt()))
        print(String.format("s_inodes_per_group=0x%08x (%d)\n", s_inodes_per_group.toInt(),s_inodes_per_group.toInt()))
        print(String.format("s_mtime=0x%08x : %s\n", s_mtime.toInt(), timestr(s_mtime)))
        print(String.format("s_wtime=0x%08x : %s\n", s_wtime.toInt(), timestr(s_wtime)))
        print(String.format("s_mnt_count=0x%04x\n", s_mnt_count.toInt()))
        print(String.format("s_max_mnt_count=0x%04x\n", s_max_mnt_count.toInt()))
        print(String.format("s_magic=0x%04x\n", s_magic.toInt()))
        print(String.format("s_state=0x%04x\n", s_state.toInt()))
        print(String.format("s_errors=0x%04x\n", s_errors.toInt()))
        print(String.format("s_minor_rev_level=0x%04x\n", s_minor_rev_level.toInt()))
        print(String.format("s_lastcheck=0x%08x : %s\n", s_lastcheck.toInt(), timestr(s_lastcheck)))
        print(String.format("s_checkinterval=0x%08x\n", s_checkinterval.toInt()))
        print(String.format("s_creator_os=0x%08x\n", s_creator_os.toInt()))
        print(String.format("s_rev_level=0x%08x\n", s_rev_level.toInt()))
        print(String.format("s_def_resuid=0x%04x\n", s_def_resuid.toInt()))
        print(String.format("s_def_resgid=0x%04x\n", s_def_resgid.toInt()))

        print(String.format(
            "i->%d, b->%d groups\n",
            (s_inodes_count / s_inodes_per_group).toInt(),
            ((s_blocks_count + s_blocks_per_group - 1u) / s_blocks_per_group).toInt()
        ))
        print(String.format("s_first_ino=%d\n", s_first_ino.toInt()))
        print(String.format("s_inode_size=%d\n", s_inode_size.toInt()))
        print(String.format("s_block_group_nr=%d\n", s_block_group_nr.toInt()))
        print(String.format(
            "s_feature_compat=0x%08x: %s\n",
            s_feature_compat.toInt(),
            f_compat2str(s_feature_compat)
        ))
        print(String.format(
            "s_feature_incompat=0x%08x: %s\n",
            s_feature_incompat.toInt(),
            f_incompat2str(s_feature_incompat)
        ))
        print(String.format(
            "s_feature_ro_compat=0x%08x: %s\n",
            s_feature_ro_compat.toInt(),
            f_rocompat2str(s_feature_ro_compat)
        ))
        print(String.format("s_uuid=%s\n", s_uuid.toHex()))
        print(String.format("s_volume_name=%s\n", ubytestostr(s_volume_name)))
        print(String.format("s_last_mounted=%s\n", ubytestostr(s_last_mounted)))
        print(String.format("s_algo_bitmap=0x%08x\n", s_algo_bitmap.toInt()))
    }

    fun getblock(n:UInt):ByteArray
    {
        if (n>=s_blocks_count)
            throw RuntimeException("blocknr too large")

        val blocksize_:Long = blocksize().toLong();
        val buf = ByteArray(blocksize_.toInt())

        r?.seek(blocksize_*n.toLong())
        r?.read(buf,blocksize().toLong())

        return buf;
    }

    companion object {
        fun f_compat2str(f: UInt): String {
            val l = mutableListOf<String>()
            //val f = ff.toInt()
            val all:UInt = Constants.EXT4_FEATURE_COMPAT_DIR_PREALLOC or
                    Constants.EXT4_FEATURE_COMPAT_IMAGIC_INODES or
                    Constants.EXT4_FEATURE_COMPAT_HAS_JOURNAL or
                    Constants.EXT4_FEATURE_COMPAT_EXT_ATTR or
                    Constants.EXT4_FEATURE_COMPAT_RESIZE_INODE or
                    Constants.EXT4_FEATURE_COMPAT_DIR_INDEX

            if (f and Constants.EXT4_FEATURE_COMPAT_DIR_PREALLOC != 0u) l.add("DIR_PREALLOC")
            if (f and Constants.EXT4_FEATURE_COMPAT_IMAGIC_INODES != 0u) l.add("IMAGIC_INODES")
            if (f and Constants.EXT4_FEATURE_COMPAT_HAS_JOURNAL != 0u) l.add("HAS_JOURNAL")
            if (f and Constants.EXT4_FEATURE_COMPAT_EXT_ATTR != 0u) l.add("EXT_ATTR")
            if (f and Constants.EXT4_FEATURE_COMPAT_RESIZE_INODE != 0u) l.add("RESIZE_INODE")
            if (f and Constants.EXT4_FEATURE_COMPAT_DIR_INDEX != 0u) l.add("DIR_INDEX")
            if ((f and all.inv()) != 0u) l.add(String.format("unk_%x", f and all.inv()))

            return l.joinToString (",")
        }

        fun f_incompat2str(f: UInt): String {
            val l = mutableListOf<String>()
            //val f = ff;//.toInt()
            val all:UInt = Constants.EXT4_FEATURE_INCOMPAT_COMPRESSION or
                    Constants.EXT4_FEATURE_INCOMPAT_FILETYPE or
                    Constants.EXT4_FEATURE_INCOMPAT_RECOVER or
                    Constants.EXT4_FEATURE_INCOMPAT_JOURNAL_DEV or
                    Constants.EXT4_FEATURE_INCOMPAT_META_BG or
                    Constants.EXT4_FEATURE_INCOMPAT_EXTENTS or
                    Constants.EXT4_FEATURE_INCOMPAT_64BIT or
                    Constants.EXT4_FEATURE_INCOMPAT_MMP or
                    Constants.EXT4_FEATURE_INCOMPAT_FLEX_BG or
                    Constants.EXT4_FEATURE_INCOMPAT_EA_INODE or
                    Constants.EXT4_FEATURE_INCOMPAT_DIRDATA or
                    Constants.EXT4_FEATURE_INCOMPAT_BG_USE_META_CSUM or
                    Constants.EXT4_FEATURE_INCOMPAT_LARGEDIR or
                    Constants.EXT4_FEATURE_INCOMPAT_INLINE_DATA
            
            if (f and Constants.EXT4_FEATURE_INCOMPAT_COMPRESSION != 0u) l.add("COMPRESSION")
            if (f and Constants.EXT4_FEATURE_INCOMPAT_FILETYPE != 0u) l.add("FILETYPE")
            if (f and Constants.EXT4_FEATURE_INCOMPAT_RECOVER != 0u) l.add("RECOVER")
            if (f and Constants.EXT4_FEATURE_INCOMPAT_JOURNAL_DEV != 0u) l.add("JOURNAL_DEV")
            if (f and Constants.EXT4_FEATURE_INCOMPAT_META_BG != 0u) l.add("META_BG")
            if (f and Constants.EXT4_FEATURE_INCOMPAT_EXTENTS != 0u) l.add("EXTENTS")
            if (f and Constants.EXT4_FEATURE_INCOMPAT_64BIT != 0u) l.add("64BIT")
            if (f and Constants.EXT4_FEATURE_INCOMPAT_MMP != 0u) l.add("MMP")
            if (f and Constants.EXT4_FEATURE_INCOMPAT_FLEX_BG != 0u) l.add("FLEX_BG")
            if (f and Constants.EXT4_FEATURE_INCOMPAT_EA_INODE != 0u) l.add("EA_INODE")
            if (f and Constants.EXT4_FEATURE_INCOMPAT_DIRDATA != 0u) l.add("DIRDATA")
            if (f and Constants.EXT4_FEATURE_INCOMPAT_BG_USE_META_CSUM != 0u) l.add("BG_USE_META_CSUM")
            if (f and Constants.EXT4_FEATURE_INCOMPAT_LARGEDIR != 0u) l.add("LARGEDIR")
            if (f and Constants.EXT4_FEATURE_INCOMPAT_INLINE_DATA != 0u) l.add("INLINE_DATA")
            if ((f and all.inv()) != 0u) l.add(String.format("unk_%x", f and all.inv()))

            return l.joinToString (",")
        }

        fun f_rocompat2str(f: UInt): String {
            val l = mutableListOf<String>()
            //val f = ff.toInt()
            val all:UInt = Constants.EXT4_FEATURE_RO_COMPAT_SPARSE_SUPER or
                    Constants.EXT4_FEATURE_RO_COMPAT_LARGE_FILE or
                    Constants.EXT4_FEATURE_RO_COMPAT_BTREE_DIR or
                    Constants.EXT4_FEATURE_RO_COMPAT_HUGE_FILE or
                    Constants.EXT4_FEATURE_RO_COMPAT_GDT_CSUM or
                    Constants.EXT4_FEATURE_RO_COMPAT_DIR_NLINK or
                    Constants.EXT4_FEATURE_RO_COMPAT_EXTRA_ISIZE or
                    Constants.EXT4_FEATURE_RO_COMPAT_QUOTA or
                    Constants.EXT4_FEATURE_RO_COMPAT_BIGALLOC or
                    Constants.EXT4_FEATURE_RO_COMPAT_METADATA_CSUM

            if (f and Constants.EXT4_FEATURE_RO_COMPAT_SPARSE_SUPER != 0u) l.add("SPARSE_SUPER")
            if (f and Constants.EXT4_FEATURE_RO_COMPAT_LARGE_FILE != 0u) l.add("LARGE_FILE")
            if (f and Constants.EXT4_FEATURE_RO_COMPAT_BTREE_DIR != 0u) l.add("BTREE_DIR")
            if (f and Constants.EXT4_FEATURE_RO_COMPAT_HUGE_FILE != 0u) l.add("HUGE_FILE")
            if (f and Constants.EXT4_FEATURE_RO_COMPAT_GDT_CSUM != 0u) l.add("GDT_CSUM")
            if (f and Constants.EXT4_FEATURE_RO_COMPAT_DIR_NLINK != 0u) l.add("DIR_NLINK")
            if (f and Constants.EXT4_FEATURE_RO_COMPAT_EXTRA_ISIZE != 0u) l.add("EXTRA_ISIZE")
            if (f and Constants.EXT4_FEATURE_RO_COMPAT_QUOTA != 0u) l.add("QUOTA")
            if (f and Constants.EXT4_FEATURE_RO_COMPAT_BIGALLOC != 0u) l.add("BIGALLOC")
            if (f and Constants.EXT4_FEATURE_RO_COMPAT_METADATA_CSUM != 0u) l.add("METADATA_CSUM")
            if ((f and all.inv()) != 0u) l.add(String.format("unk_%x", f and all.inv()))

            return  l.joinToString (",")
        }
    }
}