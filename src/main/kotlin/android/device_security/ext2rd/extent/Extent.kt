@file:OptIn(ExperimentalUnsignedTypes::class)
package android.device_security.ext2rd.extent

import android.device_security.debug.HexDump
import android.device_security.ext2rd.Inode
import android.device_security.ext2rd.SuperBlock
import android.device_security.ext2rd.toHex
import kotlin.collections.toUByteArray
import kotlin.math.log
import kotlin.text.toUInt

class Extent(private val inode: Inode) {
    val eh: ExtentHeader = ExtentHeader()
    val extents: MutableList<ExtentNode> = arrayListOf()
    fun parse(buf:UByteArray,first: Int){
        var p:Int= first
        HexDump.hexdump(buf.toByteArray())
        eh.parse(buf,p);p+=12;
        if(eh.eh_magic.toUInt()!= 0xf30au){
            println(String.format("invalid ehmagic=%04x - %s", eh.eh_magic.toInt(),
                buf.copyOfRange(0,12).toHex()))
            //println(HexDump.hexdump(buf.toByteArray()))

            //eh.dump()
            //throw "invalid extent hdr magic";
            return
        }/* else {
            //println(HexDump.hexdump(buf.toByteArray()))
            //eh.dump()
        }*/
        for (i in 0 until eh.eh_entries.toInt()) {
            if (eh.eh_depth.toInt() == 0){
                extents.add(ExtentLeaf(buf,p,inode))
            } else {
                extents.add(ExtentInternal(buf,p,inode))
            }
            p += 12
        }
        if (eh.eh_depth.toInt() == 0 && extents.isNotEmpty()) {
            extents.sortBy { (it as ExtentLeaf).ee_block }
        }

    }
    fun enumblocks(super_: SuperBlock, cb:(ub:ByteArray)->Boolean):Int{

        var startPhysBlock = 0UL
        var currentPhysBlock = 0UL
        var logicalBlock = 0U
        for(i in 0 until eh.eh_entries.toInt()){
            val extentLeaf = extents.getOrNull(i) as? ExtentLeaf
            if(extentLeaf !== null) {
                //extentLeaf.dump()

                if (currentPhysBlock == 0UL) {
                    currentPhysBlock = extentLeaf.startblock()
                    startPhysBlock = currentPhysBlock
                } else {
                    startPhysBlock = extentLeaf.startblock()
                }
                val logicalBlockNew = extentLeaf.ee_block

                //println("physBlock=$currentPhysBlock,$startPhysBlock logBlock=$logicalBlock,$logicalBlockNew")
                var blkProcessed=0
                if (logicalBlock == logicalBlockNew) {
                    blkProcessed = extents[i].enumblocks(super_, cb)
                    if (blkProcessed <= 0) {
                        return -1
                    }
                    currentPhysBlock += blkProcessed.toUInt()
                } else if(logicalBlock<logicalBlockNew){
                    //println("p2:"+(logicalBlock*super_.blocksize()).toInt())

                    val until = logicalBlockNew-logicalBlock

                    for(i in 0 until until.toInt()){
                        cb(super_.blankblock())
                        logicalBlock += 1U
                    }
                    val blkProcessed_ = extents[i].enumblocks(super_, cb)
                    if (blkProcessed_<=0) {
                        return -1
                    }
                    currentPhysBlock += blkProcessed_.toUInt()
                    blkProcessed += blkProcessed_

                }
                //println("blkprocessed $blkProcessed $logicalBlock")
                logicalBlock += blkProcessed.toUInt()
            } else {
                //println("size = ${inode.i_size},inode =${inode.i_gid},${inode.i_uid}")
                val extentInternal = extents.getOrNull(i) as? ExtentInternal
                if(extentInternal !== null) {
                    //Basically ExentInternal used for
                    //extentInternal.dump()
                    //return false
                    //println("extent internal ")
                    if (extents[i].enumblocks(super_, cb)<=0) {
                        return -1
                    }
                }
            }
        }
        return 1
    }

