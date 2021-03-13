import { Component, OnInit, OnDestroy, Input, EventEmitter, Output } from '@angular/core';
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
  @Input()
  isLoading: boolean = false;
  @Input()
  blocks: Block[];
  @Output() updateBlocks: any = new EventEmitter();

  private latestBlockHeight = 0;
  private subscription$: Subscription;
  public lottieConfig: Object;

  limit = 10;

  truncate = truncate;
  amAgo = amAgo;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private blocksService: BlocksService,
    private errorService: ErrorService) {
      this.lottieConfig = {
        path: 'assets/loader.json',
        renderer: 'canvas',
        autoplay: true,
        loop: true
      };
    }

  ngOnInit() {
  }

  ngOnDestroy() {
    if (this.subscription$ != null) {
      this.subscription$.unsubscribe();
    }
  }

  getBlocks(isInfiniteScroll) {
    this.updateBlocks.emit(isInfiniteScroll);
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
