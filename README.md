# ktextrd

Executable jar file to dump/export files in ext2/3/4 images also it supports sparse format.
The tool is intended to analysis android's ota/factory files statically.

Basically this is a kotlin port of [ext2rd](https://github.com/nlitsme/extfstools).
Though it lacks several operating system dependent features,
But added some features and fix to help your analysis.

 - Wild card search in images
 - Extract multiple files without maintaining original directory structure

You can also include it in your project as a library to help research.
Enjoy!

## Build
```
./gradlew shadowJar 
```

Run jar file with a below command.

```
java -jar dist/ktextrd-all.jar
```
You can also execute it by a shell file in the project directory.
```
./ktextrd.sh
```


## Command Line Options
### **-l**
List files in the image.
```
./ktextrd.sh ext2img.ext4 -l
```
### **-v**
Verbosely list files in the image.
```
./ktextrd.sh ext2img.ext4 -v
```
### **-d**
Dump elements(superblock,inodes...) in the image.
```
./ktextrd.sh ext2img.ext4 -d
```
### **-fd &lt;file path | inode number&gt;**
Dump a file in the image.
You can specify the file by inode number or path.
```
./ktextrd.sh ext2img.ext4 -fd /root/hello.c
./ktextrd.sh ext2img.ext4 -fd #37
```
### **-fx &lt;file path | inode number&gt;[:output file]**
Export a file in the image.  
(optional) You can add the output path after the identifier of the file by separating ':'
```
./ktextrd.sh ext2img.ext4 -fx /root/hello.c
./ktextrd.sh ext2img.ext4 -fx /root/hello.c:hello.c
./ktextrd.sh ext2img.ext4 -fx #37:hello.c
```

### **-wd &lt;file pattern&gt;**
Dump files in the image. You can specify the files by a search string.
You can include '?' and '*' as the wild card matcher in the search string.
In case testing your search string, 
it provides '-w' option which list matched files instead of showing file dump. 
```
./ktextrd.sh ext2img.ext4 -w */inode.c
./ktextrd.sh ext2img.ext4 -wd */inode.c
```
### **-wx[f] &lt;file pattern&gt;:&lt;outputpath&gt;**
Export files which are founded by search string.
You have to add the output path after the search string by separating ':'.
The output path is a mandatory parameter and you must specify existent directory.
```
./ktextrd.sh ext2img.ext4 -wx *.c:/dist
```
Basically the command tries to maintain the original directory structure.
If you want to output all files under a specific directory. add a 'f'lat option to the option.
(Inode numbers are appended to the file name inc case file names are duped)
```
./ktextrd.sh ext2img.ext4 -wxf *.c:./dist
```
### **-b &lt;*start block*&gt;[-*end block*]**
Dump a block or blocks by number.
```
./ktextrd.sh ext2img.ext4 -b 10
./ktextrd.sh ext2img.ext4 -b 100-115
```
### **-S &lt;*new offset*&gt;**
Update offset bytes from a file head to the super block area. (Default = 1024 byte)

### **-R &lt;*inode number*&gt;**
Override an inode number of the root directory of the image. (Default number = 2)

## Command Line Options For SuperImage (Logical Partition)
Basically the tool if for reading a ext2 or ext4 format image file.
But also, it can read a logical partition image file like super.img  
If the tool detects the file is a logical partition image file, 
you can operate the file with the following options. The feature support to read sparse format file.
### **-d**
Dump elements(each images) in the image.
### **-x &lt;outputpath&gt;**
Expand all images in the file into output path

## to do
 - Restore timestamp and attributes of file when exports
 - Dump all files in image
 - Maven repository settings
 - Add useful tool for factory/ota image analysis
   - payload.bin support
 - Refactoring
