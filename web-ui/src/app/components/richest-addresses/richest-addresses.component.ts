import { Component, OnInit, OnDestroy } from '@angular/core';

import 'rxjs/add/operator/do';
import 'rxjs/add/operator/map';

import { Balance } from '../../models/balance';

import { BalancesService } from '../../services/balances.service';
import { TickerService } from '../../services/ticker.service';
import { ServerStats } from '../../models/ticker';

@Component({
  selector: 'app-richest-addresses',
  templateUrl: './richest-addresses.component.html',
  styleUrls: ['./richest-addresses.component.css']
})
export class RichestAddressesComponent implements OnInit {

  // ticker
  ticker: ServerStats;

  // pagination
  limit = 10;
  items: Balance[] = [];

  constructor(
    private balancesService: BalancesService,
    private tickerService: TickerService) { }

  ngOnInit() {
    this.load();
    this.tickerService.get().subscribe(response => this.ticker = response);
  }

  load() {
    let lastSeenAddress = '';
    if (this.items.length > 0) {
      lastSeenAddress = this.items[this.items.length - 1].address;
    }

    this.balancesService
      .getHighest(this.limit, lastSeenAddress)
      .do(response => this.items = this.items.concat(response.data))
      .subscribe();
  }

  getPercent(balance: Balance): number {
    return balance.available * 100 / this.ticker.circulatingSupply;
  }
}
