import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';

import { Balance } from '../models/balance';
import { PaginatedResult } from '../models/paginated-result';
import { LightWalletTransaction } from '../models/light-wallet-transaction';
import { Transaction } from '../models/transaction';
import { UTXO } from '../models/utxo';
import { WrappedResult } from '../models/wrapped-result';
import { TposContract } from '../models/tpos-contract';

const httpOptions = {
  headers: new HttpHeaders({ 'Content-Type': 'application/json' })
};

@Injectable()
export class AddressesService {

  private baseUrl = environment.api.url + '/addresses';
  private baseUrlV2 = environment.api.url + '/v2/addresses';

  constructor(private http: HttpClient) { }

  get(address: string): Observable<Balance> {
    const url = `${this.baseUrl}/${address}`;
    return this.http.get<Balance>(url);
  }

  getTransactions(
    address: string,
    limit: number = 10,
    lastSeenTxid: string = '',
    order: string = 'desc'): Observable<PaginatedResult<Transaction>> {
    let url = `${this.baseUrl}/${address}/transactions?limit=${limit}&orderBy=${order}`;
    if (lastSeenTxid !== '') {
      url += `&lastSeenTxid=${lastSeenTxid}`;
    }
    return this.http.get<PaginatedResult<Transaction>>(url);
  }

  getTransactionsV2(
    address: string,
    limit: number = 10,
    lastSeenTxid: string = '',
    order: string = 'desc'): Observable<WrappedResult<LightWalletTransaction>> {

    let url = `${this.baseUrlV2}/${address}/transactions?limit=${limit}&order=${order}`;
    if (lastSeenTxid !== '') {
      url += `&lastSeenTxid=${lastSeenTxid}`;
    }

    return this.http.get<WrappedResult<LightWalletTransaction>>(url);
  }

  getUtxos(address): Observable<UTXO[]> {
    const url = `${this.baseUrl}/${address}/utxos`;
    return this.http.get<UTXO[]>(url);
  }

  getTposContracts(address): Observable<WrappedResult<TposContract>> {
    const url = `${this.baseUrl}/${address}/tposcontracts`;
    return this.http.get<WrappedResult<TposContract>>(url);
  }
}
