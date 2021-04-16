import { Component, Input, OnInit } from '@angular/core';

import { TickerService } from '../../../services/ticker.service';
import { XSNService } from '../../../services/xsn.service';
import { ServerStats } from '../../../models/ticker';
import { RewardsSummary, NodeStats, Prices } from '../../../models/xsn';
import { Config } from '../../../config';
import { amAgo, numberWithCommas } from '../../../utils';


@Component({
  selector: 'app-ticker',
  templateUrl: './ticker.component.html',
  styleUrls: ['./ticker.component.css']
})
export class TickerComponent implements OnInit {

  @Input()
  lastBlock: any;
  ticker: ServerStats = new ServerStats();
  nodeStats: NodeStats = new NodeStats();
  prices: Prices = new Prices();
  rewardsSummary: RewardsSummary = new RewardsSummary();
  config = Config;
  interval = null;

  amAgo = amAgo;
  numberWithCommas = numberWithCommas;

  constructor(private tickerService: TickerService, private xsnService: XSNService) { }

  ngOnInit() {
    this.interval = setInterval(() => this.reload(), 10000);
    this.reload();
  }

  ngOnDestroy() {
    clearInterval(this.interval);
    this.interval = null;
  }

  public reload() {
    this.tickerService
      .get()
      .subscribe(
        response => {
          this.ticker = Object.assign({}, response);
        },
        response => this.onError(response)
      );

    this.xsnService
      .getNodeStats()
      .subscribe(
        response => this.nodeStats = Object.assign({}, response),
        response => this.onError(response)
      );

    this.xsnService
      .getPrices()
      .subscribe(
        response => this.prices = Object.assign({}, response),
        response => this.onError(response)
      );

    this.xsnService
      .getRewardsSummary()
      .subscribe(
        response => this.rewardsSummary = Object.assign({}, response),
        response => this.onError(response)
      );
  }

  private onError(response: any) {
    console.log(response);
  }
}
