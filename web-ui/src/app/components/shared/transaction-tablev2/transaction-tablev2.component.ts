import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';

import { truncate, amAgo } from '../../../utils';
import { TransactionsService } from '../../../services/transactions.service';
import { Transaction } from '../../../models/transaction';
import { LightWalletTransaction } from '../../../models/light-wallet-transaction';
import { AddressesService } from '../../../services/addresses.service';
import { ErrorService } from '../../../services/error.service';

@Component({
  selector: 'app-transaction-tablev2',
  templateUrl: './transaction-tablev2.component.html',
  styleUrls: ['./transaction-tablev2.component.css']
})
export class TransactionTablev2Component implements OnInit {

  @Input()
  hideBlockHash: boolean;
  @Input()
  address: string;
  @Input()
  allowInfiniteScroll: boolean;
  @Input()
  transactions: Array<LightWalletTransaction>;
  @Input()
  isLoading: boolean = false;
  @Input()
  loadingType: number = 1;
  @Output()
  updateTransactions: EventEmitter<any> = new EventEmitter();

  emptyArray = new Array(10);

  public lottieConfig: Object;

  limit = 10;

  truncate = truncate;
  amAgo = amAgo;

  constructor(private errorService: ErrorService, private transactionsService: TransactionsService, private addressesService: AddressesService) {
    this.lottieConfig = {
      path: 'assets/loader.json',
      renderer: 'canvas',
      autoplay: true,
      loop: true
    };
    this.emptyArray = Array(10).fill(4);
  }

  ngOnInit() {
    // this.updateTransactions.emit();
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

  getFee(tx: LightWalletTransaction) {
    const received = tx
      .outputs
      .map(output => output.value)
      .reduce((a, b) => a + b, 0);
    
    return received;
  }

  getAmount(tx: LightWalletTransaction): string {
    const spent = tx
      .inputs
      .map(input => input.value)
      .reduce((a, b) => a + b, 0);

    const received = tx
      .outputs
      .map(output => output.value)
      .reduce((a, b) => a + b, 0);

    const diff = Math.abs(received - spent);
    if (received >= spent) {
      return '+' + diff;
    } else {
      return '-' + diff;
    }
  }
}
