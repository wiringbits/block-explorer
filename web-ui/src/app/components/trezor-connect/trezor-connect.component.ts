import { Component, OnInit } from '@angular/core';
import { Observable, forkJoin, throwError } from 'rxjs';
import { map } from 'rxjs/operators';

import TrezorConnect from 'trezor-connect';

import { TrezorRepositoryService } from '../../services/trezor-repository.service';
import { AddressesService } from '../../services/addresses.service';
import { TransactionsService } from '../../services/transactions.service';
import { UTXO } from '../../models/utxo';
import { TransactionFees } from '../../models/fees';
import { TposContract } from '../../models/tpos-contract';
import { environment } from '../../../environments/environment';


import {
  TrezorAddress,
  getAddressTypeByAddress,
  getScriptTypeByAddress,
  getAddressTypeByPrefix,
  selectUtxos,
  toTrezorInput,
  toTrezorReferenceTransaction,
  convertToSatoshis,
  generatePathAddress,
  ScriptType
} from '../../trezor/trezor-helper';
import { TposContractsService } from '../../services/tposcontracts.service';
import { NotificationService } from '../../services/notification.service';
import { FormGroup, Validators, FormControl } from '@angular/forms';


@Component({
  selector: 'app-trezor-connect',
  templateUrl: './trezor-connect.component.html',
  styleUrls: ['./trezor-connect.component.css']
})
export class TrezorConnectComponent implements OnInit {

  trezorAddresses: TrezorAddress[] = [];
  verifiedTrezorAddress: string[] = [];
  utxos: UTXO[] = [];
  txid = '';
  transactionFees = TransactionFees;
  tposContractFormControl: FormGroup;
  showAllButton = true;
  tposAddress: TrezorAddress;
  tposTransaction: string;
  generatedTransaction: string;
  tposContracts: TposContract[] = [];

  constructor(
    private addressesService: AddressesService,
    private transactionsService: TransactionsService,
    private trezorRepositoryService: TrezorRepositoryService,
    private tposContractsService: TposContractsService,
    private notificationService: NotificationService
  ) { }

  ngOnInit() {
    TrezorConnect.manifest({
      email: 'trezor@wiringbits.net',
      appUrl: environment.api.url
    });

    this.trezorAddresses = this.trezorRepositoryService.get();
    this.loadUtxos(this.trezorAddresses);
    this.tposContractFormControl = this.createFormGroup();

    this.trezorAddresses.forEach(async address => {
      const contracts = await this.addressesService.getTposContracts(address.address);
      if (contracts.data) {
        contracts.data.forEach(element => {
          this.tposContracts.push(element);
        });
      }
    });
  }

  createFormGroup(): FormGroup {
    return new FormGroup({
      merchantAddress: new FormControl('', [Validators.required]),
      contractAmount: new FormControl('', [Validators.required]),
      commissionPercent: new FormControl('', [Validators.required])
    });
  }

  async verifyAddresses() {
    const bundle = this.trezorAddresses.map(address => {
      return {
        'path': address.serializedPath,
        'showOnTrezor': false
      };
    });
    const response = await TrezorConnect.getAddress({ bundle: bundle });

    if (response.success) {
      this.showAllButton = false;
      response.payload.forEach(element => {
        this.verifiedTrezorAddress.push(element.address);
      });
    } else {
      this.notificationService.warning(response.payload.error);
    }
  }

  getAvailableSatoshis() {
    return this.utxos.map(u => u.satoshis).reduce((a, b) => a + b, 0);
  }

  private loadUtxos(addresses: TrezorAddress[]) {
    const observables = addresses
      .map(trezorAddress => trezorAddress.address)
      .map(address => this.addressesService.getUtxos(address));
    forkJoin(observables).subscribe(
      allUtxos => this.utxos = allUtxos.reduce((utxosA, utxosB) => {
        return utxosA.concat(utxosB);
      }, [])
    );
  }

  isTrezorAddressVerified(address: string): boolean {
    return this.verifiedTrezorAddress.includes(address);
  }

  private onTrezorAddressGenerated(trezorAddress: TrezorAddress): TrezorAddress {
    if (typeof (trezorAddress.address) === 'undefined') {
      return;
    }
    this.trezorRepositoryService.add(trezorAddress);
    this.verifiedTrezorAddress.push(trezorAddress.address);
    this.addressesService
      .getUtxos(trezorAddress.address)
      .subscribe(utxos => this.utxos = this.utxos.concat(utxos));
    return trezorAddress;
  }

