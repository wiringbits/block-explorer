import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { HomeComponent } from './home/home.component';
import { HomeRoutingModule } from './home-routing.module';
import { TickerComponent } from './ticker/ticker.component';
import { TranslateModule } from '@ngx-translate/core';
import { SharedModule } from '../shared/shared.module';
import { PipesModule } from '../../pipes/pipes.module';
import { MomentModule } from 'ngx-moment';
import { TabsModule, AlertModule } from 'ngx-bootstrap';
import { InfiniteScrollModule } from 'ngx-infinite-scroll';

@NgModule({
    imports: [
        HomeRoutingModule,
        CommonModule,
        FormsModule,
        TranslateModule,
        ReactiveFormsModule,
        SharedModule,
        PipesModule,
        TranslateModule,
        MomentModule,
        TabsModule,
        AlertModule,
        InfiniteScrollModule
    ],
    declarations: [
        HomeComponent,
        TickerComponent
    ]
})
export class HomeModule { }
