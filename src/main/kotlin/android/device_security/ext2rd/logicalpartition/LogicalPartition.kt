package android.device_security.ext2rd.logicalpartition

import android.device_security.ext2rd.exportinode
import android.device_security.ext2rd.get64le
import android.device_security.ext2rd.memcpy
import android.device_security.ext2rd.prepareOutfile
import android.device_security.ext2rd.reader.IReadWriter
import android.device_security.ext2rd.toHex
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.file.Paths

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

        if(checkBytes.memcmp(byteArrayOf(0x7f,0x45,0x4c,0x46))){
            return Pair(name+".elf",PartitionFormat.ELF);
        } else if(checkBytes4Ext4.memcmp(byteArrayOf(0x53, 0xEF.toByte()) ) ){
            return Pair(name+".ext4",PartitionFormat.EXT4);
        } else  if(checkBytes.memcmp(byteArrayOf(0xeb.toByte(),0x3C,0x90.toByte()))){
            return Pair(name+".vfat",PartitionFormat.VFAT);
        } else  if(checkBytes.memcmp(byteArrayOf(0x41,0x4e,0x44,0x52))){
            return Pair(name+".img",PartitionFormat.IMG);
        }

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

        if(mode==Mode.DUMP) println("Partitions = $usedPartitions used, $notUsedPartitions not used, total $totalPartitions\n")

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
                    println("\nExisting Partition:"+nameAndType.first)
                    var ext4FileSize = 0UL

                    if(nameAndType.second == PartitionFormat.EXT4){
                        val uLongBuf:ByteArray = temp.memcpy(0x404,ULong.SIZE_BYTES)
                        ext4FileSize = uLongBuf.toUByteArray().get64le(0) * 4096u
                        if(mode == Mode.DUMP) println("ext4FileSize="+ext4FileSize)
                    }

                    var save_path = ""
                    //if(save_path.length == 0){
                    val currentRelativePath = Paths.get("")
                    save_path = currentRelativePath.toAbsolutePath().toString()
                    //}
                    val fb:ByteArray = ByteArray(512)
                    if(mode == Mode.EXPAND) {
                        var path: String
                        path = Paths.get(save_path).resolve(nameAndType.first).toAbsolutePath()
                            .toString()

                        //output file here
                        val f = prepareOutfile(path)
                        val fw = FileOutputStream(f)
                        fp.seek(extent.targetData.toInt() * 512L)
                        var p = 0UL
                        var progress = 0
                        while (true) {
                            if (fp.read(fb, 512) <= 0) {
                                break
                            }
                            if (p < extent.numSectors * 512UL) {
                                if (ext4FileSize >= 0UL) {
                                    if (p >= ext4FileSize) break
                                }
                                fw.write(fb)
                                fw.flush()
                            } else {
                                break
                            }
                            p += 512UL
                            if ((p % 8388608UL) == 0UL) {
                                progress += 1
                                print(".")
                                if (progress == 52) {
                                    progress = 0
                                    print("\n")
                                }
                            }
                        }

                        //truncate file via channel
                        if (ext4FileSize >= 0UL) {
                            //println("pos:"+p+","+ext4FileSize+","+extent.numSectors*512UL)
                            if(ext4FileSize > p){

                            }
                            fw.getChannel().truncate(ext4FileSize.toLong());
                        }
                        fw.getChannel().force(true);
                        fw.getChannel().lock();
                        fw.close()
                    }
                    print("\n")
                }
            }
        }
    }
}