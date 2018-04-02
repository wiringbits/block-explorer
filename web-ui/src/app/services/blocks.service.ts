import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs/Observable';

import { environment } from '../../environments/environment';

import { Block, BlockDetails } from '../models/block';

const httpOptions = {
  headers: new HttpHeaders({ 'Content-Type': 'application/json' })
};

@Injectable()
export class BlocksService {

  private baseUrl = environment.api.url + '/blocks';

  constructor(private http: HttpClient) { }

  get(blockhash: string): Observable<BlockDetails> {
    const url = `${this.baseUrl}/${blockhash}`;
    return this.http.get<BlockDetails>(url);
  }

  getLatest(): Observable<Block[]> {
    return this.http.get<Block[]>(this.baseUrl);
  }
}
