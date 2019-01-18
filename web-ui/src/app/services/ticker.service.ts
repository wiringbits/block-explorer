import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';

import { ServerStats } from '../models/ticker';

const httpOptions = {
  headers: new HttpHeaders({ 'Content-Type': 'application/json' })
};

@Injectable()
export class TickerService {

  private baseUrl = environment.api.url + '/stats';

  constructor(private http: HttpClient) { }

  get(): Observable<ServerStats> {
    const url = this.baseUrl;
    return this.http.get<ServerStats>(url);
  }
}
