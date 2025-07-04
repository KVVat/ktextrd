import android.device_security.ext2rd.Constants
import android.device_security.ext2rd.Ext2FileSystem
import android.device_security.ext2rd.ImageType
import android.device_security.ext2rd.action
import android.device_security.ext2rd.dumpblocks
import android.device_security.ext2rd.dumpfs
import android.device_security.ext2rd.exportfile
import android.device_security.ext2rd.exportinode
import android.device_security.ext2rd.hexdumpfile
import android.device_security.ext2rd.hexdumpinode
import android.device_security.ext2rd.listfiles
import android.device_security.ext2rd.checkformat
import android.device_security.ext2rd.logicalpartition.LogicalPartition
import android.device_security.ext2rd.reader.IReadWriter
import android.device_security.ext2rd.reader.RandomAccessReader
import android.device_security.ext2rd.reader.SparseReader
import android.device_security.ext2rd.verboselistfiles
import android.device_security.ext2rd.wildcarddumpfiles
import android.device_security.ext2rd.wildcardextractfiles
import java.io.File
import java.io.RandomAccessFile
import java.nio.file.Path

fun usage(){
  println("Usage: ktextrd [-l] [-v] <fsname> [exports...]");
  println("     -l lists all files");
  println("     -v verbosely lists all files");
  //println("     -B       open as block device");
  println("     -d dump super blocks, and all inodes");
  println("     -o OFS1/OFS2  specify offset to efs/sparse image");
  println("     -b from[-until]  hexdump blocks");
  println("     -S OFS specify super block offset");
  println("     -R inode specify root inode for -l and -d");
  println("     -fd dump a file path/inode");
  println("     -fx extract a file path/inode");
  println("     -wd dump files in image with wild card");
  println("     -wx extract files in image with wild card");
}

fun getintarg(args:Array<String>,i:Int):Long{
  if(i<args.size){
    return args[i].toLong()
  } else {
    return 0;
  }
}
fun getstrarg(args:Array<String>,i:Int):String {
  if(i<args.size){
    return args[i]
  } else {
    return "";
  }
}

fun String.isInt(): Boolean {
  try {
    this.toInt()
  } catch (nfe: NumberFormatException) {
    return false
  }
  return true
}

