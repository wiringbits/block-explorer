import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';

import { Balance } from '../models/balance';
import { PaginatedResult } from '../models/paginated-result';
import { WrappedResult } from '../models/wrapped-result';

const httpOptions = {
  headers: new HttpHeaders({ 'Content-Type': 'application/json' })
};

@Injectable()
export class BalancesService {

  private baseUrl = environment.api.url + '/balances';
  private baseUrlV2 = environment.api.url + '/v2/balances';

  constructor(private http: HttpClient) { }

  get(offset: number = 0, limit: number = 10, orderBy: string = ''): Observable<PaginatedResult<Balance>> {
    const url = `${this.baseUrl}?offset=${offset}&limit=${limit}&orderBy=${orderBy}`;
    return this.http.get<PaginatedResult<Balance>>(url);
  }

  getHighest(limit: number = 10, lastSeenAddress: string = ''): Observable<WrappedResult<Balance>> {
    let url = `${this.baseUrlV2}?limit=${limit}`;
    if (lastSeenAddress !== '') {
      url += `&lastSeenAddress=${lastSeenAddress}`;
    }

    return this.http.get<WrappedResult<Balance>>(url);
  }
}
