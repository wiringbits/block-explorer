import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';

import { Transaction } from '../models/transaction';
import { PaginatedResult } from '../models/paginated-result';

const httpOptions = {
  headers: new HttpHeaders({ 'Content-Type': 'application/json' })
};

@Injectable()
export class TransactionsService {

  private baseUrl = environment.api.url + '/transactions';

  constructor(private http: HttpClient) { }

  getList(lastSeenTxId: string, limit: number = 10, orderBy: string = 'asc'): Observable<PaginatedResult<Transaction>> {
    let url = `${this.baseUrl}?limit=${limit}&orderBy=${orderBy}`;
    if (lastSeenTxId) {
      url += `&lastSeenTxid=${lastSeenTxId}`;
    }

    return this.http.get<PaginatedResult<Transaction>>(url);
  }

  get(txid: string): Observable<Transaction> {
    const url = `${this.baseUrl}/${txid}`;
    return this.http.get<Transaction>(url);
  }

  getRaw(txid: string): Observable<any> {
    const url = `${this.baseUrl}/${txid}/raw`;
    return this.http.get<any>(url);
  }

  push(hex: string): Observable<any> {
    const url = this.baseUrl;
    const body = {
      hex: hex
    };

    return this.http.post<any>(url, body, httpOptions);
  }
}
