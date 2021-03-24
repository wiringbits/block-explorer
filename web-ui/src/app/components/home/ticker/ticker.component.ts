import { Component, Input, OnInit } from '@angular/core';

import { TickerService } from '../../../services/ticker.service';
import { XSNService } from '../../../services/xsn.service';
import { NodeStats, Prices, ServerStats } from '../../../models/ticker';
import { RewardsSummary } from '../../../models/xsn';
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

  amAgo = amAgo;
  numberWithCommas = numberWithCommas;

  constructor(private tickerService: TickerService, private xsnService: XSNService) { }

  ngOnInit() {
    this.tickerService
      .get()
      .subscribe(
        response => {
          this.ticker = response;
          console.log(this.ticker);
        },
        response => this.onError(response)
      );

    this.tickerService
      .getNodeStats()
      .subscribe(
        response => this.nodeStats = response,
        response => this.onError(response)
      );

    this.tickerService
      .getPrices()
      .subscribe(
        response => this.prices = response,
        response => this.onError(response)
      );

    this.xsnService
      .getRewardsSummary()
      .subscribe(
        response => this.rewardsSummary = response,
        response => this.onError(response)
      );
  }

  private onError(response: any) {
    console.log(response);
  }
}
