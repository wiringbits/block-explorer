import { Component, OnInit, HostListener } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { BlockDetails } from '../../../models/block';
import { BlocksService } from '../../../services/blocks.service';
import { ErrorService } from '../../../services/error.service';

@Component({
  selector: 'app-block',
  templateUrl: './block.component.html',
  styleUrls: ['./block.component.css']
})
export class BlockComponent implements OnInit {

  currentView = 'details';

  blockhash: string;
  blockDetails: BlockDetails;

  constructor(private route: ActivatedRoute,
    private blocksService: BlocksService,
    private errorService: ErrorService) { }

  ngOnInit() {
    this.route.params.forEach(params => this.onQuery(params['id']));
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
  }

  private onBlockRetrieved(response: BlockDetails) {
    this.blockDetails = response;
    this.blockhash = response.block.hash;
  }

  private onError(response: any) {
    this.errorService.renderServerErrors(null, response);
  }

  selectView(view: string) {
    this.currentView = view;
  }

  isSelected(view: string): boolean {
    return this.currentView === view;
  }
}
