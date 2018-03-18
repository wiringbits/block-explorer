import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { TranslateService } from '@ngx-translate/core';

import { Address } from '../../models/address';

import { AddressesService } from '../../services/addresses.service';
import { ErrorService } from '../../services/error.service';
import { NavigatorService } from '../../services/navigator.service';

@Component({
  selector: 'app-address-details',
  templateUrl: './address-details.component.html',
  styleUrls: ['./address-details.component.css']
})
export class AddressDetailsComponent implements OnInit {

  address: Address;
  addressString: string;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private navigatorService: NavigatorService,
    private addressesService: AddressesService,
    private errorService: ErrorService) { }

  ngOnInit() {
    this.addressString = this.route.snapshot.paramMap.get('address');
    this.addressesService.get(this.addressString).subscribe(
      response => this.onAddressRetrieved(response),
      response => this.onError(response)
    );
  }

  private onAddressRetrieved(response: Address) {
    this.address = response;
  }

  private onError(response: any) {
    this.errorService.renderServerErrors(null, response);
  }

  getSent(address: Address): number {
    return address.received - address.balance;
  }
}
