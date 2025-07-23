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
import org.apache.commons.io.FilenameUtils
import java.io.File
import java.io.FileOutputStream
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.Paths

fun is_directory_eager(fs: Ext2FileSystem, e: DirectoryEntry):Boolean{
    val ft = e.filetype.toInt()
    if(ft == Constants.EXT4_FT_DIR){
        return true
    } else if(ft == Constants.EXT4_FT_UNKNOWN){
        //for case the directory entry is broken, eval the mode of the inode
        val child = fs.getinode(e.inode)
        if((child.i_mode and 0xf000u).toInt() == Constants.EXT4_S_IFDIR)
            return true
    }
    return false
}

fun findpath(fs: Ext2FileSystem, nr:UInt, search:String,path:String):List<Pair<UInt,String>> {
    val i = fs.getinode(nr)
    val results = mutableListOf<Pair<UInt, String>>()

    if ((i.i_mode and 0xf000u).toInt() != Constants.EXT4_S_IFDIR)
        return listOf()

    i.enumblocks(fs.superblk) { buf ->
        //println("enum"+buf.size)
        if (buf.size <= 0) return@enumblocks true

        var p = 0u

        while (p < fs.superblk.blocksize()) {
            val e = DirectoryEntry()
            val n = e.parse(buf.toUByteArray(), p).toUInt()
            p = p + n
            if (n == 0u) break

            if (e.name == "." || e.name == ".." || e.name.length == 0)
                continue

            val pathOut = Paths.get(path).resolve(e.name).toString()
            //println("pathOut="+path)
            if (is_directory_eager(fs, e)) {
                val r = findpath(fs, e.inode, search, pathOut)
                results.addAll(r)
            } else {
                if (FilenameUtils.wildcardMatch(pathOut, search)) {
                    results.add(Pair<UInt, String>(e.inode, pathOut))
                }
            }
        }
        return@enumblocks true
    }

    //println(results.toString())
    return results
}


fun searchpath(fs: Ext2FileSystem, nr:UInt, path:String):UInt{
    val i = fs.getinode(nr)

    if((i.i_mode and 0xf000u).toInt() != Constants.EXT4_S_IFDIR)
        return 0u
    var found = 0u
    i.enumblocks(fs.superblk) { buf ->
        if(buf.size<=0) return@enumblocks true
        var p = 0u
        while(p<fs.superblk.blocksize()){
            val e = DirectoryEntry()
            val n = e.parse(buf.toUByteArray(),p).toUInt()
            p = p+n
            if(n == 0u) break

            if(e.name == "." || e.name == ".." || e.name.length == 0)
                continue

            if(path.length<e.name.length)
                continue
            //println(">"+path.length+","+e.name.length+","+path+","+e.name)
            if(path.equals(e.name)){
                found = e.inode //found
                return@enumblocks false
            }

            if(path.length>e.name.length && path[e.name.length]==File.separatorChar && path.startsWith(e.name)){
                //dig directory
                if(is_directory_eager(fs,e)){
                    found = searchpath(fs,e.inode,path.substring(e.name.length+1))
                    return@enumblocks false
                } else {
                    //println("not a directory "+path)
                    return@enumblocks false
                }
            }
        }
        return@enumblocks true
    }
    return found
}
fun recursedirs(fs: Ext2FileSystem, nr:UInt, path:String, cb:(de: DirectoryEntry, path:String)->Unit){
    val i = fs.getinode(nr)

    if((i.i_mode and 0xf000u).toInt() != Constants.EXT4_S_IFDIR)
        return

    i.enumblocks(fs.superblk) {buf->
        //println("enum"+buf.size)
        if(buf.size<=0) return@enumblocks true

        var p = 0u

        while(p<fs.superblk.blocksize()){
            val e = DirectoryEntry()
            val n = e.parse(buf.toUByteArray(),p).toUInt()
            p = p+n
            if(n == 0u) break

            if(e.name == "." || e.name == ".." || e.name.length == 0)
                continue

            cb(e,path)

            if(is_directory_eager(fs,e)){
                recursedirs(fs,e.inode,path+"/"+e.name,cb)
            }
        }
        return@enumblocks true
    }
}

interface action {
    fun perform(fs: Ext2FileSystem)
}

