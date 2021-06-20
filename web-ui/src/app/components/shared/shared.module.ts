import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { MomentModule } from 'ngx-moment';
import { NavbarComponent } from './navbar/navbar.component';
import { FinderComponent } from './finder/finder.component';
import { FooterComponent } from './footer/footer.component';
import { BlockTableComponent } from './block-table/block-table.component';
import { InfiniteScrollModule } from 'ngx-infinite-scroll';
import { TransactionTableComponent } from './transaction-table/transaction-table.component';
import { TransactionTablev2Component } from './transaction-tablev2/transaction-tablev2.component';
import { PipesModule } from '../../pipes/pipes.module';
import { BsDropdownModule } from 'ngx-bootstrap/dropdown';
import { TooltipModule } from 'ng2-tooltip-directive';

import { LottieAnimationViewModule } from 'ng-lottie';

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        CommonModule,
        PipesModule,
        ReactiveFormsModule,
        RouterModule,
        TranslateModule,
        MomentModule,
        InfiniteScrollModule,
        BsDropdownModule.forRoot(),
        LottieAnimationViewModule.forRoot(),
        TooltipModule
    ],
    declarations: [
        NavbarComponent,
        FinderComponent,
        FooterComponent,
        BlockTableComponent,
        TransactionTableComponent,
        TransactionTablev2Component
    ],
    exports: [
        NavbarComponent,
        FinderComponent,
        FooterComponent,
        BlockTableComponent,
        TransactionTableComponent,
        TransactionTablev2Component,
        LottieAnimationViewModule
    ],
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
})
export class SharedModule { }