fun main(args : Array<String>) {

  val actions = mutableListOf<action>()
  val offsets = mutableListOf<ULong>()

  var targetFile = ""
  var sb_offset: ULong = 0x400u
  var rootdir_in: Int = Constants.ROOTDIRINODE
  //arg parsing
  //for(var i: Int in 0 until args.size){
  var i = 0
  while(i<args.size){
    val arg = args[i]
    if(arg.startsWith("-")){

      when(arg[1]) {
        'l' -> actions.add(listfiles())
        'v' -> actions.add(verboselistfiles())
        'd' -> actions.add(dumpfs())
        'o' -> {
          //parameter for OffsetReader which read the specific sparse image
          offsets.add(getintarg(args,++i).toULong())
        }
        //[f]ile the command handles single path
        'f' -> {
          if(arg.length==3){
            //dump inode or path in stdout
            //-fd #10
            //-fd /usr/local/test.txt
            when(arg[2]) {
              'd' -> {
                getstrarg(args, ++i).let {
                  if (it[0] == '#') {
                    val num = it.substring(1).toUInt()
                    println("dump inode $num")
                    actions.add(hexdumpinode(num))
                  } else {
                    println("dump a file $it")
                    actions.add(hexdumpfile(it))
                  }
                }
              }

              'x' -> {
                //extract indode or path// read next arg
                //-fx #10:/path_to_out/, #10:file_to_extract
                //-fx /file/to/extract:/path/to/
                getstrarg(args, ++i).split(":").let {
                  if (it.size > 0 && it.size <= 2) {
                    var outpath: String
                    if (it[0][0] == '#') {
                      val num = it[0].substring(1).toUInt()
                      println("export inode $num")
                      if (it.size == 2) {
                        outpath = it[1]
                      } else {
                        outpath = "inode_${it[0]}"
                      }
                      actions.add(exportinode(num, outpath))
                    } else {
                      println("dump a file ${it[0]}")
                      if (it.size == 2) {
                        outpath = it[1]
                      } else {
                        outpath = Path.of(it[0]).fileName.toString()
                      }
                      actions.add(exportfile(it[0], outpath))
                    }
                  } else {
                    println("invalid parameter format")
                  }
                }
              }
              else -> {
                usage()
                return
              }
            }
          } else {
            println("invalid parameter")
            usage()
            return
          }

        }
        'w' -> {
          //search files with [w]ild card in image then extract them, those that match the pattern
          if(arg.length==2){
            getstrarg(args, ++i).let {
              actions.add(wildcarddumpfiles(it,true))
              //dump files
              //-wd *.txt
            }
          } else {
            when(arg[2]) {
              'd' -> {
                getstrarg(args, ++i).let {
                  actions.add(wildcarddumpfiles(it))
                  //dump files
                  //-wd *.txt
                }
              }

              'x' -> {
                //-wx[f] *.txt:/path_to_out/
                var ignoreDir = false
                if (arg.length == 4 && arg[3] == 'f') {
                  ignoreDir = true
                }
                getstrarg(args,++i).split(":").let{
                  if(it.size==2 && it[1].length>=2) {
                      var extractdir = it[1]
                      if(it[1].length>=2 &&it[1][0]!='/' && (it[1][0]!='.' && it[1][1] !='/')){
                        extractdir = "./${it[1]}"
                      }
                      val f = File(extractdir)
                      if(f.exists() && f.isDirectory) {
                        actions.add(wildcardextractfiles(it[0], extractdir, ignoreDir))
                      } else {
                        println("output directory should be exist/specified")
                      }
                  } else {
                    println("invalid parameter format. you need to specify search pattern and output path.")
                  }
                }
              }
              else -> {
                usage()
                return
              }
            }
          }
        }
        'b' -> {
          //dumpblocks
          getstrarg(args,++i).split("-").let{
            if(it.size==2){
              if(it[0].isInt() && it[1].isInt()){
                val start = it[0].toUInt()
                val end = it[1].toUInt()
                println("start=$start, end=$end")
                if(end>=start) {
                  actions.add(dumpblocks(start, end + 1u))
                } else {
                  println("invalid block range. the latter number must be greater than the former one")
                }
              } else {
                println("number format error. spec:BLOCK[-BLOCK]")
              }
            } else if(it.size == 1) {
              if(it[0].isInt()){
                val start = it[0].toUInt()
                actions.add(dumpblocks(start,start+1u))
              } else {
                println("number format error. spec:BLOCK[-BLOCK]")
              }
            } else {
              println("invalid block range format. spec:BLOCK[-BLOCK]")
            }
          }
        }
        'S' -> {
          //update super-block offset
          getintarg(args,++i).toInt().let{
            if(it>0){
              sb_offset=it.toULong()
            }
          }
        }
        'R' -> {
          //update root inode
          getintarg(args,++i).toInt().let {
            if (it > 0) {
              rootdir_in = it
            }
          }
        }
        else -> {
          usage()
          return
        }
      }

    } else if(targetFile == "") {
      targetFile = arg
    }
    i++
  }
  if(targetFile == "" || !File(targetFile).exists()){
    println("File not found")
    usage()
    return
  } else if(File(targetFile).exists() && args.size==1){
    println("No action specified. Lemme check the file format of image")
    actions.add(checkformat())
    //usage()
    //return
  }
  if (offsets.size>0) {
    //r= std::make_shared<OffsetReader>(r, offsets.front(), r->size()-offsets.front());
    //offsets.pop_front();
  }

  val r = RandomAccessFile(File(targetFile),"rw")
  val ext2 = Ext2FileSystem()

  val rr: IReadWriter
  if(SparseReader.issparse(r)){
    rr = SparseReader(r)
  } else {
    rr = RandomAccessReader(r)
  }

  ext2.sb_offset = sb_offset
  ext2.rootdir_in = rootdir_in.toULong()
  //
  ext2.parse(rr)
  //
  if(ext2.fileSystemType == ImageType.EXT2
    || ext2.fileSystemType == ImageType.EXT3
    || ext2.fileSystemType == ImageType.EXT4){
    actions.forEach {
      it.perform(ext2)
    }
  } else {
    //Logical Partition used for super.img
    //We can dump it or expand it with this tool
    val lpp =LogicalPartition()
    if(lpp.isValid(rr)) {
      lpp.parse(rr)
    } else {
      println("The file format is not supported")
    }
  }
}