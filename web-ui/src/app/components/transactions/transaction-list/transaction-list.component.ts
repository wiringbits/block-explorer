import { Component, OnInit } from '@angular/core';
import { TransactionsService } from '../../../services/transactions.service';
import { ErrorService } from '../../../services/error.service';
import { Prices, ServerStats } from '../../../models/ticker';
import { TickerService } from '../../../services/ticker.service';
import { Transaction } from '../../../models/transaction';

@Component({
  selector: 'app-transaction-list',
  templateUrl: './transaction-list.component.html',
  styleUrls: ['./transaction-list.component.css']
})
export class TransactionListComponent implements OnInit {

  ticker: ServerStats = new ServerStats();
  prices: Prices = new Prices();
  stats: ServerStats = new ServerStats();
  address: string;
  limit = 20;
  isLoading: boolean;
  transactions: Transaction[] = [];
  loadingType = 2;

  constructor(private tickerService: TickerService,
    private transactionsService: TransactionsService, private errorService: ErrorService) { }

  ngOnInit() {
    this.updateTransactions();
    this.tickerService
      .getPrices()
      .subscribe(
        response => this.prices = response,
        response => console.log(response)
      );

    this.tickerService
      .get()
      .subscribe(
        response => this.ticker = this.stats = response,
        response => console.log(response)
      );
  }

  updateTransactions() {
    let lastSeenTxId = '';
    if (this.transactions.length > 0) {
      lastSeenTxId = this.transactions[this.transactions.length - 1].id;
    }
    this.isLoading = true;
    this.transactionsService
      .getList(lastSeenTxId, this.limit)
      .subscribe(
        response => this.onTransactionRetrieved(response.data),
        response => this.onError(response)
      );
  }

  private onTransactionRetrieved(response: Transaction[]) {
    this.isLoading = false;
    this.loadingType = 1;
    // this.lastSeenTxId = this.transactions.reduce((max, block) => Math.max(block.height, max), 0);
    this.transactions = this.transactions.concat(response)/*.filter(item => item["received"] > 0)*/.sort(function (a, b) {
      if (a.height > b.height) return -1;
      else return 1;
    });
  }

  private onError(response: any) {
    this.errorService.renderServerErrors(null, response);
  }
}
