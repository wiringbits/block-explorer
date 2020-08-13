import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { SharedModule } from '../shared/shared.module';
import { TranslateModule } from '@ngx-translate/core';
import { MomentModule } from 'ngx-moment';
import { TabsModule, AlertModule } from 'ngx-bootstrap';
import { PipesModule } from '../../pipes/pipes.module';
import { InfiniteScrollModule } from 'ngx-infinite-scroll';
import { AddressesRoutingModule } from './addresses-routing.module';
import { AddressesComponent } from './addresses.component';
import { AddressDetailsComponent } from './address-details/address-details.component';
import { AddressListComponent } from './address-list/address-list.component';

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        AddressesRoutingModule,
        SharedModule,
        PipesModule,
        TranslateModule,
        MomentModule,
        TabsModule,
        AlertModule,
        InfiniteScrollModule
    ],
    declarations: [
        AddressesComponent,
        AddressDetailsComponent,
        AddressListComponent
    ]
})

export class AddressesModule { }
