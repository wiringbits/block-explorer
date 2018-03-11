import { Component, OnInit } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Observable } from 'rxjs/Observable';

@Component({
  selector: 'app-transaction-finder',
  templateUrl: './transaction-finder.component.html',
  styleUrls: ['./transaction-finder.component.css']
})
export class TransactionFinderComponent implements OnInit {

  form: FormGroup;

  constructor(private formBuilder: FormBuilder) {
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
    console.log('submit now!');
  }
}
