import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { SharedModule } from '../shared/shared.module';
import { TranslateModule } from '@ngx-translate/core';
import { MomentModule } from 'ngx-moment';
import { TabsModule, AlertModule } from 'ngx-bootstrap';
import { PipesModule } from '../../pipes/pipes.module';
import { InfiniteScrollModule } from 'ngx-infinite-scroll';
import { TransactionsRoutingModule } from './transactions-routing.module';
import { TransactionsComponent } from './transactions.component';
import { TransactionComponent } from './transaction/transaction.component';
import { TransactionDetailsComponent } from './transaction-details/transaction-details.component';
import { TransactionRawComponent } from './transaction-raw/transaction-raw.component';
import { TransactionListComponent } from './transaction-list/transaction-list.component';

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        TransactionsRoutingModule,
        SharedModule,
        PipesModule,
        TranslateModule,
        MomentModule,
        TabsModule,
        AlertModule,
        InfiniteScrollModule
    ],
    declarations: [
        TransactionsComponent,
        TransactionListComponent,
        TransactionComponent,
        TransactionDetailsComponent,
        TransactionRawComponent
    ]
})

export class TransactionsModule { }
