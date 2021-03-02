import { Component, OnInit } from '@angular/core';
import { Block } from '../../../models/block';
import { BlocksService } from '../../../services/blocks.service';
import { ErrorService } from '../../../services/error.service';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements OnInit {

  currentView = 'latestBlocks';
  blocks: Block[] = [];
  address: string;
  limit = 20;

  constructor(private blocksService: BlocksService,
    private errorService: ErrorService) { }

  ngOnInit() {
    this.updateBlocks();
  }

  selectView(view: string) {
    this.currentView = view;
  }

  isSelected(view: string): boolean {
    return this.currentView === view;
  }

  private updateBlocks(isInfiniteScroll = false) {
    if (isInfiniteScroll) {
      return;
    }
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
}
