import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { TransactionsComponent } from './transactions.component';
import { TransactionComponent } from './transaction/transaction.component';
import { TransactionListComponent } from './transaction-list/transaction-list.component';

const routes: Routes = [
    {
        path: '',
        component: TransactionsComponent,
        children: [
            {
                path: ':id',
                component: TransactionComponent
            },
            {
                path: '',
                component: TransactionListComponent
            }
        ]
    }
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule]
})
export class TransactionsRoutingModule { }
