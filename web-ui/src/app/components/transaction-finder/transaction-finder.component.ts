import { Component, OnInit } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Observable } from 'rxjs/Observable';

import { ErrorService } from '../../services/error.service';
import { NavigatorService } from '../../services/navigator.service';
import { TransactionsService } from '../../services/transactions.service';

@Component({
  selector: 'app-transaction-finder',
  templateUrl: './transaction-finder.component.html',
  styleUrls: ['./transaction-finder.component.css']
})
export class TransactionFinderComponent implements OnInit {

  form: FormGroup;

  constructor(
    private formBuilder: FormBuilder,
    private navigatorService: NavigatorService,
    private transactionsService: TransactionsService,
    public errorService: ErrorService) {

    this.createForm();
  }

  ngOnInit() {
  }

  private createForm() {
    this.form = this.formBuilder.group({
      transactionId: [null, [Validators.required, Validators.pattern('^[A-Fa-f0-9]{64}$')]],
    });
  }

  onSubmit() {
    const txid = this.form.get('transactionId').value;

    // instead of redirecting, we check if the transaction is valid.
    this.transactionsService.get(txid)
      .subscribe(
        response => this.navigatorService.transactionDetails(txid),
        response => this.errorService.renderServerErrors(this.form, response)
      );
  }
}
