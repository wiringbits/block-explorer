export class Fee {
    constructor(
        public name: string,
        public amount: number,
    ) { }
}

export const TransactionFees = [
    new Fee('High', 1000),
    new Fee('Normal', 500),
    new Fee('Low', 100)
];
