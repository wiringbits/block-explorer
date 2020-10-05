import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';

import { Tposnode } from '../models/tposnode';
import { PaginatedResult } from '../models/paginated-result';

const httpOptions = {
  headers: new HttpHeaders({ 'Content-Type': 'application/json' })
};

@Injectable()
export class TposnodesService {

  private baseUrl = environment.api.url + '/merchantnodes';

  constructor(private http: HttpClient) { }

  get(offset: number = 0, limit: number = 10, orderBy: string = ''): Observable<PaginatedResult<Tposnode>> {
    const url = `${this.baseUrl}?offset=${offset}&limit=${limit}&orderBy=${orderBy}`;
    return this.http.get<PaginatedResult<Tposnode>>(url);
  }

  getByIP(ip: string): Observable<Tposnode> {
    const url = `${this.baseUrl}/${ip}`;
    return this.http.get<Tposnode>(url);
  }
}
