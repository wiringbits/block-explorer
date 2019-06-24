import { Component, OnInit } from '@angular/core';

import { TickerService } from '../../services/ticker.service';
import { ServerStats } from '../../models/ticker';


@Component({
  selector: 'app-ticker',
  templateUrl: './ticker.component.html',
  styleUrls: ['./ticker.component.css']
})
export class TickerComponent implements OnInit {

  ticker: ServerStats;

  constructor(private tickerService: TickerService) { }

  ngOnInit() {
    this.tickerService
      .get()
      .subscribe(
        response => this.onTickerRetrieved(response),
        response => this.onError(response)
      );
  }

  private onTickerRetrieved(ticker: ServerStats) {
    this.ticker = ticker;
  }

  private onError(response: any) {
    console.log(response);
  }
}
