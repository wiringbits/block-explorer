import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { Observable } from 'rxjs/Observable';

import { Balance } from '../../models/balance';
import { Transaction } from '../../models/transaction';

import { AddressesService } from '../../services/addresses.service';
import { ErrorService } from '../../services/error.service';

@Component({
  selector: 'app-address-details',
  templateUrl: './address-details.component.html',
  styleUrls: ['./address-details.component.css']
})
export class AddressDetailsComponent implements OnInit {

  address: Balance;
  addressString: string;

  // pagination
  total = 0;
  currentPage = 1;
  pageSize = 10;
  asyncItems: Observable<Transaction[]>;

  constructor(
    private route: ActivatedRoute,
    private addressesService: AddressesService,
    private errorService: ErrorService) { }

  ngOnInit() {
    this.addressString = this.route.snapshot.paramMap.get('address');
    this.addressesService.get(this.addressString).subscribe(
      response => this.onAddressRetrieved(response),
      response => this.onError(response)
    );
    this.getPage(this.currentPage);
  }

  private onAddressRetrieved(response: Balance) {
    this.address = response;
  }

  getPage(page: number) {
    const offset = (page - 1) * this.pageSize;
    const limit = this.pageSize;
    const order = 'time:desc';

    this.asyncItems = this.addressesService
      .getTransactions(this.addressString, offset, limit, order)
      .do(response => this.total = response.total)
      .do(response => this.currentPage = 1 + (response.offset / this.pageSize))
      .map(response => response.data);
  }

  private onError(response: any) {
    this.errorService.renderServerErrors(null, response);
  }
}