  generateNextAddress(addressType: number): Promise<any> {
    const newIdByType = this.trezorAddresses
      .filter(item => getAddressTypeByAddress(item.address) === getAddressTypeByPrefix(addressType))
      .length;

    const path = generatePathAddress(addressType, newIdByType);
    return this.getTrezorAddress(path)
      .then(this.onTrezorAddressGenerated.bind(this));
  }

  signTransaction(destinationAddress: string, xsns: number, fee: number) {
    this.txid = '';
    const satoshis = convertToSatoshis(xsns);
    const generatedInputs = this.generateInputs(+satoshis + +fee);
    if (generatedInputs.error) {
      this.notificationService.error(generatedInputs.error);
      return;
    }

    const outputs = [{
      address: destinationAddress,
      amount: satoshis.toString(),
      script_type: getScriptTypeByAddress(destinationAddress, ScriptType.OUTPUT)
    }];

    if (generatedInputs.change > 0) {
      outputs.push({
        address: generatedInputs.addressToChange,
        amount: generatedInputs.change.toString(),
        script_type: getScriptTypeByAddress(generatedInputs.addressToChange, ScriptType.OUTPUT)
      });
    }

    const hashTransactions = generatedInputs.inputs.map((x) => {
      return x.prev_hash;
    });

    this.getRefTransactions(hashTransactions).subscribe(txs => {
      this.signTrezorTransaction({
        inputs: generatedInputs.inputs,
        outputs: outputs,
        refTxs: txs,
        coin: 'Stakenet'
      }).then((result) => {
        if (result.payload.error) {
          this.notificationService.error(result);
        } else {
          this.pushTransaction(result.payload.serializedTx);
        }
      });
    });
  }


  async sendTPOS() {
    if (!this.validateTposForm()) {
      return;
    }

    const contractForm = this.tposContractFormControl.getRawValue();
    const fee = TransactionFees[0].amount; // 1000 satoshis
    const contractAmount = contractForm.contractAmount;
    const collateral = convertToSatoshis(1); // The collateral is one xsn
    const generatedInputs = this.generateInputs(collateral + fee);

    if (generatedInputs.error) {
      this.notificationService.error(generatedInputs.error);
      return;
    }

    // A new legacy address is required for each new tpos contract.
    const tposAddress = await this.generateNextAddress(44);
    if (tposAddress === undefined) {
      this.notificationService.error('An error occurred while generating the tpos address');
      return;
    }
    this.notificationService.info('TPoS Address:' + tposAddress.address);
    if (tposAddress.payload) {
      this.notificationService.error(tposAddress.payload.error);
      return;
    } else {
      this.tposAddress = tposAddress;
    }

    const firstOutPoint = generatedInputs.inputs[0].prev_hash + ':' + generatedInputs.inputs[0].prev_index;
    const messageSigned = await TrezorConnect.signMessage({
      path: tposAddress.path,
      message: firstOutPoint
    });

    if (messageSigned.payload.error) {
      this.notificationService.error(messageSigned.payload.error);
      return;
    }

    // Add the outputs to the transaction
    const outputs: any[] = [{
      address: tposAddress.address,
      amount: collateral.toString(),
      script_type: getScriptTypeByAddress(tposAddress.address, ScriptType.OUTPUT)
    }];

    if (generatedInputs.change > 0) {
      outputs.push({
        address: generatedInputs.addressToChange,
        amount: generatedInputs.change.toString(),
        script_type: getScriptTypeByAddress(generatedInputs.addressToChange, ScriptType.OUTPUT)
      });
    }

    // create the opreturn value
    const merchantAddress = contractForm.merchantAddress;
    const comissionPercent = contractForm.commissionPercent;
    const hexSignature = this.base64ToHex(messageSigned.payload.signature);
    const contract = await this.tposContractsService.encodeTPOS(tposAddress.address, merchantAddress, comissionPercent, hexSignature);

    const tposContract = {
      amount: '0',
      op_return_data: contract.tposContractEncoded,
      script_type: 'PAYTOOPRETURN',
    };

    outputs.push(tposContract);

    const hashTransactions = generatedInputs.inputs.map((x) => {
      return x.prev_hash;
    });

    // push the transaction to explorer
    this.getRefTransactions(hashTransactions).subscribe(async txs => {
      const tposTransaction = {
        inputs: generatedInputs.inputs,
        outputs: outputs,
        refTxs: txs,
        coin: 'Stakenet'
      };

      const trezorResult = await this.signTrezorTransaction(tposTransaction);
      if (trezorResult.payload.error) {
        this.notificationService.error(trezorResult.payload.error);
      } else {
        const txid = await this.pushTransaction(trezorResult.payload.serializedTx);
        this.tposTransaction = txid;
        this.tposContractFormControl = this.createFormGroup();
        // remove the utxos spent in the tpos contract to create the second transaction.
        generatedInputs.inputs.forEach(input => {
          this.removeUTXO(input.prev_hash, input.prev_index);
        });
        this.signTransaction(tposAddress.address, contractAmount, fee);
      }
    });

  }

