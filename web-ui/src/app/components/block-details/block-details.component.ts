import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { TranslateService } from '@ngx-translate/core';

import { BlockDetails } from '../../models/block';

import { BlocksService } from '../../services/blocks.service';
import { ErrorService } from '../../services/error.service';
import { NavigatorService } from '../../services/navigator.service';

@Component({
  selector: 'app-block-details',
  templateUrl: './block-details.component.html',
  styleUrls: ['./block-details.component.css']
})
export class BlockDetailsComponent implements OnInit {

  blockDetails: BlockDetails;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private navigatorService: NavigatorService,
    private blocksService: BlocksService,
    private errorService: ErrorService) { }

  ngOnInit() {
    this.route.params.forEach(params => this.onBlockhash(params['blockhash']));
  }

  private onBlockhash(blockhash: string) {
    this.blockDetails = null;
    this.blocksService.get(blockhash).subscribe(
      response => this.onBlockRetrieved(response),
      response => this.onError(response)
    );
  }

  private onBlockRetrieved(response: BlockDetails) {
    this.blockDetails = response;
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
