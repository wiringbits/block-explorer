import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';

import { truncate, amAgo } from '../../../utils';
import { Transaction } from '../../../models/transaction';

@Component({
  selector: 'app-transaction-table',
  templateUrl: './transaction-table.component.html',
  styleUrls: ['./transaction-table.component.css']
})
export class TransactionTableComponent implements OnInit {

  @Input()
  hideBlockHash: boolean;
  @Input()
  address: string;
  @Input()
  allowInfiniteScroll: boolean;
  @Input()
  transactions: Array<Transaction>;
  @Input()
  isLoading: boolean = false;
  @Input()
  loadingType: number = 1;
  @Output()
  updateTransactions: EventEmitter<any> = new EventEmitter();

  public lottieConfig: Object;

  limit = 10;
  emptyArray = new Array(10);

  truncate = truncate;
  amAgo = amAgo;

  constructor() {
    this.lottieConfig = {
      path: 'assets/loader.json',
      renderer: 'canvas',
      autoplay: true,
      loop: true
    };
    this.emptyArray = Array(10).fill(4);
  }

  ngOnInit() {
  }

  getTransactions() {
    if (this.allowInfiniteScroll == false) {
      return;
    }
    this.updateTransactions.emit();
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
