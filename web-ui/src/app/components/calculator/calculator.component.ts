import { Component, OnInit, OnDestroy, ViewChild, EventEmitter, AfterViewChecked } from '@angular/core';
import { SliderComponent } from '@syncfusion/ej2-angular-inputs';
import { ServerStats } from '../../models/ticker';
import { TickerService } from '../../services/ticker.service';
import { XSNService } from '../../services/xsn.service';
import { RewardsSummary, NodeStats, Prices } from '../../models/xsn';

@Component({
  selector: 'app-calculator',
  templateUrl: './calculator.component.html',
  styleUrls: ['./calculator.component.css']
})
export class CalculatorComponent implements OnInit, AfterViewChecked {

  transaction: any;
  prices: Prices = new Prices();
  stats: NodeStats = new NodeStats();
  rewardsSummary: RewardsSummary = new RewardsSummary();
  serverStats: ServerStats = new ServerStats();
  holdAmount: number = null;

  requiredForMasternode = 15000;

  masternodeCount: number = 0;
  xsnStaking: number = 0;
  mnstaking: number = 0;
  stakingcoin: number = 0;

  stakingWaitTime = "";
  mnWaitTime = "";

  interval: any;

  // hydra node
  xsnPriceLog: number = 1;
  xsnPrice: number = 1;
  xsnPriceInputValue: number = null;

  tradingVolumeLog = 1;
  tradingVolume = 0;
  tradingVolumeValue: number = null;

  ownedNodesLog = 1;
  ownedNodes = 1;
  ownedNodesValue: number = null;

  masternodeCountLog = 2300;
  masternodeCountValue: number = null;

  dayMonthYear = "month";
  orderbookHostingEnabled = true; // Used to calculate orderbook rewards, included in total rewards calculation
  orderbookMNs = 1000;
  orderbookMNsValue: number = null;

  blockRewards = 0;
  orderbookRewards = 0;
  orderbookRewardsString = "";
  mnHostingCost = 0;
  mnCollateralValue = 0;
  totalRewards = 0;
  roi: number = null;
  daysUntilFreeMasternode: number = null;
  dayMonthYearMultiplier = 1;
  loadedData = false;
  tradingVolumeString = "10M";

  xsnPriceInputMode = false;
  totalMasternodeInputMode = false;
  yourMasternodesInputMode = false;

  public value: number = 30;
  public rangevalue: Number[] = [30,70];
  public range: string = 'MinRange';

  public xsnPriceEventEmitter = new EventEmitter<boolean>();
  xsnPriceFocus = false;

  @ViewChild('slider')
  public mnSlider: SliderComponent;

  constructor(private tickerService: TickerService, private xsnService: XSNService) { }

  ngOnInit() {
    this.loadData();
    this.interval = setInterval(() => this.loadData(), 10000);
  }

  ngOnDestroy() {
    clearInterval(this.interval);
    this.interval = null;
  }

  ngAfterViewChecked() {
    if (this.xsnPriceFocus) {
      this.focusOnXSNPrice();
      this.xsnPriceFocus = false;
    }
  }

  focusOnXSNPrice() {
    this.xsnPriceEventEmitter.emit(true);
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

    this.tradingVolumeLogChange();
    
    this.loadedData = true;
  }

  private onError(response: any) {
    console.log(response);
  }

  private yearToTime(num: number) {
    let date = {
      'Y': 0,
      'M': 0,
      'd': 0,
      'h': 0,
      'm': 0,
      's': 0
    }
    if (num >= 1) {
      date['Y'] = Math.floor(num);
    }
    num = (num % 1) * 365;
    if (num / 30 > 0) {
      date['M'] = Math.floor(num / 30);
    }
    num = num % 30;
    if (num >= 1) {
      date['d'] = Math.floor(num);
    }
    num = (num % 1) * 24;
    if (num >= 1) {
      date['h'] = Math.floor(num);
    }
    num = (num % 1) * 60;
    if (num >= 1) {
      date['m'] = Math.floor(num);
    }
    num = (num % 1) * 60;
    if (num > 0) {
      date['s'] = Math.ceil(num);
    }
    return date;
  }

