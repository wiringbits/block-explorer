
import {tap} from 'rxjs/operators';
import { Component, OnInit, HostListener } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { Balance } from '../../models/balance';
import { AddressesService } from '../../services/addresses.service';
import { ErrorService } from '../../services/error.service';
import { LightWalletTransaction } from '../..//models/light-wallet-transaction';

import { getNumberOfRowsForScreen } from '../../utils';

@Component({
  selector: 'app-address-details',
  templateUrl: './address-details.component.html',
  styleUrls: ['./address-details.component.css']
})
export class AddressDetailsComponent implements OnInit {

  address: Balance;
  addressString: string;

  // pagination
  limit = 30;
  items: LightWalletTransaction[] = [];

  constructor(
    private route: ActivatedRoute,
    private addressesService: AddressesService,
    private errorService: ErrorService) { }

  ngOnInit() {
    const height = this.getScreenSize();
    this.limit = getNumberOfRowsForScreen(height);
    this.addressString = this.route.snapshot.paramMap.get('address');
    this.addressesService.get(this.addressString).subscribe(
      response => this.onAddressRetrieved(response),
      response => this.onError(response)
    );
  }

  private onAddressRetrieved(response: Balance) {
    this.address = response;
    this.load();
  }

  load() {
    const order = 'desc';
    let lastSeenTxid = '';
    if (this.items.length > 0) {
      lastSeenTxid = this.items[this.items.length - 1].id;
    }

    this.addressesService
      .getTransactionsV2(this.addressString, this.limit, lastSeenTxid, order).pipe(
      tap(response => this.items.push(...response.data)))
      .subscribe();
  }

  @HostListener('window:resize', ['$event'])
  private getScreenSize(_?): number {
    return window.innerHeight;
  }

  private onError(response: any) {
    this.errorService.renderServerErrors(null, response);
  }

  renderValue(tx: LightWalletTransaction): string {
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
