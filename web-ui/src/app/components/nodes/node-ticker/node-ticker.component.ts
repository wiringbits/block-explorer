import { Component, OnInit } from '@angular/core';

import { TickerService } from '../../../services/ticker.service';
import { XSNService } from '../../../services/xsn.service';
import { ServerStats } from '../../../models/ticker';
import { RewardsSummary, NodeStats } from '../../../models/xsn';
import { Config } from '../../../config';

@Component({
  selector: 'app-node-ticker',
  templateUrl: './node-ticker.component.html',
  styleUrls: ['./node-ticker.component.css']
})
export class NodeTickerComponent implements OnInit {

  stats: ServerStats = new ServerStats();
  nodeStats: NodeStats = new NodeStats();
  rewardsSummary: RewardsSummary = new RewardsSummary();
  config = Config;

  constructor(private tickerService: TickerService, private xsnService: XSNService) { }

  ngOnInit() {
    this.tickerService
      .get()
      .subscribe(
        response => this.stats = response,
        response => this.onError(response)
      );

    this.xsnService
      .getRewardsSummary()
      .subscribe(
        response => this.rewardsSummary = response,
        response => this.onError(response)
      );

    this.xsnService
      .getNodeStats()
      .subscribe(
        response => this.nodeStats = response,
        response => this.onError(response)
      );

      
  }

  private onError(response: any) {
    console.log(response);
  }
}
