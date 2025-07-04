package android.device_secuirty

import android.device_security.ext2rd.Constants
import android.device_security.ext2rd.Ext2FileSystem
import android.device_security.ext2rd.action
import android.device_security.ext2rd.listfiles
import android.device_security.ext2rd.reader.IReadWriter
import android.device_security.ext2rd.reader.RandomAccessReader
import android.device_security.ext2rd.reader.SparseReader
import main
import java.io.File
import java.io.RandomAccessFile
import java.nio.file.Paths
import org.junit.jupiter.api.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class Ext2FileSystemTest {

    @Test
    fun testMain(){
        main(arrayOf<String>("testfiles/ext2img.img","-f"))
        //main(arrayOf<String>("testfiles/ext2img.img","-v"))
        //main(arrayOf<String>("testfiles/ext2img.img","-d"))
        //main(arrayOf<String>("testfiles/ext2img.img","-fd","/usr/src/linux/fs/msdos/Makefile"))
        //main(arrayOf<String>("testfiles/ext2img.img","-fd","#107"))
        //main(arrayOf<String>("testfiles/ext2img.img","-fx","#107:a_file.txt"))
        //main(arrayOf<String>("testfiles/ext2img.img","-fx","/usr/src/linux/fs/msdos/Makefile"))
        //main(arrayOf<String>("testfiles/ext2img.img","-w","*hello.c"))
        //main(arrayOf<String>("testfiles/ext2img.img","-wd","*/inode.c"))
        //main(arrayOf<String>("testfiles/ext2img.img","-wx","*/inode.c:./dist/"))
        //main(arrayOf<String>("testfiles/ext2img.img","-wxf","*/*.c:dist/"))
        //main(arrayOf<String>("testfiles/ext2img.img","-b","12000-12100"))
    }

    @Test
    fun testRawReader() {
        val uri = Paths.get( "testfiles", "ext2fs2.img").toUri()
        //https://github.com/munjeni/super_image_dumper - super img
        val r = RandomAccessFile(File(uri.path),"rw")

        val sb_offset: ULong = 0x400u
        val rootdir_in: Int = Constants.ROOTDIRINODE
        val ext2 = Ext2FileSystem()

        val rr: IReadWriter
        if(SparseReader.issparse(r)){
            rr = SparseReader(r)
            println("[sparse image]");
        } else {
            rr = RandomAccessReader(r)
        }

        ext2.sb_offset = sb_offset
        ext2.rootdir_in = rootdir_in.toULong()
        //
        ext2.parse(rr)

        arrayOf<action>(
            listfiles(),
            //verboselistfiles(),
            //exportfile("/usr/src/patch-2.5/partime.c","partime.c"),
            //wildcarddumpfiles("*.vdex"),
            //wildcardextractfiles("*.apk","./dist/",false)
            //findfiles("*.txt"),
            //extractfiles("/*/*.apk","out",true) searchstr,outpath,digdirectory
            //hexdumpfile("/directory1/fortune1"),
            //dumpblocks(1u,10u),
            //hexdumpfile("usr/local/lib/gcc-include/va-mips.h"),
            //dumpfs()
        ).forEach{
            it.perform(ext2)
        }
    }
}