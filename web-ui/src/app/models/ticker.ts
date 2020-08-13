
export class ServerStats {
  totalSupply: number;
  circulatingSupply: number;
  transactions: number;
  blocks: number;
  masternodes: number;
  tposnodes: number;
}

export class NodeStats {
  masternodes: number;
  enabledMasternodes: number;
  masternodesProtocols: Record<string, number>;
  tposnodes: number;
  enabledTposnodes: number;
  tposnodesProtocols: Record<string, number>;
  coinsStaking: number;
}

export class Prices {
  usd: number;
  btc: number;
  volume: number;
  marketcap: number;
}
