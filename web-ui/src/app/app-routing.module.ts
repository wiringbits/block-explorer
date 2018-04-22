import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { HomeComponent } from './components/home/home.component';
import { TransactionDetailsComponent } from './components/transaction-details/transaction-details.component';
import { AddressDetailsComponent } from './components/address-details/address-details.component';
import { BlockDetailsComponent } from './components/block-details/block-details.component';
import { RichestAddressesComponent } from './components/richest-addresses/richest-addresses.component';
import { MasternodesComponent } from './components/masternodes/masternodes.component';

const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'addresses/:address', component: AddressDetailsComponent },
  { path: 'blocks/:blockhash', component: BlockDetailsComponent },
  { path: 'masternodes', component: MasternodesComponent },
  { path: 'transactions/:txid', component: TransactionDetailsComponent },
  { path: 'richest-addresses', component: RichestAddressesComponent },
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
