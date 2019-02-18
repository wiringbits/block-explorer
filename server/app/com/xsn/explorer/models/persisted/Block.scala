package com.xsn.explorer.models.persisted

import com.xsn.explorer.models.BlockExtractionMethod
import com.xsn.explorer.models.values._

case class Block(
    hash: Blockhash,
    previousBlockhash: Option[Blockhash],
    nextBlockhash: Option[Blockhash],
    tposContract: Option[TransactionId],
    merkleRoot: Blockhash,
    size: Size,
    height: Height,
    version: Int,
    time: Long,
    medianTime: Long,
    nonce: Long,
    bits: String,
    chainwork: String,
    difficulty: BigDecimal,
    extractionMethod: BlockExtractionMethod)
