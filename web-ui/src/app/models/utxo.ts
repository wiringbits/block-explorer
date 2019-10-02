export class UTXO {
  constructor(
    public address: string,
    public satoshis: number,
    public script: string,
    public txid: string,
    public outputIndex: number
  ) { }
}
