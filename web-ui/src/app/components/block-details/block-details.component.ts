import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { BlockDetails } from '../../models/block';
import { Transaction } from '../../models/transaction';

import { BlocksService } from '../../services/blocks.service';
import { ErrorService } from '../../services/error.service';
import { PaginatedResult } from '../../models/paginated-result';

@Component({
  selector: 'app-block-details',
  templateUrl: './block-details.component.html',
  styleUrls: ['./block-details.component.css']
})
export class BlockDetailsComponent implements OnInit {

  blockhash: string;
  blockDetails: BlockDetails;

  // pagination
  total = 0;
  currentPage = 1;
  pageSize = 10;
  transactions: Transaction[] = null;

  constructor(
    private route: ActivatedRoute,
    private blocksService: BlocksService,
    private errorService: ErrorService) { }

  ngOnInit() {
    this.route.params.forEach(params => this.onQuery(params['query']));
  }

  private onQuery(query: string) {
    this.clearCurrentValues();
    this.blocksService.get(query).subscribe(
      response => this.onBlockRetrieved(response),
      response => this.onError(response)
    );
  }

  private clearCurrentValues() {
    this.blockhash = null;
    this.blockDetails = null;
    this.total = 0;
    this.currentPage = 1;
    this.pageSize = 10;
    this.transactions = null;
  }

  private onBlockRetrieved(response: BlockDetails) {
    this.blockDetails = response;
    this.blockhash = response.block.hash;
    this.loadPage(this.currentPage);
  }

  loadPage(page: number) {
    const offset = (page - 1) * this.pageSize;
    const limit = this.pageSize;
    const order = 'time:desc';

    this.blocksService
      .getTransactions(this.blockhash, offset, limit, order)
      .subscribe(response => this.onTransactionsResponse(response));
  }

  private onTransactionsResponse(response: PaginatedResult<Transaction>) {
    this.total = response.total;
    this.currentPage = 1 + (response.offset / this.pageSize);
    this.transactions = response.data;
  }

  private onError(response: any) {
    this.errorService.renderServerErrors(null, response);
  }

  getBlockType(details: BlockDetails): string {
    if (details.block.tposContract != null) {
      return 'Trustless Proof of Stake';
    } else if (details.block.height > 75) {
      return 'Proof of Stake';
    } else {
      return 'Proof of Work';
    }
  }

  isPoW(details: BlockDetails): boolean {
    return details.block.height <= 75;
  }

  isPoS(details: BlockDetails): boolean {
    return !this.isPoW(details) && details.block.tposContract == null;
  }

  isTPoS(details: BlockDetails): boolean {
    return !this.isPoW(details) && details.block.tposContract != null;
  }

  getPoSTotalReward(details: BlockDetails): number {
    let total = 0;

    if (details.rewards.masternode != null) {
      total += details.rewards.masternode.value;
    }

    if (details.rewards.coinstake != null) {
      total += details.rewards.coinstake.value;
    }

    return total;
  }

  getTPoSTotalReward(details: BlockDetails): number {
    let total = 0;

    if (details.rewards.masternode != null) {
      total += details.rewards.masternode.value;
    }

    if (details.rewards.owner != null) {
      total += details.rewards.owner.value;
    }

    if (details.rewards.merchant != null) {
      total += details.rewards.merchant.value;
    }

    return total;
  }
}
