
export class TposContract {
    txid: string;
    index: number;
    owner: string;
    merchant: string;
    merchantCommission: number;
    time: number;
    state: string;

    constructor(
        txid: string,
        index: number,
        owner: string,
        merchant: string,
        merchantCommission: number,
        time: number,
        state: string) {

        this.txid = txid;
        this.index = index;
        this.owner = owner;
        this.merchant = merchant;
        this.merchantCommission = merchantCommission;
        this.time = time;
        this.state = state;
    }
}


export class TposContractEncoded {
    tposContractEncoded: String;
}
