import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { TranslateService } from '@ngx-translate/core';

import { Transaction } from '../../models/transaction';

import { ErrorService } from '../../services/error.service';
import { NavigatorService } from '../../services/navigator.service';
import { TransactionsService } from '../../services/transactions.service';

@Component({
  selector: 'app-transaction-details',
  templateUrl: './transaction-details.component.html',
  styleUrls: ['./transaction-details.component.css']
})
export class TransactionDetailsComponent implements OnInit {

  transaction: Transaction;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private navigatorService: NavigatorService,
    private transactionsService: TransactionsService,
    private errorService: ErrorService) { }

  ngOnInit() {
    const txid = this.route.snapshot.paramMap.get('txid');
    this.transactionsService.get(txid).subscribe(
      response => this.onTransactionRetrieved(response),
      response => this.onError(response)
    );
  }

  private onTransactionRetrieved(response: Transaction) {
    this.transaction = response;
  }

  private onError(response: any) {
    this.errorService.renderServerErrors(null, response);
  }

  getFee(tx: Transaction): number {
    const vout = tx.output.map(t => t.value).reduce((a, b) => a + b);
    return Math.max(0, this.getVIN(tx) - vout);
  }

  private getVIN(tx): number {
    if (tx.input == null) {
      return 0;
    } else {
      return tx.input.value;
    }
  }
}
