import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, Subject  } from 'rxjs';

import { environment } from '../../environments/environment';

import { NodeStats, Prices, ServerStats } from '../models/ticker';

const httpOptions = {
  headers: new HttpHeaders({ 'Content-Type': 'application/json' })
};

@Injectable()
export class TickerService {

  private baseUrl = environment.api.url;
  isUpdating: Boolean = false;
  isUpdatingObserver: Subject<boolean> = new Subject<boolean>();

  constructor(private http: HttpClient) {
    this.isUpdatingObserver.subscribe(value => this.isUpdating = value);
  }

  get(): Observable<ServerStats> {
    const url = this.baseUrl + '/stats';
    return this.http.get<ServerStats>(url);
  }

  getNodeStats(): Observable<NodeStats> {
    const url = this.baseUrl + '/node-stats';
    return this.http.get<NodeStats>(url);
  }

  getPrices(): Observable<Prices> {
    const url = this.baseUrl + '/prices';
    return this.http.get<Prices>(url);
  }

  setUpdating(value: Boolean = true) {
    this.isUpdating = value;
    return this.isUpdating;
  }
}
