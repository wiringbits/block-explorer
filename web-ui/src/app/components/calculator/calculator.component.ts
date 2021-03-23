import { Component, OnInit, OnDestroy } from '@angular/core';
import { ServerStats } from '../../models/ticker';
import { TickerService } from '../../services/ticker.service';
import { XSNService } from '../../services/xsn.service';
import { RewardsSummary, NodeStats, Prices } from '../../models/xsn';

@Component({
  selector: 'app-calculator',
  templateUrl: './calculator.component.html',
  styleUrls: ['./calculator.component.css']
})
export class CalculatorComponent implements OnInit {

  transaction: any;
  prices: Prices = new Prices();
  stats: NodeStats = new NodeStats();
  rewardsSummary: RewardsSummary = new RewardsSummary();
  serverStats: ServerStats = new ServerStats();
  holdAmount = null;

  requiredForMasternode = 15000;

  masternodeCount = 0;
  xsnStaking = 0;
  mnstaking = 0;
  stakingcoin = 0;

  interval: any;

  // hydra node
  xsnPriceLog = 0;
  xsnPrice = 0;
  tradingVolumeLog = 0;
  tradingVolume = 0;
  ownedNodesLog = 0;
  ownedNodes = 0;
  masternodeCountLog = 2700;
  dayMonthYear = "month";
  orderbookHostingEnabled = true; // Used to calculate orderbook rewards, included in total rewards calculation
  orderbookMNs = 1500;
  blockRewards = 0;
  orderbookRewards = 0;
  mnHostingCost = 0;
  mnCollateralValue = 0;
  totalRewards = 0;
  roi = null;
  daysUntilFreeMasternode = null;

  public value: number = 30;
  public rangevalue: Number[] = [30,70];
  public range: string = 'MinRange';

  constructor(private tickerService: TickerService, private xsnService: XSNService) { }

  ngOnInit() {
    this.loadData();
    this.interval = setInterval(() => this.loadData(), 10000);
  }

  ngOnDestroy() {
    clearInterval(this.interval);
    this.interval = null;
  }

  loadData() {
    this.tickerService
      .get()
      .subscribe(
        response => {
          this.serverStats = response;
          this.onChangeAmount();
        },
        response => this.onError(response)
      );
      
    this.xsnService
      .getRewardsSummary()
      .subscribe(
        response => {
          this.rewardsSummary = response;
          this.onChangeAmount();
        },
        response => this.onError(response)
      );

    this.xsnService
      .getNodeStats()
      .subscribe(
        response => {
          this.stats = response;
          this.onChangeAmount();
        },
        response => this.onError(response)
      );

    this.xsnService
      .getPrices()
      .subscribe(
        response => {
          this.prices = response;
          this.onChangeAmount();
        },
        response => this.onError(response)
      );
  }

  private onError(response: any) {
    console.log(response);
  }

  onChangeAmount() {
    if (this.holdAmount > 99999999) {
      this.holdAmount = 99999999;
    }
    this.stakingcoin = this.rewardsSummary.stakingROI * this.holdAmount;
    this.mnstaking = Math.floor(this.holdAmount / this.requiredForMasternode) * this.requiredForMasternode * this.rewardsSummary.masternodesROI + (this.holdAmount % this.requiredForMasternode) * this.rewardsSummary.stakingROI;
    if (this.stakingcoin > this.mnstaking) {
      this.masternodeCount = 0;
      this.xsnStaking = this.holdAmount || 0;
    } else {
      this.masternodeCount = Math.floor(this.holdAmount / this.requiredForMasternode);
      this.xsnStaking = this.holdAmount % this.requiredForMasternode || 0;
    }
  }

  toFinString (x: number, fixed: number = 2) {
    if (x < 1000000) {
        return x.toFixed(fixed).toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
    } else if (x < 1000000000) {
        return (x / 1000000).toFixed(fixed).toString() + 'M'
    } else {
        return (x / 1000000000).toFixed(fixed).toString() + 'B'
    }
  }

  xsnPriceLogChange() {
    // xsnPriceChange handler
    if (this.xsnPriceLog == 100) {
      this.xsnPrice = 1000;
    } else if (this.xsnPriceLog > 98) {
      this.xsnPrice = 500;
    } else{
      this.xsnPrice = Math.round(Math.exp(Math.log(1) + ((Math.log(500) - Math.log(1)) / 100) * this.xsnPriceLog));
    }
  }

  get dayMonthYearMultiplier() {
    return (this.dayMonthYear === 'day' ? 1 : this.dayMonthYear === 'month' ? 30 : 365);
  }

  tradingVolumeLogChange() {
    const val = Math.round(this.tradingVolumeLog < 50 ? (this.tradingVolumeLog * 20) / 2 : (((this.tradingVolumeLog - 50) * 100 * 3.9) + 500));
    this.tradingVolume = val > 1000 ? Math.round(val / 500) * 500 : val;
  }

  ownedNodesLogChange() {
    this.ownedNodes = Math.round(this.ownedNodesLog < 50 ? this.ownedNodesLog / 2 : (((this.ownedNodesLog - 50) * 1.5) + 25));
  }

  calculateHydraResult() {
    this.blockRewards = (this.ownedNodes / this.masternodeCountLog) * (1440 * 9) * this.xsnPrice * this.dayMonthYearMultiplier;
    this.orderbookRewards = Math.max(0.225, (0.45 - (0.000225 * this.tradingVolume))) * (this.tradingVolume * 0.0025 * 1000000) * (this.ownedNodes / this.orderbookMNs) * this.dayMonthYearMultiplier;
    this.mnHostingCost = this.ownedNodes * (this.orderbookHostingEnabled ? 2.5 : 0.15) * this.dayMonthYearMultiplier;
    this.mnCollateralValue = this.xsnPrice * this.ownedNodes * 15000;
    this.totalRewards = this.blockRewards + (this.orderbookHostingEnabled ? this.orderbookRewards : 0) - this.mnHostingCost;
    this.roi = ((((this.mnCollateralValue + this.totalRewards) / this.mnCollateralValue) - 1) * 100).toFixed(2);
    this.daysUntilFreeMasternode = ((15000 * this.xsnPrice) / (this.totalRewards / this.dayMonthYearMultiplier)).toFixed(1) + ' days';
  }
}
