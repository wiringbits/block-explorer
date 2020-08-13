import { Component, OnInit } from '@angular/core';

import { TickerService } from '../../../services/ticker.service';
import { NodeStats, Prices, ServerStats } from '../../../models/ticker';
import { Config } from '../../../config';


@Component({
  selector: 'app-ticker',
  templateUrl: './ticker.component.html',
  styleUrls: ['./ticker.component.css']
})
export class TickerComponent implements OnInit {

  ticker: ServerStats = new ServerStats();
  nodeStats: NodeStats = new NodeStats();
  prices: Prices = new Prices();
  config = Config;

  constructor(private tickerService: TickerService) { }

  ngOnInit() {
    this.tickerService
      .get()
      .subscribe(
        response => this.ticker = response,
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
  }

  private onError(response: any) {
    console.log(response);
  }
}
