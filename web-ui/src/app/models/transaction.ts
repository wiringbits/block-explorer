
export class Transaction {
  id: string;
  size: number;
  blockhash: string;
  time: number;
  blocktime: number;
  confirmations: number;
  input: TransactionValue[];
  output: TransactionValue[];
}

class TransactionValue {
  address: string;
  value: number;
}
