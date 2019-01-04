import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs/Observable';

import { environment } from '../../environments/environment';

import { Balance } from '../models/balance';
import { PaginatedResult } from '../models/paginated-result';
import { LightWalletTransaction } from '../models/light-wallet-transaction';
import { Transaction } from '../models/transaction';
import { WrappedResult } from '../models/wrapped-result';

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

  getTransactions(address: string, offset: number = 0, limit: number = 10, orderBy: string = ''): Observable<PaginatedResult<Transaction>> {
    const url = `${this.baseUrl}/${address}/transactions?offset=${offset}&limit=${limit}&orderBy=${orderBy}`;
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
}
