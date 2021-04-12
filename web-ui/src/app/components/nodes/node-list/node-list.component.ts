import { Component, OnInit } from '@angular/core';

@Component({
    selector: 'app-node-list',
    templateUrl: './node-list.component.html',
    styleUrls: ['./node-list.component.css']
})
export class NodeListComponent implements OnInit {
    isMasternodeUpdating = false;
    isTPosNodesUpdating = false;
    lottieConfig = null;
    constructor() {
        this.lottieConfig = {
            path: 'assets/Updating.json',
            renderer: 'canvas',
            autoplay: true,
            loop: true
          };
    }

    ngOnInit() {
    }

    updateMasternode(value) {
        this.isMasternodeUpdating = value;
    }

    updateTPosNodes(value) {
        this.isTPosNodesUpdating = value;
    }
}
