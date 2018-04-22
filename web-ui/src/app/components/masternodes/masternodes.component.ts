import { Component, OnInit } from '@angular/core';

import { Observable } from 'rxjs/Observable';

import 'rxjs/add/operator/do';
import 'rxjs/add/operator/map';

import { Masternode } from '../../models/masternode';

import { MasternodesService } from '../../services/masternodes.service';
import { ErrorService } from '../../services/error.service';

@Component({
  selector: 'app-masternodes',
  templateUrl: './masternodes.component.html',
  styleUrls: ['./masternodes.component.css']
})
export class MasternodesComponent implements OnInit {

  // pagination
  total = 0;
  currentPage = 1;
  pageSize = 10;
  asyncItems: Observable<Masternode[]>;

  constructor(
    private masternodesService: MasternodesService,
    private errorService: ErrorService) { }

  ngOnInit() {
    this.getPage(this.currentPage);
  }

  getPage(page: number) {
    const offset = (page - 1) * this.pageSize;
    const limit = this.pageSize;

    this.asyncItems = this.masternodesService
      .get(offset, limit, 'activeSeconds:desc')
      .do(response => this.total = response.total)
      .do(response => this.currentPage = 1 + (response.offset / this.pageSize))
      .map(response => response.data);
  }
}