    /**
     * ファイルの論理ブロックを0から順に列挙し、コールバックに渡す。
     * 「穴」はゼロ埋めブロックを生成。
     */
    /*
    fun enumLogicalBlocks(
        super_: SuperBlock,
        fileSize: ULong, // inode.i_size
        cb: (blockData: ByteArray) -> Boolean // 各ブロックデータ (またはゼロ埋め) を処理
    ): Boolean {
        val blockSize = super_.s_log_block_size.toInt()
        val totalLogicalBlocks = (fileSize + blockSize.toULong() - 1u) / blockSize.toULong()

        var currentLogicalBlock: ULong = 0u // 現在処理中のファイルの論理ブロック番号
        var extentIndex = 0 // extents リストの現在のインデックス

        while (currentLogicalBlock < totalLogicalBlocks) {
            if (eh.eh_depth.toInt() == 0) { // リーフノード (実際のデータマッピングを持つ)
                val currentExtentLeaf: ExtentLeaf? = extents.getOrNull(extentIndex) as? ExtentLeaf

                when {
                    // ケース1: 現在の論理ブロックに一致するエクステントがある
                    currentExtentLeaf != null && currentExtentLeaf.ee_block.toULong() == currentLogicalBlock -> {
                        val numBlocksInExtent = currentExtentLeaf.getActualLength().toUInt()
                        val physicalStartBlock = currentExtentLeaf.physicalStartBlock()

                        for (i in 0u until numBlocksInExtent) {
                            if (currentLogicalBlock >= totalLogicalBlocks) break // ファイルサイズ超過

                            val blockData = if (currentExtentLeaf.isInitialized()) {
                                super_.getBlock(physicalStartBlock + i.toULong()) // 実データ
                            } else {
                                ByteArray(blockSize) // 未初期化ならゼロ埋め
                            }
                            if (!cb(blockData)) return false // コールバックが処理中断を要求
                            currentLogicalBlock++
                        }
                        extentIndex++ // 次のエクステントへ
                    }
                    // ケース2: 次のエクステントはまだ先 (または currentExtentLeaf が null でもない) -> 「穴」
                    currentExtentLeaf == null || currentExtentLeaf.ee_block.toULong() > currentLogicalBlock -> {
                        if (!cb(ByteArray(blockSize))) return false // ゼロ埋めブロックを渡す
                        currentLogicalBlock++
                    }
                    // ケース3: (通常は到達しないはずだが) 予期せぬ状態。安全のため「穴」として扱う
                    else -> {
                        if (!cb(ByteArray(blockSize))) return false
                        currentLogicalBlock++
                    }
                }
            } else { // インデックスノード (ツリーの内部ノード)
                // この部分は、ExtentInternal がどのように次のレベルのブロック列挙をサポートするかに依存します。
                // currentLogicalBlock を含む範囲を指す internalNode を見つけ、
                // そのノードに対して再帰的に enumLogicalBlocks (または類似のメソッド) を呼び出す必要があります。
                // この再帰呼び出しは、処理した論理ブロック数を返すか、
                // currentLogicalBlock を適切に進める必要があります。

                // --- 以下は大幅な簡略化とTODO ---
                var processedByChild = false
                for (node_idx in extentIndex until extents.size) { // 適切な internalNode を探す
                    val internalNode = extents[node_idx] as ExtentInternal
                    // TODO: currentLogicalBlock が internalNode.ei_block の範囲内かチェックする
                    // (このチェックは、internalNode がカバーする論理ブロック範囲の開始と終了を知る必要がある)

                    // 仮に internalNode が currentLogicalBlock から始まる範囲を処理すると仮定
                    if (internalNode.ei_block.toULong() <= currentLogicalBlock) {
                        val nextLevelExtent = Extent()
                        val ub = super_.getBlock(internalNode.leafnode().toUInt()).toUByteArray()
                        nextLevelExtent.parse(ub, 0)

                        // 次のレベルに渡すファイルサイズや開始論理ブロックの調整が必要
                        // ここでは、1ブロックだけ処理を委譲し、成功したら進むという単純な仮定
                        var childProcessedCount = 0UL
                        val success = nextLevelExtent.enumLogicalBlocks(super_, fileSize /*TODO:調整必要*/ ) { data ->
                            val result = cb(data)
                            if (result) childProcessedCount++
                            result && (currentLogicalBlock + childProcessedCount < totalLogicalBlocks) // ファイルサイズ超えない範囲で
                        }
                        if (!success && childProcessedCount == 0UL) return false // 子が即失敗

                        currentLogicalBlock += childProcessedCount
                        if (childProcessedCount > 0) extentIndex = node_idx // 同じ internalNode を再度使わないように
                        if (currentLogicalBlock >= totalLogicalBlocks || childProcessedCount > 0) {
                            processedByChild = true
                            break // このループでの処理は完了
                        }
                        // もし internalNode が currentLogicalBlock をカバーしないなら、次の internalNode へ
                    } else {
                        // この internalNode はまだ先なので、今の currentLogicalBlock は「穴」かもしれない
                        // (ただし、インデックスノードの構造上、通常は隙間なくカバーされるか、
                        //  リーフと同様に ei_block が次の開始点を示す)
                        break // 次の論理ブロックで再評価
                    }
                }
                if (!processedByChild && currentLogicalBlock < totalLogicalBlocks) {
                    // どの internalNode も処理しなかった場合、現在の論理ブロックは「穴」とみなす
                    // (インデックスノードの設計によっては、これはエラーケースかもしれない)
                    if (!cb(ByteArray(blockSize))) return false
                    currentLogicalBlock++
                }
                // --- ここまで大幅な簡略化とTODO ---
            }
        }
        return true
    }*/

    fun dump()
    {
        var i=0;
        eh.dump();
        val leaf0 = extents[0] as ExtentLeaf

        extents.forEach {e->
            print(String.format("EXT %d: ",i))
            e.dump()
            i++;
        }
    }
}