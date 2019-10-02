import { Component, OnInit } from '@angular/core';
import { Observable, forkJoin } from 'rxjs';
import { map } from 'rxjs/operators';

import TrezorConnect from 'trezor-connect';

import { TrezorRepositoryService } from '../../services/trezor-repository.service';
import { AddressesService } from '../../services/addresses.service';
import { TransactionsService } from '../../services/transactions.service';
import { UTXO } from '../../models/utxo';
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

  constructor(
    private addressesService: AddressesService,
    private transactionsService: TransactionsService,
    private trezorRepositoryService: TrezorRepositoryService
  ) { }

  ngOnInit() {
    TrezorConnect.manifest({
      email: 'trezor@wiringbits.net',
      appUrl: 'https://xsnexplorer.io'
    });
    this.trezorAddresses = this.trezorRepositoryService.get();
    this.loadUtxos(this.trezorAddresses);
  }

  getAvailableSatoshis() {
    return this.utxos.map(u => u.satoshis).reduce((a, b) => a + b, 0);
  }

  private loadUtxos(addresses: TrezorAddress[]) {
    const observables = addresses
      .map( trezorAddress => trezorAddress.address )
      .map( address => this.addressesService.getUtxos(address) );
    forkJoin(observables).subscribe(
      allUtxos => this.utxos = allUtxos.reduce((utxosA, utxosB) => {
        return utxosA.concat(utxosB);
      }, [])
    );
  }

  isTrezorAddressVerified(address: string): boolean {
    return this.verifiedTrezorAddress.includes(address);
  }

  private onTrezorAddressGenerated(trezorAddress: TrezorAddress)     {
    if (typeof(trezorAddress.address) === 'undefined') {
      return;
    }
    this.trezorRepositoryService.add(trezorAddress);
    this.verifiedTrezorAddress.push(trezorAddress.address);
    this.addressesService
      .getUtxos(trezorAddress.address)
      .subscribe( utxos => this.utxos = this.utxos.concat(utxos) );
  }

  generateNextAddress(addressType: number): void {
    const newIdByType = this.trezorAddresses
      .filter(item => getAddressTypeByAddress(item.address) === getAddressTypeByPrefix(addressType))
      .length;
    const path = generatePathAddress(addressType, newIdByType);
    this.getTrezorAddress(path)
      .then(this.onTrezorAddressGenerated.bind(this));
  }

  signTransaction(destinationAddress: string, xsns: number, fee: number) {
    this.txid = '';
    const satoshis = convertToSatoshis(xsns);
    const generatedInputs = this.generateInputs(+satoshis + +fee);
    if (generatedInputs.error) {
      console.log(generatedInputs.error);
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

    this.getRefTransactions(hashTransactions).subscribe( txs => {
      this.signTrezorTransaction({
        inputs: generatedInputs.inputs,
        outputs: outputs,
        refTxs: txs,
        coin: 'Stakenet'
      }).then((result) => {
        if (result.payload.error) {
          console.log(result);
        } else {
          this.pushTransaction(result.payload.serializedTx);
        }
      });
    });
  }

  sendTPOS() {
    console.log('sending tpos');
  }

  verifyAddress(trezorAddress: TrezorAddress): void {
    this.getTrezorAddress(trezorAddress.serializedPath).then( response => {
      if (response.address === trezorAddress.address) {
        this.verifiedTrezorAddress.push(trezorAddress.address);
      } else {
        console.log('Fail to verify');
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
    const observables = txids.map( txid => this.transactionsService.getRaw(txid) );
    const result = forkJoin(observables).pipe(
      map( rawTxs => rawTxs.map(toTrezorReferenceTransaction) )
    );

    return result;
  }

  private async getTrezorAddress(path: string): Promise<TrezorAddress> {
    const result = await TrezorConnect.getAddress({path: path, coin: 'Stakenet', showOnTrezor: false});
    return result.payload;
  }

  private async signTrezorTransaction(params) {
    console.log('sending to sign');
    console.log(params);
    // return new Promise((succ, fail) => {
    //   succ('Testing');
    // });
    const result = await TrezorConnect.signTransaction(params);
    return result;
  }

  private pushTransaction(hex: string) {
    console.log('push hex');
    console.log(hex);
    this.transactionsService
      .push(hex)
      .subscribe(response => {
        console.log(response)
        this.txid = response.txid;
      });
  }

  private generateInputs(satoshis: number) {
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

  precise(elem) {
    elem.value = Number(elem.value).toFixed(8);
  }

  satoshiToXsn(amount: number) {
    return amount / 100000000;
  }

  refresh() { }
}