  verifyAddress(trezorAddress: TrezorAddress): void {
    this.getTrezorAddress(trezorAddress.serializedPath).then(response => {
      if (response.address === trezorAddress.address) {
        this.verifiedTrezorAddress.push(trezorAddress.address);
      } else {
        this.notificationService.warning('Fail to verify');
      }
    });
  }

  resetWallet() {
    this.trezorRepositoryService.clear();
    this.trezorAddresses = [];
    this.verifiedTrezorAddress = [];
    this.utxos = [];
    this.txid = '';
  }

  private getRefTransactions(txids: string[]): Observable<any[]> {
    const observables = txids.map(txid => this.transactionsService.getRaw(txid));
    const result = forkJoin(observables).pipe(
      map(rawTxs => rawTxs.map(toTrezorReferenceTransaction))
    );

    return result;
  }

  private async getTrezorAddress(path: string): Promise<TrezorAddress> {
    const result = await TrezorConnect.getAddress({ path: path, coin: 'Stakenet', showOnTrezor: false });
    return result.payload;
  }

  private async signTrezorTransaction(params) {
    const result = await TrezorConnect.signTransaction(params);
    return result;
  }

  private async pushTransaction(hex: string): Promise<any> {
    const response = await this.transactionsService.push(hex);
    this.notificationService.info('Transaction id: ' + response.txid);
    return response.txid;
  }

  private generateInputs(satoshis: number) {
    this.loadUtxos(this.trezorAddresses);
    const selectedUtxos = selectUtxos(this.utxos, satoshis);
    if (selectedUtxos.utxos.length === 0) {
      return {
        error: 'No utoxs'
      };
    }
    const change = selectedUtxos.total - satoshis;
    const inputs = selectedUtxos.utxos.map(utxo => toTrezorInput(this.trezorAddresses, utxo));
    const addressToChange = selectedUtxos.utxos[selectedUtxos.utxos.length - 1].address;

    return {
      inputs: inputs,
      addressToChange: addressToChange,
      change: change
    };
  }

  getLegacyAddresses(): TrezorAddress[] {
    return this.trezorAddresses.filter(value =>
      getAddressTypeByAddress(value.address) === TrezorAddress.LEGACY
    );
  }

  precise(elem) {
    elem.value = Number(elem.value).toFixed(8);
  }

  satoshiToXsn(amount: number) {
    return amount / 100000000;
  }

  refresh() { }

  private base64ToHex(str: string) {
    const raw = atob(str);
    let result = '';
    for (let i = 0; i < raw.length; i++) {
      const hex = raw.charCodeAt(i).toString(16);
      result += (hex.length === 2 ? hex : '0' + hex);
    }
    return result.toUpperCase();
  }

  private validateTposForm(): Boolean {
    if (this.tposContractFormControl.invalid) {
      return false;
    }

    const contract = this.tposContractFormControl.getRawValue();

    if (!this.validMerchantAddress(contract.merchantAddress) || !this.validaCommissionPercent(contract.commissionPercent)) {
      return false;
    }

    return true;

  }

  private validMerchantAddress(merchantAddress: string): Boolean {

    if (getAddressTypeByAddress(merchantAddress) === TrezorAddress.P2SHSEGWIT) {
      this.notificationService.warning('Merchant address cannot be of type P2SHSEGWIT');
      return false;
    }
    return true;
  }

  private validaCommissionPercent(commission: number): Boolean {
    if (!Number.isInteger(commission)) {
      this.notificationService.error('Commission must be an integer value');
      return false;
    }

    if (commission < 1 || commission > 99) {
      this.notificationService.error('Commission must be a value between 1 and 99');
      return false;
    }

    return true;
  }

  private removeUTXO(transactionId: string, index: number) {
    this.utxos = this.utxos.filter(element => !(element.txid === transactionId && element.outputIndex === index));
  }

}
