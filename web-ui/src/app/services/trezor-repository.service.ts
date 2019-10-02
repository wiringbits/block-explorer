import { Injectable } from '@angular/core';
import { TrezorAddress } from '../trezor/trezor-helper';

@Injectable()
export class TrezorRepositoryService {

  private trezorAddresses: TrezorAddress[] = [];

  constructor() {
    const lStorage = localStorage.getItem('trezorAddresses');
    this.trezorAddresses = lStorage === null ? [] : JSON.parse(lStorage);
  }

  add(trezorAddress: TrezorAddress): void {
    this.trezorAddresses.push(trezorAddress);
    localStorage.setItem('trezorAddresses', JSON.stringify(this.trezorAddresses));
  }

  get(): TrezorAddress[] {
    return this.trezorAddresses;
  }

  clear() {
    localStorage.removeItem('trezorAddresses');
  }
}
