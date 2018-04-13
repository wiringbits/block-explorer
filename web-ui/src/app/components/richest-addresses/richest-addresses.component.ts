import { Component, OnInit, OnDestroy } from '@angular/core';

import { Balance } from '../../models/balance';

import { BalancesService } from '../../services/balances.service';
import { ErrorService } from '../../services/error.service';
import { PaginatedResult } from '../../models/paginated-result';

@Component({
  selector: 'app-richest-addresses',
  templateUrl: './richest-addresses.component.html',
  styleUrls: ['./richest-addresses.component.css']
})
export class RichestAddressesComponent implements OnInit {

  balances: Balance[];

  constructor(
    private balancesService: BalancesService,
    private errorService: ErrorService) { }

  ngOnInit() {
    this.balances = [];
    this.updateBalances();
  }

  private updateBalances() {
    this.balancesService
      .getRichest()
      .subscribe(
        response => this.onBalancesRetrieved(response),
        response => this.onError(response)
      );
  }

  private onBalancesRetrieved(response: PaginatedResult<Balance>) {
    this.balances = response.data;
  }

  private onError(response: any) {
    this.errorService.renderServerErrors(null, response);
  }
}
