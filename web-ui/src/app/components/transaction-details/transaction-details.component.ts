import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { TranslateService } from '@ngx-translate/core';

import { ErrorService } from '../../services/error.service';
import { NavigatorService } from '../../services/navigator.service';
import { TransactionsService } from '../../services/transactions.service';

@Component({
  selector: 'app-transaction-details',
  templateUrl: './transaction-details.component.html',
  styleUrls: ['./transaction-details.component.css']
})
export class TransactionDetailsComponent implements OnInit {

  transaction: object;

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

  private onTransactionRetrieved(response: any) {
    console.log(response);
    this.transaction = response;
  }

  private onError(response: any) {
    this.errorService.renderServerErrors(null, response);
  }

  inputAddresses(transaction): string[] {
    // TODO: complete
    // transaction.vin.keys
    return [];
  }

  // TODO: verify correctness
  outputAddresses(transaction): string[] {
    const keys: number[] = Array.from(transaction.vout.keys());
    const nestedAddresses = keys.map(k => transaction.vout[k].scriptPubKey.addresses);
    return this.flatten(nestedAddresses);
  }

  // TODO: move function to another package
  private flatten = function (arr, result = []) {
    for (let i = 0, length = arr.length; i < length; i++) {
      const value = arr[i];
      if (Array.isArray(value)) {
        this.flatten(value, result);
      } else {
        result.push(value);
      }
    }
    return result;
  };
}
