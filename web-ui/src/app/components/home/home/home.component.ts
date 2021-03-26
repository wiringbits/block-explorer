import { Component, OnInit } from '@angular/core';
import { Block } from '../../../models/block';
import { Transaction } from '../../../models/transaction';
import { BlocksService } from '../../../services/blocks.service';
import { TransactionsService } from '../../../services/transactions.service';
import { ErrorService } from '../../../services/error.service';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements OnInit {

  currentView = 'latestBlocks';
  blocks: Block[] = [];
  transactions: Transaction[] = [];
  address: string;
  limit = 10;
  isLoading: Boolean = false;

  constructor(private blocksService: BlocksService, private transactionsService: TransactionsService,
    private errorService: ErrorService) { }

  ngOnInit() {
    this.updateBlocks();
    this.updateTransactions();
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

  private updateTransactions() {
    let lastSeenTxId = '';
    if (this.transactions.length > 0) {
      lastSeenTxId = this.transactions[this.transactions.length - 1].id;
    }
    this.isLoading = true;
    this.transactionsService
      .getList(lastSeenTxId, this.limit)
      .subscribe(
        response => this.onTransactionRetrieved(response.data),
        response => this.onError(response)
      );
  }

  private onTransactionRetrieved(response: Transaction[]) {
    this.isLoading = false;
    // this.lastSeenTxId = this.transactions.reduce((max, block) => Math.max(block.height, max), 0);
    this.transactions = this.transactions.concat(response).sort(function (a, b) {
      if (a.height > b.height) return -1;
      else return 1;
    });
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
