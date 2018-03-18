import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs/Observable';

import { environment } from '../../environments/environment';

import { Address } from '../models/address';

const httpOptions = {
  headers: new HttpHeaders({ 'Content-Type': 'application/json' })
};

@Injectable()
export class AddressesService {

  private baseUrl = environment.api.url + '/addresses';

  constructor(private http: HttpClient) { }

  get(address: string): Observable<Address> {
    const url = `${this.baseUrl}/${address}`;
    return this.http.get<Address>(url);
  }
}
