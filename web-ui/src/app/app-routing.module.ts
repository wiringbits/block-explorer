import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { HomeComponent } from './components/home/home.component';
import { TransactionComponent } from './components/transaction/transaction.component';
import { AddressDetailsComponent } from './components/address-details/address-details.component';
import { BlockDetailsComponent } from './components/block-details/block-details.component';
import { MasternodeDetailsComponent } from './components/masternode-details/masternode-details.component';

const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'addresses/:address', component: AddressDetailsComponent },
  { path: 'blocks/:blockhash', component: BlockDetailsComponent },
  { path: 'transactions/:txid', component: TransactionComponent },
  { path: 'masternodes/:ip', component: MasternodeDetailsComponent },
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
