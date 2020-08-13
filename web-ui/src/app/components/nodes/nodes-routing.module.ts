import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { NodesComponent } from './nodes.component';
import { MasternodeDetailsComponent } from './masternode-details/masternode-details.component';
import { NodeListComponent } from './node-list/node-list.component';

const routes: Routes = [
    {
        path: '',
        component: NodesComponent,
        children: [
            {
                path: ':id',
                component: MasternodeDetailsComponent
            },
            {
                path: '',
                component: NodeListComponent
            }
        ]
    }
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule]
})
export class NodesRoutingModule { }
