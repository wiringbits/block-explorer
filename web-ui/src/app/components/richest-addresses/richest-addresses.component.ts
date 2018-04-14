import { Component, OnInit, OnDestroy } from '@angular/core';

import { Observable } from 'rxjs/Observable';

import 'rxjs/add/operator/do';
import 'rxjs/add/operator/map';

import { Balance } from '../../models/balance';

import { BalancesService } from '../../services/balances.service';
import { ErrorService } from '../../services/error.service';
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
  total = 0;
  currentPage = 1;
  pageSize = 10;
  asyncItems: Observable<Balance[]>;

  constructor(
    private balancesService: BalancesService,
    private tickerService: TickerService,
    private errorService: ErrorService) { }

  ngOnInit() {
    this.getPage(this.currentPage);
    this.tickerService.get().subscribe(response => this.ticker = response);
  }

  getPage(page: number) {
    const offset = (page - 1) * this.pageSize;
    const limit = this.pageSize;

    this.asyncItems = this.balancesService
      .getRichest(offset, limit)
      .do(response => this.total = response.total)
      .do(response => this.currentPage = 1 + (response.offset / this.pageSize))
      .map(response => response.data);
  }

  getPercent(balance: Balance): number {
    return balance.available * 100 / this.ticker.circulatingSupply;
  }
}
