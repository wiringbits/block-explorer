
import { tap } from 'rxjs/operators';
import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';

import { Balance } from '../../../models/balance';

import { BalancesService } from '../../../services/balances.service';
import { TickerService } from '../../../services/ticker.service';
import { ServerStats } from '../../../models/ticker';

import { getNumberOfRowsForScreen } from '../../../utils';
import { addressLabels } from '../../../config';

@Component({
  selector: 'app-address-list',
  templateUrl: './address-list.component.html',
  styleUrls: ['./address-list.component.css']
})
export class AddressListComponent implements OnInit {

  // ticker
  ticker: ServerStats;

  // pagination
  limit = 100;
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
    // people started using this view to compute all the generated coins which is not possible
    // because this view was designed to display the richest addresses only, computing balances
    // here leads to inaccurate results.
    //
    // for now, let's limit this to 100 results only.
    // if (this.items.length >= 100) {
    //   return;
    // }

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
