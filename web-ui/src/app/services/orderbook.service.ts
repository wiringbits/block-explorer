import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';

import { NodesInfo, TradesNumber, Volume } from '../models/orderbook';

const HTTPOPTIONS = {
    exposeHeaders: 'Access-Control-Allow-Origin',
    headers: new HttpHeaders({'Content-Type': 'application/json'})
};

@Injectable()
export class OrderBookService {

  private baseUrl = environment.api.orderbook;

  constructor(private http: HttpClient) { }

  getTradesNumber(subUrl: string, query: string): Observable<TradesNumber> {
    const url = `${this.baseUrl}/trading-pairs/${subUrl}/trades-number${query}`;
    return this.http.get<TradesNumber>(url, HTTPOPTIONS);
  }

  getNodesInfo(subUrl: string): Observable<NodesInfo> {
    const url = `${this.baseUrl}/trading-pairs/${subUrl}/nodes-info`;
    return this.http.get<NodesInfo>(url, HTTPOPTIONS);
  }

  getVolume(subUrl: string, query: string): Observable<Volume> {
    const url = `${this.baseUrl}/trading-pairs/${subUrl}/volume${query}`;
    return this.http.get<Volume>(url, HTTPOPTIONS);
  }
}
