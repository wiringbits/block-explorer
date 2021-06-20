import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';

import { RewardsSummary, NodeStats, Prices } from '../models/xsn';

const httpOptions = {
  headers: new HttpHeaders({ 'Content-Type': 'application/json' })
};

@Injectable()
export class XSNService {

  private baseUrl = environment.api.url + '/xsn';

  constructor(private http: HttpClient) { }

  getRewardsSummary(): Observable<RewardsSummary> {
    const url = `${this.baseUrl}/rewards-summary`;
    return this.http.get<RewardsSummary>(url);
  }

  getNodeStats(): Observable<NodeStats> {
    const url = `${this.baseUrl}/node-stats`;
    return this.http.get<NodeStats>(url);
  }

  getPrices(): Observable<Prices> {
    const url = `${this.baseUrl}/prices`;
    return this.http.get<Prices>(url);
  }

}