  onChangeAmount() {
    this.stakingWaitTime = "";
    this.mnWaitTime = "";
    if (this.holdAmount > 1000000) {
      this.holdAmount = 1000000;
    }
    this.stakingcoin = this.rewardsSummary.stakingROI * this.holdAmount;
    if (this.holdAmount >= 15000) {
      this.mnstaking = Math.floor(this.holdAmount / this.requiredForMasternode) * this.requiredForMasternode * this.rewardsSummary.masternodesROI + (this.holdAmount % this.requiredForMasternode) * this.rewardsSummary.stakingROI;
    } else {
      this.mnstaking = null;
    }
    if (this.rewardsSummary && this.holdAmount) {
      let val = 9 / (this.rewardsSummary.stakingROI * this.holdAmount);
      let date = this.yearToTime(val);
      for (let key in date) {
        if (date[key] > 0) {
          this.stakingWaitTime += date[key] + key /* + (date[key] > 1 ? 's' : '') */ + ' ';
        }
      }
    } else {
      this.stakingWaitTime = "0h 0m 0s";
    }
    if (this.mnstaking) {
      let val = 9 / this.mnstaking;
      let date = this.yearToTime(val);
      for (let key in date) {
        if (date[key] > 0) {
          this.mnWaitTime += date[key] + key /* + (date[key] > 1 ? 's' : '') */ + ' ';
        }
      }
    } else {
      this.mnWaitTime = "0h 0m 0s";
    }
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
    } else {
      this.xsnPrice = Math.round(Math.exp(Math.log(1) + ((Math.log(500) - Math.log(1)) / 100) * this.xsnPriceLog));
    }
    this.calculateHydraResult();
  }

  xsnPriceInput() {
    this.xsnPrice = this.xsnPriceInputValue ? this.xsnPriceInputValue : 0;
    if (this.xsnPrice == 1000) {
      this.xsnPriceLog = 100;
    } else {
      this.xsnPriceLog = Math.round((100 * Math.log(this.xsnPrice)) / ((2 * Math.log(2)) + (3 * Math.log(5))));
    }
    this.calculateHydraResult();
  }

  ownedNodesInput() {
    this.ownedNodes = this.ownedNodesValue ? this.ownedNodesValue : 0;
    this.ownedNodesLog = Math.round(this.ownedNodes < 25 ? this.ownedNodes * 2 : (((this.ownedNodes - 25) * (2/3)) + 50));
    this.calculateHydraResult();
  }

  masternodeCountInput() {
    this.masternodeCountLog = this.masternodeCountValue ? this.masternodeCountValue : 0;
    this.calculateHydraResult();
  }

  orderbookMNsChange() {
    this.calculateHydraResult();
  }

  orderbookMNsInput() {
    if (this.orderbookMNsValue) {
      this.orderbookMNsValue = Math.ceil(this.orderbookMNsValue / 100) * 100;
      this.orderbookMNs = this.orderbookMNsValue;
    } else {
      this.orderbookMNs = 0;
    }
    this.calculateHydraResult();
  }

  masternodeCountLogChange() {
    this.calculateHydraResult();
  }

  tradingVolumeLogChange() {
    const val = Math.round(this.tradingVolumeLog < 50 ? (this.tradingVolumeLog * 20) / 2 : (((this.tradingVolumeLog - 50) * 100 * 3.9) + 500));
    this.tradingVolume = val > 1000 ? Math.round(val / 500) * 500 : val;
    this.tradingVolumeString = this.tradingVolume < 1000 ? (this.tradingVolume + 'M') : ((this.tradingVolume / 1000).toFixed(2) + "B");
    this.calculateHydraResult();
  }

  ownedNodesLogChange() {
    this.ownedNodes = Math.round(this.ownedNodesLog < 50 ? this.ownedNodesLog / 2 : (((this.ownedNodesLog - 50) * 1.5) + 25));
    this.calculateHydraResult();
  }

  tradingVolumeInput() {
    if (this.tradingVolumeValue) {
      this.tradingVolume = this.tradingVolumeValue;
      this.tradingVolumeLog = Math.round(this.tradingVolume < 500 ? this.tradingVolume / 10 : (((this.tradingVolume - 500) / 390) + 50));
      this.calculateHydraResult();
    }
  }

  orderbookHostingEnabledChange(value) {
    this.orderbookHostingEnabled = value;
    this.calculateHydraResult();
  }

  calculateHydraResult() {
    this.blockRewards = (this.ownedNodes / this.masternodeCountLog) * (1440 * 9) * this.xsnPrice * this.dayMonthYearMultiplier;
    this.orderbookRewards = Math.max(0.225, (0.45 - (0.000225 * this.tradingVolume))) * (this.tradingVolume * 0.0025 * 1000000) * (this.ownedNodes / this.orderbookMNs) * this.dayMonthYearMultiplier;
    this.orderbookRewardsString = this.toFinString(this.orderbookRewards);
    this.mnHostingCost = this.ownedNodes * (this.orderbookHostingEnabled == true ? 2.5 : 0.15) * this.dayMonthYearMultiplier;
    this.mnCollateralValue = this.xsnPrice * this.ownedNodes * 15000;
    this.totalRewards = this.blockRewards + (this.orderbookHostingEnabled == true ? this.orderbookRewards : 0) - this.mnHostingCost;
    this.roi = this.mnCollateralValue > 0 ? (((this.mnCollateralValue + this.totalRewards) / this.mnCollateralValue) - 1) * 100 : 0;
    this.daysUntilFreeMasternode = this.totalRewards > 0 ? (15000 * this.xsnPrice) / (this.totalRewards / this.dayMonthYearMultiplier) : 0;
  }
}
