
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';

import { environment } from '../../environments/environment';
import { TposContractEncoded } from '../models/tpos-contract';

const httpOptions = {
    headers: new HttpHeaders({ 'Content-Type': 'application/json' })
};

@Injectable()
export class TposContractsService {

    private baseUrl = environment.api.url + '/tposcontracts';

    constructor(private http: HttpClient) { }

    encodeTPOS(tposAddress: string, merchantAddress: string, commission: number, signature: string): Promise<TposContractEncoded> {
        // tslint:disable-next-line: max-line-length
        const url = `${this.baseUrl}/encode`;
        const body = {
            tposAddress: tposAddress,
            merchantAddress: merchantAddress,
            commission: commission,
            signature: signature
        };

        return this.http.post<TposContractEncoded>(url, body, httpOptions).toPromise();
    }
}
