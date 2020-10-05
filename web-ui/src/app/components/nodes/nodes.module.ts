import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { SharedModule } from '../shared/shared.module';
import { TranslateModule } from '@ngx-translate/core';
import { MomentModule } from 'ngx-moment';
import { TabsModule, AlertModule } from 'ngx-bootstrap';
import { PipesModule } from '../../pipes/pipes.module';
import { InfiniteScrollModule } from 'ngx-infinite-scroll';
import { NodesRoutingModule } from './nodes-routing.module';
import { NodesComponent } from './nodes.component';
import { MasternodesComponent } from './masternodes/masternodes.component';
import { MasternodeDetailsComponent } from './masternode-details/masternode-details.component';
import { NgxPaginationModule } from 'ngx-pagination';
import { TposnodesComponent } from './tposnodes/tposnodes.component';
import { NodeListComponent } from './node-list/node-list.component';
import { NodeTickerComponent } from './node-ticker/node-ticker.component';

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        NodesRoutingModule,
        SharedModule,
        PipesModule,
        TranslateModule,
        MomentModule,
        TabsModule,
        AlertModule,
        InfiniteScrollModule,
        NgxPaginationModule
    ],
    declarations: [
        NodesComponent,
        MasternodesComponent,
        TposnodesComponent,
        NodeListComponent,
        NodeTickerComponent,
        MasternodeDetailsComponent
    ]
})

export class NodesModule { }
