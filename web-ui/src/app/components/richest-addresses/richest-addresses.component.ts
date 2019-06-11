
import {tap} from 'rxjs/operators';
import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';

import { Balance } from '../../models/balance';

import { BalancesService } from '../../services/balances.service';
import { TickerService } from '../../services/ticker.service';
import { ServerStats } from '../../models/ticker';

import { getNumberOfRowsForScreen } from '../../utils';
import { addressLabels } from '../../config';

@Component({
  selector: 'app-richest-addresses',
  templateUrl: './richest-addresses.component.html',
  styleUrls: ['./richest-addresses.component.css']
})
export class RichestAddressesComponent implements OnInit {

  // ticker
  ticker: ServerStats;

  // pagination
  limit = 30;
  items: Balance[] = [];

  addressLabel = addressLabels;

  constructor(
    private balancesService: BalancesService,
    private tickerService: TickerService) { }

  ngOnInit() {
    const height = this.getScreenSize();
    this.limit = getNumberOfRowsForScreen(height);
    this.load();
    this.tickerService.get().subscribe(response => this.ticker = response);
  }

  load() {
    let lastSeenAddress = '';
    if (this.items.length > 0) {
      lastSeenAddress = this.items[this.items.length - 1].address;
    }

    this.balancesService
      .getHighest(this.limit, lastSeenAddress).pipe(
      tap(response => this.items.push(...response.data)))
      .subscribe();
  }

  @HostListener('window:resize', ['$event'])
  private getScreenSize(_?): number {
    return window.innerHeight;
  }

  getPercent(balance: Balance): number {
    return balance.available * 100 / this.ticker.circulatingSupply;
  }
}
