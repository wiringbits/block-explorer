import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';
import { NgxSpinnerService } from "ngx-spinner";

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
  @Output()
  updateTransactions: EventEmitter<any> = new EventEmitter();

  limit = 10;

  truncate = truncate;
  amAgo = amAgo;

  constructor(private errorService: ErrorService, private transactionsService: TransactionsService, private addressesService: AddressesService, private spinner: NgxSpinnerService) { }

  ngOnInit() {
    // this.updateTransactions.emit();
  }

  getTransactions() {
    console.log(this.allowInfiniteScroll);
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
