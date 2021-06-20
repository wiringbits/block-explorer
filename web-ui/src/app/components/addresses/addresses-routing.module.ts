import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AddressesComponent } from './addresses.component';
import { AddressDetailsComponent } from './address-details/address-details.component';
import { AddressListComponent } from './address-list/address-list.component';

const routes: Routes = [
    {
        path: '',
        component: AddressesComponent,
        children: [
            {
                path: ':id',
                component: AddressDetailsComponent
            },
            {
                path: '',
                component: AddressListComponent
            }
        ]
    }
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule]
})
export class AddressesRoutingModule { }
