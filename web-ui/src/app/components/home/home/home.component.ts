import { Component, OnInit, OnDestroy } from '@angular/core';
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
  isTransactionLoading: Boolean = false;
  isBlockLoading: Boolean = false;
  interval = null;
  isBlockUpdating: Boolean = false;
  isTransactionUpdating: Boolean = false;
  public lottieConfig: Object;

  constructor(private blocksService: BlocksService, private transactionsService: TransactionsService,
    private errorService: ErrorService) {
      this.lottieConfig = {
        path: 'assets/Updating.json',
        renderer: 'canvas',
        autoplay: true,
        loop: true
      };
    }

  ngOnInit() {
    this.isBlockLoading = true;
    this.isTransactionLoading = true;
    this.loadData();
    this.interval = setInterval(() => this.loadData(true), 10000);
  }

  loadData(updating = false) {
    this.isBlockUpdating = updating;
    this.isTransactionUpdating = updating;
    this.updateBlocks();
    this.updateTransactions();
  }

  ngOnDestroy() {
    clearInterval(this.interval);
    this.interval = null;
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
    this.transactionsService
      .getList(lastSeenTxId, this.limit)
      .subscribe(
        response => this.onTransactionRetrieved(response.data),
        response => this.onError(response)
      );
  }

  private onTransactionRetrieved(response: Transaction[]) {
    this.isTransactionLoading = false;
    this.isTransactionUpdating = false;
    this.transactions = Object.assign([], response.sort(function (a, b) {
      if (a.height > b.height) return -1;
      else return 1;
    }));
  }

  private onBlockRetrieved(response: Block[]) {
    this.isBlockLoading = false;
    this.isBlockUpdating = false;
    this.blocks = Object.assign([], response.sort(function (a, b) {
      if (a.height > b.height) return -1;
      else return 1;
    }));
  }

  private onError(response: any) {
    this.errorService.renderServerErrors(null, response);
  }
}
