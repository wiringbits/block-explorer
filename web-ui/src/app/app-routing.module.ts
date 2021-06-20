import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { TrezorConnectComponent } from './components/trezor-connect/trezor-connect.component';
import { CalculatorComponent } from './components/calculator/calculator.component';
import { DexMonitorComponent } from './components/dex-monitor/dex-monitor.component';

const routes: Routes = [
  { path: '', loadChildren: './components/home/home.module#HomeModule' },
  { path: 'blocks', loadChildren: './components/blocks/blocks.module#BlocksModule' },
  { path: 'transactions', loadChildren: './components/transactions/transactions.module#TransactionsModule' },
  { path: 'nodes', loadChildren: './components/nodes/nodes.module#NodesModule' },
  { path: 'addresses', loadChildren: './components/addresses/addresses.module#AddressesModule' },
  { path: 'trezor', component: TrezorConnectComponent },
  { path: 'dex-monitor', component: DexMonitorComponent },
  { path: 'calculator', component: CalculatorComponent },
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
