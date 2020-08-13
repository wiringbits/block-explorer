import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { MomentModule } from 'ngx-moment';
import { ExplorerAmountPipe } from './explorer-amount.pipe';
import { ExplorerCurrencyPipe } from './explorer-currency.pipe';
import { ExplorerDatetimePipe } from './explorer-datetime.pipe';

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        CommonModule,
        ReactiveFormsModule,
        RouterModule,
        TranslateModule,
        MomentModule
    ],
    declarations: [
        ExplorerAmountPipe,
        ExplorerCurrencyPipe,
        ExplorerDatetimePipe
    ],
    exports: [
        ExplorerAmountPipe,
        ExplorerCurrencyPipe,
        ExplorerDatetimePipe
    ]
})
export class PipesModule { }
