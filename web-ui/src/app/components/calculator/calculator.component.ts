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
}
