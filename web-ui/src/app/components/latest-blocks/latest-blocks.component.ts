import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { Observable } from 'rxjs/Observable';
import { Subscription } from 'rxjs/Subscription';
import { Subject } from 'rxjs/Subject';

import 'rxjs/add/operator/do';
import 'rxjs/add/operator/merge';
import 'rxjs/add/operator/switchMap';

import 'rxjs/add/observable/of';

import { TranslateService } from '@ngx-translate/core';

import { Block } from '../../models/block';

import { BlocksService } from '../../services/blocks.service';
import { ErrorService } from '../../services/error.service';

@Component({
  selector: 'app-latest-blocks',
  templateUrl: './latest-blocks.component.html',
  styleUrls: ['./latest-blocks.component.css']
})
export class LatestBlocksComponent implements OnInit, OnDestroy {

  blocks: Block[];
  private newBlocks: Block[];
  private subscription$: Subscription;

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
    const polling$ = new Subject();

    /**
     * There is around 1 new block per minute, it is a waste of resources
     * to be checking for new blocks quite frequently.
     */
    const interval = 50000;

    // polling based on https://stackoverflow.com/a/42659054/3211175
    this.subscription$ = Observable
      .of(null)
      .merge(polling$)
      .switchMap(_ =>
        this.blocksService.getLatest()
          .do(_ => {
            setTimeout(_ => polling$.next(null), interval);
          })
      )
      .subscribe(
        response => this.onBlockRetrieved(response),
        response => this.onError(response)
      );
  }

  private onBlockRetrieved(response: Block[]) {
    this.newBlocks = this.getNewBlocks(response);
    this.blocks = response;
  }

  private getNewBlocks(newBlocks: Block[]): Block[] {
    if (!this.blocks) {
      return [];
    }

    return newBlocks.filter(newBlock => newBlock.height > this.blocks[0].height);
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
    return this.newBlocks.some(newBlock => newBlock.height === item.height);
  }
}
