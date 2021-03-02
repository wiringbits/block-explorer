import { Component, OnInit, OnDestroy, Input, EventEmitter, Output } from '@angular/core';

import { Subscription } from 'rxjs';
import { truncate, amAgo } from '../../../utils';
import { TransactionsService } from '../../../services/transactions.service';
import { Transaction } from '../../../models/transaction';
import { AddressesService } from '../../../services/addresses.service';
import { ErrorService } from '../../../services/error.service';

@Component({
  selector: 'app-transaction-table',
  templateUrl: './transaction-table.component.html',
  styleUrls: ['./transaction-table.component.css']
})
export class TransactionTableComponent implements OnInit, OnDestroy {

  @Input()
  hideBlockHash: boolean;
  @Input()
  address: string;
  @Input()
  allowInfiniteScroll: boolean;
  transactions: Transaction[] = [];

  private subscription$: Subscription;

  limit = 20;

  truncate = truncate;
  amAgo = amAgo;

  constructor(private errorService: ErrorService, private transactionsService: TransactionsService, private addressesService: AddressesService) { }

  ngOnInit() {
    this.updateTransactions();
  }

  ngOnDestroy() {
    if (this.subscription$ != null) {
      this.subscription$.unsubscribe();
    }
  }

  getTransactions() {
    if (this.allowInfiniteScroll == false) {
      return;
    }
    this.updateTransactions();
  }

  private updateTransactions() {
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

  getResult(item: Transaction) {
    if (item.height) {
      return true;
    }
    return false;
  }

  getAmount(item: Transaction) {
    return item.received;
  }

  getFee(item: Transaction) {
    return Math.max(item.sent - item.received, 0);
  }
}
