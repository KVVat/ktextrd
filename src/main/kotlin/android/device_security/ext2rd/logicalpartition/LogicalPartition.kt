package android.device_security.ext2rd.logicalpartition

import android.device_security.ext2rd.get64le
import android.device_security.ext2rd.memcpy
import android.device_security.ext2rd.reader.IReadWriter
import android.device_security.ext2rd.toHex
import java.nio.ByteBuffer

const val LP_PARTITION_RESERVED_BYTES = 4096L
//https://github.com/munjeni/super_image_dumper/blob/master/superunpack.c
//https://github.com/munjeni/super_image_dumper/blob/master/include/metadata_format.h
@OptIn(ExperimentalUnsignedTypes::class)
class LogicalPartition {
    var lpMetaDataGeometry: LpMetadataGeometry = LpMetadataGeometry()
    var lpMetaDataHeader: LpMetadataHeader = LpMetadataHeader()
    enum class Mode {
        TEST,
        DUMP,
        EXPAND
    }
    enum class PartitionFormat {
        ELF,
        EXT4,
        VFAT,
        IMG,
        BIN
    }
    fun isValid(fp: IReadWriter) : Boolean {
        fp.seek(0)
        fp.seek(LP_PARTITION_RESERVED_BYTES)
        //geometry
        lpMetaDataGeometry = LpMetadataGeometry.parse(fp)
        return lpMetaDataGeometry.isValid()
    }
    val TEMP_SIZE = 0x500
    val temp:ByteArray = ByteArray(TEMP_SIZE)

    fun ByteArray.memcmp(buf:ByteArray):Boolean{
        if(this.size < buf.size) return false
        for(i in buf.indices){
            if(this[i] != buf[i]) return false
        }
        return true
    }
    fun UByteArray.memcmp(buf:UByteArray):Boolean{
        if(this.size < buf.size) return false
        for(i in buf.indices){
            if(this[i] != buf[i]) return false
        }
        return true
    }
    fun testPartitionEntry(buf:ByteArray,name:String):Pair<String,PartitionFormat>{
        var fileName = name
        var format = PartitionFormat.BIN
        val checkBytes = buf.memcpy(0,4)
        val checkBytes4Ext4 =buf.memcpy(0x438,2)
        //ByteBuffer.wrap(buf,0,4)
        //val test = ByteArray(checkBytes_.remaining())

        if(checkBytes.memcmp(byteArrayOf(0x7f,0x45,0x4c,0x46))){
            return Pair(name+".ELF",PartitionFormat.ELF);
        } else if(checkBytes4Ext4.memcmp(byteArrayOf(0x53, 0xEF.toByte()) ) ){
            return Pair(name+".EXT4",PartitionFormat.EXT4);
        }
        println(checkBytes.toHex());
        println(checkBytes4Ext4.toHex());
        /*
        else if (memcmp(temp, "\xeb\x3c\x90", 3) == 0)
        {
            printf("      Filetype VFAT.\n");
            snprintf(outname, 64, "%s.vfat", partition.name);
        }
        else if (memcmp(temp, "\x41\x4e\x44\x52", 4) == 0)
        {
            printf("      Filetype IMG.\n");
            snprintf(outname, 64, "%s.img", partition.name);
        }
        else
        {
            printf("      Filetype BIN.\n");
            snprintf(outname, 64, "%s.bin", partition.name);
        }*/

        return Pair(fileName,format)
    }
    fun parse(fp: IReadWriter,mode:Mode=Mode.TEST) {
        fp.seek(0)
        fp.seek(LP_PARTITION_RESERVED_BYTES)
        //geometry
        lpMetaDataGeometry = LpMetadataGeometry.parse(fp)
        if(mode==Mode.DUMP) lpMetaDataGeometry.dump()
        //headers&TableDescriptors
        fp.seek(LP_PARTITION_RESERVED_BYTES*3)
        lpMetaDataHeader = LpMetadataHeader.parse(fp)
        if(mode==Mode.DUMP) lpMetaDataHeader.dump()

        // You would add the converted println here, using lpMetaDataHeader:
        val usedPartitions = lpMetaDataHeader.extents.numEntries
        val totalPartitions = lpMetaDataHeader.partitions.numEntries
        val notUsedPartitions = if (totalPartitions >= usedPartitions) {
            totalPartitions - usedPartitions
        } else {
            System.err.println("Warning: Inconsistent partition counts (extents > partitions).")
            0u
        }

        if(mode==Mode.DUMP) println("\nPartitions = $usedPartitions used, $notUsedPartitions not used, total $totalPartitions\n")

        val numPartitionEntries = lpMetaDataHeader.partitions.numEntries.toInt()
        if (numPartitionEntries > 0) {
            repeat(numPartitionEntries) { i ->

                // 'index' will go from 0 up to (numPartitionEntries - 1)
                if(mode == Mode.DUMP) println("Processing partition entry index: $i")
                val pos = (LP_PARTITION_RESERVED_BYTES*3L) +
                        lpMetaDataHeader.headerSize.toInt()+lpMetaDataHeader.partitions.offset.toInt()+(i*lpMetaDataHeader.partitions.entrySize.toInt())

                fp.seek(pos)
                val partitionEntry = LpMetadataPartitionEntry.parse(fp)
                if(mode==Mode.DUMP) partitionEntry.dump()
                val pos2 = (LP_PARTITION_RESERVED_BYTES*3L) + lpMetaDataHeader.headerSize.toInt()+lpMetaDataHeader.groups.offset.toInt() +
                        (partitionEntry.groupIndex.toInt()*lpMetaDataHeader.groups.entrySize.toInt())
                fp.seek(pos2)
                val groupEntry = LpMetadataPartitionGroup.parse(fp)
                if(mode==Mode.DUMP) groupEntry.dump()

                if(partitionEntry.numExtents>0u){
                    //Read LpMetadataExtent
                    val pos3 = (LP_PARTITION_RESERVED_BYTES*3L) + lpMetaDataHeader.headerSize.toInt()+
                            lpMetaDataHeader.extents.offset.toInt()+(partitionEntry.firstExtentIndex.toInt()*lpMetaDataHeader.extents.entrySize.toInt())
                    fp.seek(pos3)
                    val extent = LpMetadataExtent.parse(fp)
                    if(mode==Mode.DUMP) extent.dump()
                    fp.seek(extent.targetData.toInt()*512L)
                    fp.read(temp,TEMP_SIZE.toLong())
                    val nameAndType = testPartitionEntry(temp,partitionEntry.name)
                    //if(mode == Mode.DUMP)
                    println("Existing Partition:"+nameAndType.first)
                    var ext4FileSize = 0UL

                    if(nameAndType.second == PartitionFormat.EXT4){
                        val uLongBuf:ByteArray = temp.memcpy(0x404,ULong.SIZE_BYTES)
                        ext4FileSize = uLongBuf.toUByteArray().get64le(0) * 4096u
                        if(mode == Mode.DUMP) println("ext4FileSize="+ext4FileSize)
                    }

                    //output file here

                }

                // Your loop body
            }
        }
    }
}