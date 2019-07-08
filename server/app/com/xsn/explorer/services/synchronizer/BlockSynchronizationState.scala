package com.xsn.explorer.services.synchronizer

import enumeratum._

sealed abstract class BlockSynchronizationState(override val entryName: String) extends EnumEntry

object BlockSynchronizationState extends Enum[BlockSynchronizationState] {

  val values = findValues

  final case object StoringBlock extends BlockSynchronizationState("STORING_BLOCK")
  final case object StoringBlockData extends BlockSynchronizationState("STORING_BLOCK_DATA")
  final case object StoringTransactions extends BlockSynchronizationState("STORING_TRANSACTIONS")
  final case object StoringOutputs extends BlockSynchronizationState("STORING_OUTPUTS")
  final case object StoringInputs extends BlockSynchronizationState("STORING_INPUTS")
  final case object SpendingOutputs extends BlockSynchronizationState("SPENDING_OUTPUTS")
  final case object StoringAddressTransactionDetails
      extends BlockSynchronizationState("STORING_ADDRESS_TRANSACTION_DETAILS")
  final case object UpdatingTPoSContracts extends BlockSynchronizationState("UPDATING_TPOS_CONTRACTS")
  final case object UpdatingBalances extends BlockSynchronizationState("UPDATING_BALANCES")

}
