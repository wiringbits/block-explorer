import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { Transaction, TransactionValue } from '../../../models/transaction';

import { ErrorService } from '../../../services/error.service';
import { NavigatorService } from '../../../services/navigator.service';
import { TransactionsService } from '../../../services/transactions.service';

@Component({
  selector: 'app-transaction-details',
  templateUrl: './transaction-details.component.html',
  styleUrls: ['./transaction-details.component.css']
})
export class TransactionDetailsComponent implements OnInit {

  transaction: Transaction;
  collapsedInput: TransactionValue[];
  collapsedOutput: TransactionValue[];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private navigatorService: NavigatorService,
    private transactionsService: TransactionsService,
    private errorService: ErrorService) { }

  ngOnInit() {
    this.route.params.forEach(params => this.onTransactionId(params['id']));
  }

  private onTransactionId(txid: string) {
    this.transactionsService.get(txid).subscribe(
      response => this.onTransactionRetrieved(response),
      response => this.onError(response)
    );
  }

  private onTransactionRetrieved(response: Transaction) {
    this.transaction = response;
    this.collapsedInput = this.collapseRepeatedRows(this.transaction.input);
    this.collapsedOutput = this.collapseRepeatedRows(this.transaction.output);
  }

  private onError(response: any) {
    this.errorService.renderServerErrors(null, response);
  }

  private collapseRepeatedRows(rows: TransactionValue[]): TransactionValue[] {
    const addresses = new Set(rows.map(r => r.address));
    const collapsedRows = Array.from(addresses)
      .map(address => {
        const sum = rows
          .filter(r => r.address === address)
          .map(r => r.value)
          .reduce((a, b) => a + b);

        const newValue = new TransactionValue();
        newValue.address = address;
        newValue.value = sum;

        return newValue;
      });

    return collapsedRows;
  }

  count(address: string, rows: TransactionValue[]): number {
    return rows
      .filter(r => r.address === address)
      .length;
  }

  getTotal(rows: TransactionValue[]): number {
    return rows.map((row) => row.value).reduce((a, b) => a + b);
  }

  getFee(tx: Transaction): number {
    const vout = tx.output.map(t => t.value).reduce((a, b) => a + b, 0);
    return Math.max(0, this.getVIN(tx) - vout);
  }

  private getVIN(tx): number {
    if (tx.input == null || tx.input.length === 0) {
      return 0;
    } else {
      return tx.input.map(t => t.value).reduce((a, b) => a + b, 0);
    }
  }
}
