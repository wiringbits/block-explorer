import { Component, OnInit } from '@angular/core';
import { TickerService } from '../../../services/ticker.service';

@Component({
    selector: 'app-node-list',
    templateUrl: './node-list.component.html',
    styleUrls: ['./node-list.component.css']
})
export class NodeListComponent implements OnInit {
    isMasternodeUpdating = false;
    isTPosNodesUpdating = false;
    lottieConfig = null;
    constructor(private tickerService: TickerService) {
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
        this.tickerService.isUpdatingObserver.next(true);
        setTimeout(() => this.tickerService.isUpdatingObserver.next(false), 3000)
        this.isMasternodeUpdating = value;
    }

    updateTPosNodes(value) {
        this.isTPosNodesUpdating = value;
    }
}
