import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';

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
  limit = 30;
  items: Balance[] = [];

  constructor(
    private balancesService: BalancesService,
    private tickerService: TickerService) { }

  ngOnInit() {
    const height = this.getScreenSize();
    this.limit = this.getLimitForScreen(height);
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
      .do(response => this.items.push(...response.data))
      .subscribe();
  }

  @HostListener('window:resize', ['$event'])
  private getScreenSize(event?): number {
    return window.innerHeight;
  }

  private getLimitForScreen(height: number): number {
    if (height < 550) {
      return 10;
    }
    return Math.min(10 + Math.ceil((height - 550) / 20), 100);
  }

  getPercent(balance: Balance): number {
    return balance.available * 100 / this.ticker.circulatingSupply;
  }
}
