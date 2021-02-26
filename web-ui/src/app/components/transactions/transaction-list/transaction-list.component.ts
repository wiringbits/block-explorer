import { Component, OnInit } from '@angular/core';
import { Prices, ServerStats } from '../../../models/ticker';
import { TickerService } from '../../../services/ticker.service';

@Component({
    selector: 'app-transaction-list',
    templateUrl: './transaction-list.component.html',
    styleUrls: ['./transaction-list.component.css']
})
export class TransactionListComponent implements OnInit {

    ticker: ServerStats = new ServerStats();
    prices: Prices = new Prices();
    stats: ServerStats = new ServerStats();

    constructor(private tickerService: TickerService) { }

    ngOnInit() {
        this.tickerService
            .getPrices()
            .subscribe(
                response => this.prices = response,
                response => console.log(response)
            );

        this.tickerService
            .get()
            .subscribe(
                response => this.stats = response,
                response => console.log(response)
            );

        this.tickerService
            .get()
            .subscribe(
              response => this.ticker = response,
              response => console.log(response)
            );
    }
}
