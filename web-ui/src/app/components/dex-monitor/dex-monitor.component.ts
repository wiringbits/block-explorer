import { Component, OnInit } from '@angular/core';

import { Prices } from '../../models/xsn';
import { NodesInfo, TradesNumber, Volume } from '../../models/orderbook';

import { XSNService } from '../../services/xsn.service';
import { OrderBookService } from '../../services/orderbook.service';

import { FormGroup, Validators, FormControl } from '@angular/forms';

@Component({
  selector: 'app-dex-monitor',
  templateUrl: './dex-monitor.component.html',
  styleUrls: ['./dex-monitor.component.css']
})
export class DexMonitorComponent implements OnInit {

  tradingPair: string;
  selectedTab: number;

  prices: Prices = new Prices();
  nodesInfo: NodesInfo = new NodesInfo();
  tradesNumber: TradesNumber = new TradesNumber();
  volume: Volume = new Volume();

  constructor(
    private xsnService: XSNService,
    private orderBookService: OrderBookService
  ) {
    this.tradingPair = "BTC_USDT";
    this.selectedTab = 1;
  }

  ngOnInit() {
    this.loadData();
  }

  loadData() {
    this.xsnService
      .getPrices()
      .subscribe(
        response => this.prices = response,
        response => this.onError(response)
      );

    this.orderBookService
      .getNodesInfo(this.tradingPair)
      .subscribe(
        response => this.nodesInfo = response,
        response => this.onError(response)
      );

    this.orderBookService
      .getTradesNumber(this.tradingPair, "?lastDays=" + this.selectedTab)
      .subscribe(
        response => this.tradesNumber = response,
        response => this.onError(response)
      );

    this.orderBookService
      .getVolume(this.tradingPair, "?lastDays=" + this.selectedTab)
      .subscribe(
        response => this.volume = response,
        response => this.onError(response)
      );
  }

  changeTradingPair() {
    console.log(this.tradingPair);
    this.loadData();
  }

  selectTab(tab) {
    this.selectedTab = tab;
    this.loadData();
  }

  private onError(response: any) {
    console.log(response);
  }
}
