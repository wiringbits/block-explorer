import { Component, OnInit } from '@angular/core';
import { Prices, ServerStats } from '../../../models/ticker';
import { TickerService } from '../../../services/ticker.service';
import { TransactionsService } from '../../../services/transactions.service';
import { Transaction } from '../../../models/transaction';
import { AddressesService } from '../../../services/addresses.service';
import { ErrorService } from '../../../services/error.service';

@Component({
    selector: 'app-transaction-list',
    templateUrl: './transaction-list.component.html',
    styleUrls: ['./transaction-list.component.css']
})
export class TransactionListComponent implements OnInit {

    ticker: ServerStats = new ServerStats();
    prices: Prices = new Prices();
    stats: ServerStats = new ServerStats();
    transactions: Transaction[] = [];
  address: string;
  limit = 20;

    constructor(private tickerService: TickerService, private errorService: ErrorService, private transactionsService: TransactionsService, private addressesService: AddressesService) { }

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
                response => this.ticker = this.stats = response,
                response => console.log(response)
            );

        this.updateTransactions();
    }

    private updateTransactions(isInfiniteScroll = false) {
        let lastSeenTxId = '';
        if (this.transactions.length > 0) {
          lastSeenTxId = this.transactions[this.transactions.length - 1].id;
        }
    
        if (this.address) {
          this.addressesService
            .getTransactions(this.address, this.limit, lastSeenTxId)
            .subscribe(
              response => this.onTransactionRetrieved(response.data),
              response => this.onError(response)
            );
        } else {
          this.transactionsService
            .getList(lastSeenTxId, this.limit)
            .subscribe(
              response => this.onTransactionRetrieved(response.data),
              response => this.onError(response)
            );
        }
      }
    
      private onTransactionRetrieved(response: Transaction[]) {
        // this.lastSeenTxId = this.transactions.reduce((max, block) => Math.max(block.height, max), 0);
        this.transactions = this.transactions.concat(response).filter(item => item["received"] > 0).sort(function (a, b) {
          if (a.height > b.height) return -1;
          else return 1;
        });
      }
    
      private onError(response: any) {
        this.errorService.renderServerErrors(null, response);
      }
}
