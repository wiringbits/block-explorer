import { Component, OnInit, Input, HostListener } from '@angular/core';
import { tap } from 'rxjs/operators';

import { BlockDetails } from '../../../models/block';
import { Transaction } from '../../../models/transaction';
import { BlocksService } from '../../../services/blocks.service';
import { getNumberOfRowsForScreen, amAgo } from '../../../utils';

@Component({
  selector: 'app-block-details',
  templateUrl: './block-details.component.html',
  styleUrls: ['./block-details.component.css']
})
export class BlockDetailsComponent implements OnInit {

  @Input()
  blockDetails: BlockDetails;

  // pagination
  limit = 30;
  transactions: Transaction[] = [];

  amAgo = amAgo;

  constructor(private blocksService: BlocksService) { }

  ngOnInit() {
    const height = this.getScreenSize();
    this.limit = getNumberOfRowsForScreen(height);

    this.load();
  }

  @HostListener('window:resize', ['$event'])
  private getScreenSize(_?): number {
    return window.innerHeight;
  }

  load() {
    let lastSeenTxid = '';
    if (this.transactions.length > 0) {
      lastSeenTxid = this.transactions[this.transactions.length - 1].id;
    }

    this.blocksService
      .getTransactionsV2(this.blockDetails.block.hash, this.limit, lastSeenTxid).pipe(
        tap(response => this.transactions.push(...response.data)))
      .subscribe();
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
