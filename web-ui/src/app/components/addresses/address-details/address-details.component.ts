
import { tap } from 'rxjs/operators';
import { Component, OnInit, HostListener } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { Balance } from '../../../models/balance';
import { AddressesService } from '../../../services/addresses.service';
import { ErrorService } from '../../../services/error.service';
import { LightWalletTransaction } from '../../../models/light-wallet-transaction';

import { getNumberOfRowsForScreen } from '../../../utils';
import { addressLabels } from '../../../config';
import { TposContract } from '../../../models/tposcontract';
import { WrappedResult } from '../../../models/wrapped-result';

@Component({
  selector: 'app-address-details',
  templateUrl: './address-details.component.html',
  styleUrls: ['./address-details.component.css']
})
export class AddressDetailsComponent implements OnInit {

  address: Balance;
  addressString: string;
  addressLabel = addressLabels;
  tposContract: TposContract = null;

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
    this.addressString = this.route.snapshot.paramMap.get('id');
    this.addressesService.get(this.addressString).subscribe(
      response => this.onAddressRetrieved(response),
      response => this.onError(response)
    );
    this.addressesService.getTposContracts(this.addressString).subscribe(
      response => this.onTposContractsReceived(response),
      response => this.onError(response)
    );
  }

  private onAddressRetrieved(response: Balance) {
    this.address = response;
  }

  private onTposContractsReceived(response: WrappedResult<TposContract>) {
    this.tposContract = response.data[0];
  }

  @HostListener('window:resize', ['$event'])
  private getScreenSize(_?): number {
    return window.innerHeight;
  }

  private onError(response: any) {
    this.errorService.renderServerErrors(null, response);
  }
}
