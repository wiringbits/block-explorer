
import { map, tap } from 'rxjs/operators';
import { Component, OnInit, EventEmitter, Output } from '@angular/core';

import { Observable } from 'rxjs';

import { Masternode } from '../../../models/masternode';

import { MasternodesService } from '../../../services/masternodes.service';
import { ErrorService } from '../../../services/error.service';

import { truncate, amAgo } from '../../../utils';

@Component({
  selector: 'app-masternodes',
  templateUrl: './masternodes.component.html',
  styleUrls: ['./masternodes.component.css']
})
export class MasternodesComponent implements OnInit {

  Math: Math = Math;

  // pagination
  total = 0;
  currentPage = 1;
  pageSize = 10;
  asyncItems: Observable<Masternode[]>;
  interval = null;
  @Output() update: any = new EventEmitter();

  amAgo = amAgo;
  truncate = truncate;

  constructor(
    private masternodesService: MasternodesService,
    private errorService: ErrorService) {
      
    }

  ngOnInit() {
    this.getPage(this.currentPage);
    this.interval = setInterval(() => this.loadData(), 10000);
  }

  loadData() {
    this.update.emit(true);
    this.getPage(this.currentPage)
  }

  ngOnDestroy() {
    clearInterval(this.interval);
    this.interval = null;
  }

  getPage(page: number) {
    const offset = (page - 1) * this.pageSize;
    const limit = this.pageSize;

    this.asyncItems = this.masternodesService
      .get(offset, limit, 'lastSeen:desc').pipe(
        tap(response => this.total = response.total),
        tap(response => this.currentPage = 1 + (response.offset / this.pageSize)),
        map(response => response.data.map(item => { return { ...item, ip: item["ip"].split(":")[0] } }).sort(function (a, b) {
          if (a.lastSeen > b.lastSeen) return -1;
          return 1;
        })));
    this.update.emit(false);
  }
}
