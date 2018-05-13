import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { TranslateService } from '@ngx-translate/core';

import { Transaction } from '../../models/transaction';

import { ErrorService } from '../../services/error.service';
import { NavigatorService } from '../../services/navigator.service';
import { BlocksService } from '../../services/blocks.service';

@Component({
  selector: 'app-block-raw',
  templateUrl: './block-raw.component.html',
  styleUrls: ['./block-raw.component.css']
})
export class BlockRawComponent implements OnInit {

  block: any;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private navigatorService: NavigatorService,
    private blocksService: BlocksService,
    private errorService: ErrorService) { }

  ngOnInit() {
    this.route.params.forEach(params => this.onBlockQuery(params['query']));
  }

  private onBlockQuery(query: string) {
    this.blocksService.getRaw(query).subscribe(
      response => this.onBlockRetrieved(response),
      response => this.onError(response)
    );
  }

  private onBlockRetrieved(response: any) {
    this.block = response;
  }

  private onError(response: any) {
    this.errorService.renderServerErrors(null, response);
  }
}
