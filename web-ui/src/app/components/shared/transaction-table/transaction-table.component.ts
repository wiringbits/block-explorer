import { Component, OnInit, OnDestroy, Input, EventEmitter, Output } from '@angular/core';

import { Subscription } from 'rxjs';
import { truncate, amAgo } from '../../../utils';
import { Transaction } from '../../../models/transaction';

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
  transactions: Transaction[] = [];
  @Output() updateTransactions: any = new EventEmitter();

  private subscription$: Subscription;

  limit = 20;

  truncate = truncate;
  amAgo = amAgo;

  constructor() { }

  ngOnInit() {
  }

  ngOnDestroy() {
    if (this.subscription$ != null) {
      this.subscription$.unsubscribe();
    }
  }

  getTransactions(isInfiniteScroll = false) {
    this.updateTransactions.emit(isInfiniteScroll);
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
