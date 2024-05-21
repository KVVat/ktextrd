package android.device_security.ext2rd.reader

import android.device_security.debug.HexDump
import android.device_security.ext2rd.memfill32le
import android.device_security.ext2rd.read16le
import android.device_security.ext2rd.read32le
import java.io.EOFException
import java.io.RandomAccessFile
import java.util.SortedMap

class SparseReader(fp_: RandomAccessFile) : IReadWriter {

    private class SparseRecord (ndwords_:ULong,isfill_:Boolean,val_:ULong){
        var isfill:Boolean=isfill_
        var ndwords:ULong=ndwords_
        var v: UInt=0u
        var offset: ULong=0u
        init {
            if(isfill){
                v=val_.toUInt()
                //dump
            } else {
                offset=val_
            }
        }
        fun dump(off:ULong){
            if(isfill){
                println(String.format("%x-%x: fill with %08x",off.toLong(),(off+(ndwords* 4u)).toLong(),v.toInt()))
            } else {
                println(String.format("%x-%x: copy from %08x",
                    off.toLong(),(off+(ndwords* 4u)).toLong(),offset.toLong()))
            }
        }
    }

    override val fp = fp_
    override var _off =0L

    var magic: UInt=0u
    var version: UInt=0u
    var hdrsize: UShort=0u
    var cnkhdrsize: UShort=0u
    var blksize: UInt=0u
    var blkcount: UInt=0u
    var chunkcount: UInt=0u
    var checksum: UInt=0u

    private val _map: SortedMap<Long, SparseRecord> = sortedMapOf()

    companion object {
        fun issparse(fp_: RandomAccessFile):Boolean{
            fp_.seek(0);
            val magic = fp_.read32le();
            //println(String.format("%x",magic.toInt()))
            return (fp_.length()>32 && magic==0xed26ff3au)
        }
    }

    fun readheader()
    {
        fp.seek(0)
        magic = fp.read32le();//buf.get32le(p);     p+=4

        if(magic != 0xed26ff3au)
            throw RuntimeException("invalid sparse magic");

        version = fp.read32le()
        hdrsize = fp.read16le()//file_hdr_size=28?
        cnkhdrsize= fp.read16le()//cnkhdrsize=12
        blksize = fp.read32le()//1024*4
        blkcount= fp.read32le()
        chunkcount= fp.read32le()
        checksum= fp.read32le()

        println(String.format("sparse: v%08x h:%04x c:%04x b:%08x nblk:%08x ncnk:%08x,  cksum:%08x"
            ,version.toInt(),hdrsize.toInt(),cnkhdrsize.toInt(),
            blksize.toInt(),blkcount.toInt(),
            chunkcount.toInt(),checksum.toInt()))

        if(cnkhdrsize.toUInt() != 12u){
            println(String.format("unexpected chunkhdr size: %d",cnkhdrsize.toInt()))
        }
    }

    fun scansparse()
    {
        fp.seek(hdrsize.toLong())

        var ofs=0L
        var i=0;
        while(true){
            i++
            try {
                val chunktype:UShort =  fp.read16le()
                /*val unused:UShort =*/ fp.read16le()
                val chunksize:UInt =    fp.read32le()
                val total_sz:UInt =      fp.read32le()
                if(cnkhdrsize.toInt() !=12) {
                    fp.skipBytes(cnkhdrsize.toInt() - 12)
                }
                //println(String.format("sparse = %d ctype = %x chunksize=%d totalsize=%d",i,
                //    chunktype.toInt(),chunksize.toInt(),total_sz.toInt()) );

                when(chunktype.toUInt()){
                    0xcac1u-> {//RAW
                        copydata(fp.filePointer,((total_sz-cnkhdrsize)/4u).toULong(),ofs)
                        ofs += (total_sz-cnkhdrsize).toLong()
                        fp.skipBytes((total_sz - cnkhdrsize).toInt())
                    }
                    0xcac2u-> {//Fill
                        val fill = fp.read32le()
                        //println(String.format("filler = %x %d",fill.toInt()),ofs)
                        filldata(fill,((chunksize*blksize)/4u).toULong(),ofs)
                        ofs += (chunksize*blksize).toLong()
                    }
                    0xcac3u-> {//Don't Care : Skip chunk
                        filldata(0u,((chunksize*blksize)/4u).toULong(),ofs)
                        ofs += (chunksize*blksize).toLong()
                        fp.skipBytes((total_sz - cnkhdrsize).toInt())
                    }
                    0xcac4u-> {//CRC
                        fp.read32le()//crc
                        //fp.skipBytes((chunksize - cnkhdrsize).toInt())
                    } else-> {
                        throw RuntimeException("unknown sparse chunk type :"+chunktype)

                    }
                }

            } catch (e: EOFException){
                //e.printStackTrace()
                break;
            }
        }
        println(String.format("end of sparse:%x",ofs))

    }
    fun copydata(sparseofs:Long, ndwords:ULong, expandedofs:Long)
    {
        _map.put(expandedofs, SparseRecord(ndwords,false,sparseofs.toULong()))
    }
    fun filldata(value:UInt, ndwords:ULong, expandedofs:Long)
    {
        _map.put(expandedofs, SparseRecord(ndwords,true,value.toULong()))
    }
    init {
        _off = 0
        readheader()
        scansparse()
        println(String.format("%d entries in sparse map",_map.size))
        if(_map.size>0){
            val i = _map.entries.first();
            print("first:");i.value.dump(i.key.toULong())
        }
        if(_map.size>1){
            val i = _map.entries.last();
            print("last:");i.value.dump(i.key.toULong())
        }
    }

    private fun findofs(ofs:Long):Map.Entry<Long, SparseRecord>{
        //println(">"+_map.size+","+ofs)
        var i = _map.tailMap(ofs).entries.first();
        if(i == _map.entries.first()){
            return _map.entries.last()
        }
        //get before 1//
        i = _map.headMap(ofs+1).entries.last();//lowerEntry(i)
        if((i.key + i.value.ndwords.toLong()*4)<ofs){
            return _map.entries.last()
        }

        return i
    }

    override fun read(buf: ByteArray?,size:Long):Long {
        var total = 0L;
        var n = size
        var p=0;
        while(n>0){
            val i= findofs(_off)
            if(i == _map.entries.last())
                break;

            val v: SparseRecord = i.value;
            val k = i.key
            val want = Math.min(n, (v.ndwords.toInt()*4)-(_off-k)).toInt()
            if(v.isfill){
                if(v.v == 0u) {
                    buf!!.fill(0, p, p+want)
                }else if(v.v == 0xFFFFFFFFu){
                    buf!!.fill(0xFF.toByte(), p, p+want)
                } else {
                    buf!!.memfill32le(v.v,p,p+want)
                }
            } else {
                val ptr = v.offset.toLong()+_off- k;
                fp.seek(ptr)
                fp.read(buf)
                //println(HexDump.hexdump(buf!!))
            }

            p+=want
            n-=want
            total += want
            _off += want
        }

        return total;
    }
    override fun seek(pos:Long)
    {
        _off= pos;
    }
    fun size() {
        (blkcount*blksize).toLong()
    }
}