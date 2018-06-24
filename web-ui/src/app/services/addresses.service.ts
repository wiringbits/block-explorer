import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs/Observable';

import { environment } from '../../environments/environment';

import { Balance } from '../models/balance';
import { PaginatedResult } from '../models/paginated-result';
import { Transaction } from '../models/transaction';

const httpOptions = {
  headers: new HttpHeaders({ 'Content-Type': 'application/json' })
};

@Injectable()
export class AddressesService {

  private baseUrl = environment.api.url + '/addresses';

  constructor(private http: HttpClient) { }

  get(address: string): Observable<Balance> {
    const url = `${this.baseUrl}/${address}`;
    return this.http.get<Balance>(url);
  }

  getTransactions(address: string, offset: number = 0, limit: number = 10, orderBy: string = ''): Observable<PaginatedResult<Transaction>> {
    const url = `${this.baseUrl}/${address}/transactions?offset=${offset}&limit=${limit}&orderBy=${orderBy}`;
    return this.http.get<PaginatedResult<Transaction>>(url);
  }
}
