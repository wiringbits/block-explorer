
export class LightWalletTransaction {
  id: string;
  size: number;
  blockhash: string;
  time: number;
  inputs: LightWalletTransactionValue[];
  outputs: LightWalletTransactionValue[];
}

class LightWalletTransactionValue {
  index: number;
  value: number;
}
