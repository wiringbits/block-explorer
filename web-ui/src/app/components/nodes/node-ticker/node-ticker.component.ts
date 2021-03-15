import { Component, OnInit } from '@angular/core';

import { TickerService } from '../../../services/ticker.service';
import { XSNService } from '../../../services/xsn.service';
import { ServerStats } from '../../../models/ticker';
import { RewardsSummary, NodeStats, Prices } from '../../../models/xsn';
import { Config } from '../../../config';

@Component({
  selector: 'app-node-ticker',
  templateUrl: './node-ticker.component.html',
  styleUrls: ['./node-ticker.component.css']
})
export class NodeTickerComponent implements OnInit {

  stats: ServerStats = new ServerStats();
  prices: Prices = new Prices();
  nodeStats: NodeStats = new NodeStats();
  rewardsSummary: RewardsSummary = new RewardsSummary();
  masternodesProtocols: Array<any> = [];
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
      .getPrices()
      .subscribe(
        response => {
          this.prices = response;
        },
        response => this.onError(response)
      );


    this.xsnService
      .getNodeStats()
      .subscribe(
        response => {
          this.nodeStats = response;
          for (let key in this.nodeStats["masternodesProtocols"]) {
            this.masternodesProtocols.push({"key": key, "value": this.nodeStats["masternodesProtocols"][key]})
          }
          this.masternodesProtocols.sort(function(a, b) {
            if (a.key > b.key) return -1;
            return 1;
          });
        },
        response => this.onError(response)
      );

      
  }

  reverseObject(object) {
    var newObject = {};
    var keys = [];

    for (var key in object) {
        keys.push(key);
    }

    for (var i = keys.length - 1; i >= 0; i--) {
      var value = object[keys[i]];
      newObject[keys[i]]= value;
    }       

    return newObject;
  }

  private onError(response: any) {
    console.log(response);
  }
}
