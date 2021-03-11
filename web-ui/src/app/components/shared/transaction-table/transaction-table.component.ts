import { Component, OnInit, OnDestroy, Input, NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { NgxSpinnerService } from "ngx-spinner";

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
  isLoading: boolean;
  percent: number;
  timer: any;

  private subscription$: Subscription;

  limit = 10;

  truncate = truncate;
  amAgo = amAgo;

  constructor(private errorService: ErrorService, private transactionsService: TransactionsService, private addressesService: AddressesService, private spinner: NgxSpinnerService) { }

  ngOnInit() {
    this.updateTransactions();
  }

  ngOnChanges(changes: any) {
    if (changes.address.currentValue != changes.address.previousValue) {
      this.transactions = [];
      this.updateTransactions();    
    }
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
    this.isLoading = true;
    this.percent = 0;
    this.timer = setInterval(() => {
      this.percent += (100 - this.percent) / 2;
    }, 500);

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
    clearInterval(this.timer);
    this.isLoading = false;
    this.percent = 0;
    // this.lastSeenTxId = this.transactions.reduce((max, block) => Math.max(block.height, max), 0);
    this.transactions = this.transactions.concat(response)/*.filter(item => item["received"] > 0) */.sort(function (a, b) {
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
