package android.device_security.ext2rd.reader

import java.io.RandomAccessFile

class RandomAccessReader(fp_: RandomAccessFile): IReadWriter
{
    override val fp = fp_
    override var _off =0L

    override fun seek(pos:Long){
        _off = pos;
        fp.seek(_off)
    }
    override fun read(buf:ByteArray?,size: Long):Long{
        return fp.read(buf).toLong()
    }
}