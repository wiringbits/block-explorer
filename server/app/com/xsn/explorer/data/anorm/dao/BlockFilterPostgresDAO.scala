package com.xsn.explorer.data.anorm.dao

import java.sql.Connection

import anorm._
import com.xsn.explorer.data.anorm.parsers.BlockFilterParsers
import com.xsn.explorer.gcs.GolombCodedSet
import com.xsn.explorer.models.values.Blockhash

class BlockFilterPostgresDAO {

  import BlockFilterParsers._

  def insert(blockhash: Blockhash, filter: GolombCodedSet)(implicit
      conn: Connection
  ): GolombCodedSet = {
    SQL(
      """
        |INSERT INTO block_address_gcs
        |  (blockhash, m, n, p, hex)
        |VALUES
        |  ({blockhash}, {m}, {n}, {p}, {hex})
        |RETURNING blockhash, m, n, p, hex
      """.stripMargin
    ).on(
      "blockhash" -> blockhash.toBytesBE.toArray,
      "m" -> filter.m,
      "n" -> filter.n,
      "p" -> filter.p,
      "hex" -> filter.getHexString.toBytes
    ).as(parseFilter.single)
  }

  def upsert(blockhash: Blockhash, filter: GolombCodedSet)(implicit
      conn: Connection
  ): Unit = {
    val _ = SQL(
      """
        |INSERT INTO block_address_gcs
        |  (blockhash, m, n, p, hex)
        |VALUES
        |  ({blockhash}, {m}, {n}, {p}, {hex})
        |ON CONFLICT (blockhash) DO UPDATE
        |SET blockhash = EXCLUDED.blockhash,
        |    m = EXCLUDED.m,
        |    n = EXCLUDED.n,
        |    hex = EXCLUDED.hex
      """.stripMargin
    ).on(
      "blockhash" -> blockhash.toBytesBE.toArray,
      "m" -> filter.m,
      "n" -> filter.n,
      "p" -> filter.p,
      "hex" -> filter.getHexString.toBytes
    ).execute()
  }

  def delete(
      blockhash: Blockhash
  )(implicit conn: Connection): Option[GolombCodedSet] = {
    SQL(
      """
        |DELETE FROM block_address_gcs
        |WHERE blockhash = {blockhash}
        |RETURNING blockhash, m, n, p, hex
      """.stripMargin
    ).on(
      "blockhash" -> blockhash.toBytesBE.toArray
    ).as(parseFilter.singleOpt)
  }

  def getBy(
      blockhash: Blockhash
  )(implicit conn: Connection): Option[GolombCodedSet] = {
    SQL(
      """
        |SELECT blockhash, m, n, p, hex
        |FROM block_address_gcs
        |WHERE blockhash = {blockhash}
      """.stripMargin
    ).on(
      "blockhash" -> blockhash.toBytesBE.toArray
    ).as(parseFilter.singleOpt)
  }
}
