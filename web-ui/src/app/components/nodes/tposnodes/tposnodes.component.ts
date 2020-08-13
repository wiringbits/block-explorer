
import { map, tap } from 'rxjs/operators';
import { Component, OnInit } from '@angular/core';

import { Observable } from 'rxjs';

import { Tposnode } from '../../../models/tposnode';

import { TposnodesService } from '../../../services/tposnodes.service';
import { ErrorService } from '../../../services/error.service';

import { amAgo } from '../../../utils';

@Component({
  selector: 'app-tposnodes',
  templateUrl: './tposnodes.component.html',
  styleUrls: ['./tposnodes.component.css']
})
export class TposnodesComponent implements OnInit {

  Math: Math = Math;

  // pagination
  total = 0;
  currentPage = 1;
  pageSize = 10;
  asyncItems: Observable<Tposnode[]>;

  amAgo = amAgo;

  constructor(
    private tposnodesService: TposnodesService,
    private errorService: ErrorService) { }

  ngOnInit() {
    this.getPage(this.currentPage);
  }

  getPage(page: number) {
    const offset = (page - 1) * this.pageSize;
    const limit = this.pageSize;

    this.asyncItems = this.tposnodesService
      .get(offset, limit, 'activeSeconds:desc').pipe(
        tap(response => this.total = response.total),
        tap(response => this.currentPage = 1 + (response.offset / this.pageSize)),
        map(response => response.data));
  }
}
