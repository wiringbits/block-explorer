import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { Masternode } from '../../../models/masternode';

import { MasternodesService } from '../../../services/masternodes.service';
import { ErrorService } from '../../../services/error.service';
import { NavigatorService } from '../../../services/navigator.service';

@Component({
  selector: 'app-masternode-details',
  templateUrl: './masternode-details.component.html',
  styleUrls: ['./masternode-details.component.css']
})
export class MasternodeDetailsComponent implements OnInit {

  Math: Math = Math;
  masternode: Masternode;

  constructor(
    private route: ActivatedRoute,
    private navigatorService: NavigatorService,
    private masternodesService: MasternodesService,
    private errorService: ErrorService) { }

  ngOnInit() {
    this.route.params.forEach(params => this.onIP(params['ip']));
  }

  private onIP(ip: string) {
    this.masternode = null;
    this.masternodesService.getByIP(ip).subscribe(
      response => this.onMasternodeRetrieved(response),
      response => this.onError(response)
    );
  }

  private onMasternodeRetrieved(response: Masternode) {
    this.masternode = response;
  }

  private onError(response: any) {
    this.errorService.renderServerErrors(null, response);
  }
}
