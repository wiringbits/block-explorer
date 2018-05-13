import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { TranslateService } from '@ngx-translate/core';

import { Transaction } from '../../models/transaction';

import { ErrorService } from '../../services/error.service';
import { NavigatorService } from '../../services/navigator.service';
import { TransactionsService } from '../../services/transactions.service';

@Component({
  selector: 'app-transaction-raw',
  templateUrl: './transaction-raw.component.html',
  styleUrls: ['./transaction-raw.component.css']
})
export class TransactionRawComponent implements OnInit {

  transaction: any;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private navigatorService: NavigatorService,
    private transactionsService: TransactionsService,
    private errorService: ErrorService) { }

  ngOnInit() {
    this.route.params.forEach(params => this.onTransactionId(params['txid']));
  }

  private onTransactionId(txid: string) {
    this.transactionsService.getRaw(txid).subscribe(
      response => this.onTransactionRetrieved(response),
      response => this.onError(response)
    );
  }

  private onTransactionRetrieved(response: any) {
    this.transaction = response;
  }

  private onError(response: any) {
    this.errorService.renderServerErrors(null, response);
  }
}
