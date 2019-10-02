import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { HomeComponent } from './components/home/home.component';
import { TransactionComponent } from './components/transaction/transaction.component';
import { AddressDetailsComponent } from './components/address-details/address-details.component';
import { BlockComponent } from './components/block/block.component';
import { MasternodeDetailsComponent } from './components/masternode-details/masternode-details.component';
import { TrezorConnectComponent } from './components/trezor-connect/trezor-connect.component';

const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'addresses/:address', component: AddressDetailsComponent },
  { path: 'blocks/:query', component: BlockComponent },
  { path: 'transactions/:txid', component: TransactionComponent },
  { path: 'masternodes/:ip', component: MasternodeDetailsComponent },
  { path: 'trezor', component: TrezorConnectComponent },
  { path: '**', redirectTo: '' }
];

@NgModule({
  imports: [
    RouterModule.forRoot(routes)
  ],
  exports: [
    RouterModule
  ]
})
export class AppRoutingModule {

}
