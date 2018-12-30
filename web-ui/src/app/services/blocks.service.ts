import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs/Observable';

import { environment } from '../../environments/environment';

import { Block, BlockDetails } from '../models/block';
import { PaginatedResult } from '../models/paginated-result';
import { Transaction } from '../models/transaction';
import { WrappedResult } from '../models/wrapped-result';

const httpOptions = {
  headers: new HttpHeaders({ 'Content-Type': 'application/json' })
};

@Injectable()
export class BlocksService {

  private baseUrl = environment.api.url + '/blocks';
  private baseUrlV2 = environment.api.url + '/v2/blocks';

  constructor(private http: HttpClient) { }

  get(blockhash: string): Observable<BlockDetails> {
    const url = `${this.baseUrl}/${blockhash}`;
    return this.http.get<BlockDetails>(url);
  }

  getRaw(query: string): Observable<any> {
    const url = `${this.baseUrl}/${query}/raw`;
    return this.http.get<any>(url);
  }

  getTransactions(hash: string, offset: number = 0, limit: number = 10, orderBy: string = ''): Observable<PaginatedResult<Transaction>> {
    const url = `${this.baseUrl}/${hash}/transactions?offset=${offset}&limit=${limit}&orderBy=${orderBy}`;
    return this.http.get<PaginatedResult<Transaction>>(url);
  }

  getTransactionsV2(hash: string, limit: number = 10, lastSeenTxid: string = ''): Observable<WrappedResult<Transaction>> {
    let url = `${this.baseUrlV2}/${hash}/transactions?&limit=${limit}`;
    if (lastSeenTxid !== '') {
      url += `&lastSeenTxid=${lastSeenTxid}`;
    }

    return this.http.get<WrappedResult<Transaction>>(url);
  }

  getLatest(): Observable<Block[]> {
    return this.http.get<Block[]>(this.baseUrl);
  }
}
