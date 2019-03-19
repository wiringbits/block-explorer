package com.xsn.explorer.data.anorm.dao

import java.sql.Connection

import anorm._
import com.xsn.explorer.data.anorm.parsers.BlockFilterParsers
import com.xsn.explorer.gcs.GolombCodedSet
import com.xsn.explorer.models.values.Blockhash

class BlockFilterPostgresDAO {

  import BlockFilterParsers._

  def insert(blockhash: Blockhash, filter: GolombCodedSet)(implicit conn: Connection): GolombCodedSet = {
    SQL(
      """
        |INSERT INTO block_address_gcs
        |  (blockhash, m, n, p, hex)
        |VALUES
        |  ({blockhash}, {m}, {n}, {p}, {hex})
        |RETURNING blockhash, m, n, p, hex
      """.stripMargin
    ).on(
      'blockhash -> blockhash.string,
      'm -> filter.m,
      'n -> filter.n,
      'p -> filter.p,
      'hex -> filter.hex.string
    ).as(parseFilter.single)
  }

  def delete(blockhash: Blockhash)(implicit conn: Connection): Option[GolombCodedSet] = {
    SQL(
      """
        |DELETE FROM block_address_gcs
        |WHERE blockhash = {blockhash}
        |RETURNING blockhash, m, n, p, hex
      """.stripMargin
    ).on(
      'blockhash -> blockhash.string
    ).as(parseFilter.singleOpt)
  }
}
