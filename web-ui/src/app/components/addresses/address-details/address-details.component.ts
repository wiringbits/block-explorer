
import { Component, OnInit, HostListener } from '@angular/core';
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';
import { Balance } from '../../../models/balance';
import { AddressesService } from '../../../services/addresses.service';
import { ErrorService } from '../../../services/error.service';
import { LightWalletTransaction } from '../../../models/light-wallet-transaction';

import { addressLabels } from '../../../config';
import { TposContract } from '../../../models/tposcontract';
import { WrappedResult } from '../../../models/wrapped-result';

import { Subscription } from 'rxjs';

@Component({
  selector: 'app-address-details',
  templateUrl: './address-details.component.html',
  styleUrls: ['./address-details.component.css']
})
export class AddressDetailsComponent implements OnInit {

  address: Balance;
  addressString: string;
  addressLabel = addressLabels;
  tposContracts: Array<TposContract>;
  selectedTpos: number;
  isLoading: boolean;
  loadingType = 2;
  addressLoaded: boolean;
  interval: number = null;

  limit = 10;
  transactions: LightWalletTransaction[] = [];
  items: LightWalletTransaction[] = [];

  private subscription$: Subscription;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private addressesService: AddressesService,
    private errorService: ErrorService) { 
      this.selectedTpos = 0;
      this.addressString = null;
    }

  ngOnInit() {
    const height = this.getScreenSize();
    this.router.events.subscribe(event => {
      if (event instanceof NavigationEnd) {
        this.reload();
      }
    });
    this.reload();
  }
  
  ngOnChanges(changes: any) {
    if (changes.address.currentValue != changes.address.previousValue) {
      this.transactions = [];
      this.updateTransactions();
    }
  }

  ngOnDestroy() {
    if (this.subscription$ != null) {
      this.subscription$.unsubscribe();
    }

    this.interval = null;
  }

  private updateTransactions() {
    let lastSeenTxId = '';
    if (this.transactions.length > 0) {
      lastSeenTxId = this.transactions[this.transactions.length - 1].id;
    }
    this.isLoading = true;

    if (this.addressString) {
      this.addressesService
        .getTransactionsV2(this.addressString, this.limit, lastSeenTxId)
        .subscribe(
          response => this.onTransactionRetrieved(response.data),
          response => this.onError(response)
        );
    } else {
    }
  }

  private onTransactionRetrieved(response: LightWalletTransaction[]) {
    this.isLoading = false;
    this.loadingType = 1;
    this.transactions = this.transactions.concat(response);
  }

  reload() {
    this.addressLoaded = false;
    this.selectedTpos = 0;
    this.transactions = [];
    this.addressString = this.route.snapshot.paramMap.get('id');
    this.addressesService.get(this.addressString).subscribe(
      response => this.onAddressRetrieved(response),
      response => this.onError(response)
    );
    this.addressesService.getTposContracts(this.addressString).subscribe(
      response => this.onTposContractsReceived(response),
      response => this.onError(response)
    );
    this.updateTransactions();
  }

  private onAddressRetrieved(response: Balance) {
    console.log(response);
    this.address = response;
    this.addressLoaded = true;
  }

  private onTposContractsReceived(response: WrappedResult<TposContract>) {
    this.tposContracts = response.data;
  }

  selectTpos(index) {
    this.selectedTpos = index;
  }

  @HostListener('window:resize', ['$event'])
  private getScreenSize(_?): number {
    return window.innerHeight;
  }

  private onError(response: any) {
    this.errorService.renderServerErrors(null, response);
  }
}