class verboselistfiles: action {
    override fun perform(fs: Ext2FileSystem) {
        recursedirs(fs,fs.rootdir_in.toUInt(),""){e,path->
            val i = fs.getinode(e.inode);
            println(String.format(
                "%9d %s %5d %5d %10d %s [%s%c] %s/%s",
                e.inode.toInt(),i.modestr(),i.i_uid.toInt(),i.i_gid.toInt(),
                i.i_size.toInt(), timestr(i.i_mtime),
                if(e.filetype>=8u) "**" else "","0-dcbpsl"[e.filetype.toInt() and 7],
                path,e.name
            )
            )
        }
    }
}

class listfiles: action {
    override fun perform(fs: Ext2FileSystem) {
        recursedirs(fs,fs.rootdir_in.toUInt(),""){e,path->
            println(String.format(
                "%9d %s%c %s/%s",e.inode.toInt(),
                if(e.filetype>=8u) "**" else "","0-dcbpsl"[e.filetype.toInt() and 7],
                path,e.name
                )
            )
        }
    }
}

class findfiles(val searchstr:String): action {
    override fun perform(fs: Ext2FileSystem) {

        val matches = findpath(fs,fs.rootdir_in.toUInt(),searchstr,"")
        if(matches.size == 0){
            println("findfiles: file not found")
            return
        }
        println("Found ${matches.size} items in media:")
        matches.forEach {item->
            val i = fs.getinode(item.first)
            if(!i._empty){
                println(String.format(
                    "%9d %9d %s - %s",item.first.toInt(),i.i_size.toInt(),i.modestr(),
                    item.second
                )
                )
            }
        }
    }
}

class hexdumpinode(val nr:UInt): action {
    override fun perform(fs: Ext2FileSystem) {
        val i = fs.getinode(nr)
        if(!i._empty){
            var ofs=0;
            i.enumblocks(fs.superblk){
                println(HexDump.hexdump(it,ofs))
                ofs += it.size
                return@enumblocks true
            }
        }
    }
}

fun prepareOutfile(save_path_:String):File{
    var save_path = save_path_
    if(!save_path.contains(File.separatorChar)){
        //Find current working directory if no path is given
        val currentRelativePath = Paths.get("")
        val s = currentRelativePath.toAbsolutePath().toString()
        save_path = s + File.separatorChar + save_path
    }
    val f = File(save_path)
    f.parentFile.mkdirs()
    if(f.isDirectory){
        println("preparing($save_path_): save path should not be a directory")
        throw RuntimeException("preparing($save_path_): save path should not be a directory")
    }
    if(f.exists() && f.isFile) f.delete()
    f.createNewFile()
    return f
}

class exportfile(val extpath:String,val _save_path:String): action {
    override fun perform(fs: Ext2FileSystem) {
        val searchstr = if(extpath[0] == File.separatorChar) extpath.substring(1) else extpath

        val ino = searchpath(fs,fs.rootdir_in.toUInt(),searchstr)
        if(ino == 0u){
            println("exportfile: path not found")
            return
        }
        var save_path = _save_path
        if(save_path.endsWith(File.separatorChar) || save_path.length == 0){
            val p = Paths.get(extpath)
            val fname = p.fileName.toString()
            save_path = Path.of(save_path,fname).toString()
        }
        val byino = exportinode(ino,save_path)
        byino.perform(fs)
        println(String.format("exportfile: found. inode no.%d",ino.toInt()))
    }
}

class exportinode(val nr:UInt,val _save_path:String): action {
    override fun perform(fs: Ext2FileSystem) {
        val i = fs.getinode(nr)
        if(!i._empty){
            if(i.issymlink()){
                println("exportinode: symlink is not supported.")
                return
            }
            val f = prepareOutfile(_save_path)
            val fw=FileOutputStream(f)
            var totalBytesWritten = 0L
            i.enumblocks(fs.superblk){ block ->
                totalBytesWritten+=block.size
                //println(block.size)
                fw.write(block)
                //fw.flush()
                return@enumblocks true
            }
            fw.flush()
            //var fsize = i.i_size.toLong();
            //println(">"+fsize+"/"+(fsize.toDouble()/1024.0/1024.0));
            //truncate file via channel
            val finalSize = minOf(i.i_size.toLong(),totalBytesWritten)
            if (i.i_size.toLong() > totalBytesWritten) {
                println("warning: file size mismatch to inode size(${i.i_size}) and actual size($totalBytesWritten)")
                //var diff = i.i_size.toLong() - totalBytesWritten
                //println("diff:"+diff+">"+diff.toDouble()/4096)
            }

            if(finalSize >= 0){
                fw.channel.truncate(finalSize);
            }


            fw.channel.force(true);
            //chn.lock()
            fw.channel.lock();
            //chn.close()
            fw.close()
            //Files.setPosixPermission to set a file permission

            //Files.setLastModifiedTime(lastAccessTime is not modifiable in NIO)
        }
    }
}

