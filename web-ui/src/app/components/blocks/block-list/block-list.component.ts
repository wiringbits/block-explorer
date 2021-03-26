import { Component, OnInit } from '@angular/core';
import { Prices, ServerStats } from '../../../models/ticker';
import { TickerService } from '../../../services/ticker.service';
import { Block } from '../../../models/block';
import { BlocksService } from '../../../services/blocks.service';
import { ErrorService } from '../../../services/error.service';

@Component({
    selector: 'app-block-list',
    templateUrl: './block-list.component.html',
    styleUrls: ['./block-list.component.css']
})
export class BlockListComponent implements OnInit {

    ticker: ServerStats = new ServerStats();
    prices: Prices = new Prices();
    stats: ServerStats = new ServerStats();
    blocks: Block[] = [];
    limit = 20;
    lastSeenHash: string;
    isLoading: Boolean = false;

    constructor(private tickerService: TickerService, private blocksService: BlocksService,
        private errorService: ErrorService) {
        this.lastSeenHash = '';
    }

    ngOnInit() {
        this.tickerService
            .getPrices()
            .subscribe(
                response => this.prices = response,
                response => console.log(response)
            );

        this.tickerService
            .get()
            .subscribe(
                response => this.ticker = this.stats = response,
                response => console.log(response)
            );

        this.updateBlocks();
    }

    private updateBlocks() {
        this.isLoading = true;
        let lastSeenHash = '';
        if (this.blocks.length > 0) {
            lastSeenHash = this.blocks[this.blocks.length - 1].hash;
        }

        this.blocksService
            .getLatestV2(this.limit, lastSeenHash)
            .subscribe(
                response => this.onBlockRetrieved(response),
                response => this.onError(response)
            );
    }

    private onError(response: any) {
        this.errorService.renderServerErrors(null, response);
    }

    private onBlockRetrieved(response: Block[]) {
        this.isLoading = false;
        // this.latestBlockHeight = this.blocks.reduce((max, block) => Math.max(block.height, max), 0);
        this.blocks = this.blocks.concat(response).sort(function (a, b) {
            if (a.height > b.height) return -1;
            else return 1;
        });
    }
}
