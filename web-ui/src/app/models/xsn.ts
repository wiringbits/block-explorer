export class RewardsSummary {
    averageReward: number;
    averageInput: number;
    medianInput: number;
    averagePoSInput: number;
    averageTPoSInput: number;
    medianWaitTime: number;
    averageWaitTime: number;
    rewardedAddressesCountLast72Hours: number;
    rewardedAddressesSumLast72Hours: number;
    masternodesROI: number;
    stakingROI: number;
    coinsTrustlesslyStaking: number
}

export class NodeStats {
    masternodes: number;
    enabledMasternodes: number;
    masternodesProtocols: Record<string, number>;
    tposnodes: number;
    enabledTposnodes: number;
    tposnodesProtocols: Record<string, number>;
}

export class Prices {
    try: number;
    gbp: number;
    nzd: number;
    uah: number;
    mxn: number;
    btc: number;
    jpy: number;
    marketcap: number;
    usd: number;
    volume: number;
    eur: number;
}
