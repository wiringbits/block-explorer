import { Injectable } from '@angular/core';

import { Router } from '@angular/router';

@Injectable()
export class NavigatorService {

  constructor(private router: Router) { }

  go(path: string) {
    this.router.navigate([path]);
  }

  addressDetails(address: string) {
    this.go('/addresses/' + address);
  }

  blockDetails(blockhash: string) {
    this.go('/blocks/' + blockhash);
  }

  transactionDetails(txid: string) {
    this.go('/transactions/' + txid);
  }

  masternodeDetails(ipAddress: string) {
    this.go(`/nodes/${ipAddress}`);
  }
}
