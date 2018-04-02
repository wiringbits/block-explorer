

export class BlockDetails {
  block: Block;
  rewards: BlockRewards;
}

export class Block {
  hash: string;
  previousBlockhash: string;
  nextBlockhash: string;
  merkleRoot: string;
  transactions: string[];
  confirmations: number;
  size: number;
  height: number;
  version: number;
  time: number;
  medianTime: number;
  nonce: number;
  bits: string;
  chainwork: string;
  difficulty: number;
  tposContract: string;
}

class BlockRewards {
  reward: BlockReward; // PoW
  coinstake: BlockReward; // PoS
  masternode: BlockReward; // PoS and TPoS
  owner: BlockReward; // TPoS
  merchant: BlockReward; // TPoS
}

class BlockReward {
  address: string;
  value: number;
}
