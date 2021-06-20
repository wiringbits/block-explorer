import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { BlocksRoutingModule } from './blocks-routing.module';
import { BlockComponent } from './block/block.component';
import { BlocksComponent } from './blocks.component';
import { BlockDetailsComponent } from './block-details/block-details.component';
import { BlockRawComponent } from './block-raw/block-raw.component';
import { SharedModule } from '../shared/shared.module';
import { TranslateModule } from '@ngx-translate/core';
import { MomentModule } from 'ngx-moment';
import { TabsModule, AlertModule } from 'ngx-bootstrap';
import { PipesModule } from '../../pipes/pipes.module';
import { InfiniteScrollModule } from 'ngx-infinite-scroll';
import { BlockListComponent } from './block-list/block-list.component';

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        BlocksRoutingModule,
        SharedModule,
        PipesModule,
        TranslateModule,
        MomentModule,
        TabsModule,
        AlertModule,
        InfiniteScrollModule
    ],
    declarations: [
        BlocksComponent,
        BlockListComponent,
        BlockComponent,
        BlockDetailsComponent,
        BlockRawComponent
    ]
})

export class BlocksModule { }
