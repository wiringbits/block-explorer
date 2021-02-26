import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';

import { TranslateService } from '@ngx-translate/core';

import { ErrorService } from '../../../services/error.service';
import { NavigatorService } from '../../../services/navigator.service';
import { AddressesService } from '../../../services/addresses.service';
import { BlocksService } from '../../../services/blocks.service';
import { TransactionsService } from '../../../services/transactions.service';
import { MasternodesService } from '../../../services/masternodes.service';

const BLOCK_REGEX = '^[A-Fa-f0-9]{64}$';
const BLOCK_NUMBER_REGEX = '^(\\d{1,10})$';
const ADDRESS_REGEX = '(^[a-zA-Z0-9]{34}$)|(^[a-zA-Z0-9]{42}$)';
const IP_ADDRESS_REGEX = '^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$';

@Component({
  selector: 'app-finder',
  templateUrl: './finder.component.html',
  styleUrls: ['./finder.component.css']
})
export class FinderComponent implements OnInit {

  form: FormGroup;

  constructor(
    private formBuilder: FormBuilder,
    private navigatorService: NavigatorService,
    private addressesService: AddressesService,
    private blocksService: BlocksService,
    private masternodesService: MasternodesService,
    private transactionsService: TransactionsService,
    private translateService: TranslateService,
    public errorService: ErrorService) {

    this.createForm();
  }

  ngOnInit() {
  }

  private createForm() {
    const pattern = `(${ADDRESS_REGEX})|(${BLOCK_REGEX})|(${IP_ADDRESS_REGEX})|(${BLOCK_NUMBER_REGEX})`;
    this.form = this.formBuilder.group({
      searchField: [null, [/*Validators.required,*/ Validators.pattern(pattern)]],
    });
  }

  onSubmit() {
    if (!this.form.valid) {
      return;
    }

    const searchField = this.form.get('searchField').value;

    if (new RegExp(ADDRESS_REGEX).test(searchField)) {
      // address
      this.addressesService.get(searchField)
        .subscribe(
          response => this.navigatorService.addressDetails(searchField),
          response => this.onNothingFound()
        );
    } else if (new RegExp(BLOCK_REGEX).test(searchField)) {
      // block or transaction
      this.transactionsService.get(searchField)
        .subscribe(
          response => this.navigatorService.transactionDetails(searchField),
          response => this.lookForBlock(searchField)
        );
    } else if (new RegExp(IP_ADDRESS_REGEX).test(searchField)) {
      // masternode
      this.masternodesService.getByIP(searchField)
        .subscribe(
          response => this.navigatorService.masternodeDetails(searchField),
          response => this.onNothingFound()
        );
    } else if (new RegExp(BLOCK_NUMBER_REGEX).test(searchField)) {
      this.lookForBlock(searchField);
    }
  }

  private lookForBlock(query: string) {
    this.blocksService.get(query)
      .subscribe(
        response => this.navigatorService.blockDetails(query),
        response => this.onNothingFound()
      );
  }

  private onNothingFound() {
    this.translateService.get('error.nothingFound')
      .subscribe(msg => this.errorService.setFieldError(this.form, 'searchField', msg));
  }
}
