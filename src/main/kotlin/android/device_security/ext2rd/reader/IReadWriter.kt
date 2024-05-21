package android.device_security.ext2rd.reader

import java.io.RandomAccessFile

interface IReadWriter
{
    val fp: RandomAccessFile
    var _off:Long
    fun seek(pos:Long)
    fun read(buf:ByteArray?,size:Long):Long
}