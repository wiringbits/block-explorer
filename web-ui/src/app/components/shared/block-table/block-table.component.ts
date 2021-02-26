import { Component, OnInit, OnDestroy, Input } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { Subscription } from 'rxjs';

import { Block } from '../../../models/block';

import { BlocksService } from '../../../services/blocks.service';
import { ErrorService } from '../../../services/error.service';
import { truncate, amAgo } from '../../../utils';

@Component({
  selector: 'app-block-table',
  templateUrl: './block-table.component.html',
  styleUrls: ['./block-table.component.css']
})
export class BlockTableComponent implements OnInit, OnDestroy {

  @Input()
  hideBlockHash: boolean;

  blocks: Block[] = [];
  private latestBlockHeight = 0;
  private subscription$: Subscription;

  limit = 20;

  truncate = truncate;
  amAgo = amAgo;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private blocksService: BlocksService,
    private errorService: ErrorService) { }

  ngOnInit() {
    this.updateBlocks();
  }

  ngOnDestroy() {
    if (this.subscription$ != null) {
      this.subscription$.unsubscribe();
    }
  }

  private updateBlocks() {
    let lastSeenHash = '';
    if (this.blocks.length > 0) {
      lastSeenHash = this.blocks[this.blocks.length - 1].hash;
    }

    this.blocksService
      .getLatest(this.limit, lastSeenHash)
      .subscribe(
        response => this.onBlockRetrieved(response),
        response => this.onError(response)
      );
  }

  private onBlockRetrieved(response: Block[]) {
    // this.latestBlockHeight = this.blocks.reduce((max, block) => Math.max(block.height, max), 0);
    this.blocks = this.blocks.concat(response).sort(function (a, b) {
      if (a.height > b.height) return -1;
      else return 1;
    });
  }

  private onError(response: any) {
    this.errorService.renderServerErrors(null, response);
  }

  extractedBy(block: Block): string {
    if (block.height <= 75) {
      return 'PoW';
    }

    if (block.tposContract == null) {
      return 'PoS';
    } else {
      return 'TPoS';
    }
  }

  age(block: Block): string {
    return '';
  }

  isBlockRecent(item: Block): boolean {
    return item.height > this.latestBlockHeight;
  }
}