class wildcardextractfiles(val searchstr:String,val _save_path:String,val ignoreDir:Boolean=false): action {
    override fun perform(fs: Ext2FileSystem) {
        val matches = findpath(fs,fs.rootdir_in.toUInt(),searchstr,"")
        if(matches.size == 0){
            println("wildcardextractfiles: file not found")
            return
        }

        val fnameCheck = mutableSetOf<String>()
        matches.forEach{
            val i = fs.getinode(it.first)
            if(!i._empty){
                //var ofs=0;
                println("wildcard dump files:${it.second}(${it.first})")
                var save_path = _save_path
                if(save_path.length == 0){
                    val currentRelativePath = Paths.get("")
                    save_path = currentRelativePath.toAbsolutePath().toString()
                }
                var path:String
                if(ignoreDir){
                    //get file name from it.second
                    val p = Paths.get(it.second)
                    val fname = p.fileName.toString()
                    if(fnameCheck.add(fname)){
                        path = Paths.get(save_path).resolve(fname).toAbsolutePath().toString()
                    } else {
                        path = Paths.get(save_path).resolve(fname+"_"+it.first).toAbsolutePath().toString()
                    }
                } else {
                    path = Paths.get(save_path).resolve(it.second).toAbsolutePath().toString()
                }
                val ef = exportinode(it.first,path)
                ef.perform(fs)
            }
        }
    }
}
class wildcarddumpfiles(val searchstr:String,val dryrun:Boolean=false): action {
    override fun perform(fs: Ext2FileSystem) {

        val matches = findpath(fs,fs.rootdir_in.toUInt(),searchstr,"")
        if(matches.size == 0){
            println("wildcard dumpfiles: file not found")
            return
        }
        println("Wild card matches : ${matches.size} items in media")
        matches.forEach{
            val i = fs.getinode(it.first)
            if(!i._empty){
                var ofs=0;
                if(dryrun == false) {
                    println("wildcard dump files:${it.second}(${it.first})")
                    i.enumblocks(fs.superblk) {
                        println(HexDump.hexdump(it, ofs))
                        ofs += it.size
                        return@enumblocks true
                    }
                } else {
                    println("Found item:${it.second}(${it.first})")
                }
            }
        }
    }
}

class hexdumpfile(val ext2path:String): action {
    override fun perform(fs: Ext2FileSystem) {
        val path = if(ext2path[0] == File.separatorChar) ext2path.substring(1) else ext2path

        val ino = searchpath(fs,fs.rootdir_in.toUInt(),path)
        if(ino == 0u){
            println("hexdumpfile: path not found")
            return
        }
        println(String.format("hexdumpfile: found. inode no.%d",ino.toInt()))
        val dump_action = hexdumpinode(ino)
        dump_action.perform(fs)
    }
}

class dumpfs: action {
    override fun perform(fs: Ext2FileSystem) {
       fs.dump()
    }
}

class checkformat: action {
    override fun perform(fs: Ext2FileSystem) {

        println("FileType:"+fs.fileSystemType)
        if(fs.fileSystemType == ImageType.EXT2
            || fs.fileSystemType == ImageType.EXT3
            || fs.fileSystemType == ImageType.EXT4){
            fs.superblk.dump()
        } else if(fs.fileSystemType == ImageType.UNKNOWN){
            println("checkformat: not an ext2 fs")

            //fs.dump()
        }
        //Count Files , Check File Format?
    }
}

class dumpblocks(val first:UInt,val last:UInt): action {
    override fun perform(fs: Ext2FileSystem) {
        try {
            for (nr in first until last) {
                println(String.format("Block %s:", nr))
                println(String.format("%s", HexDump.hexdump(fs.superblk.getblock(nr))))
            }
        } catch (e: Exception) {
            println("dumpblocks: out of the bound")
        }
    }
}