import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { BlocksComponent } from './blocks.component';
import { BlockComponent } from './block/block.component';
import { BlockListComponent } from './block-list/block-list.component';

const routes: Routes = [
    {
        path: '',
        component: BlocksComponent,
        children: [
            {
                path: ':id',
                component: BlockComponent
            },
            {
                path: '',
                component: BlockListComponent
            }
        ]
    }
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule]
})
export class BlocksRoutingModule { }
