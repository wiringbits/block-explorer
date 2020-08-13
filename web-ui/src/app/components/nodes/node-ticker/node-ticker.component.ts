import { Component, OnInit } from '@angular/core';

import { TickerService } from '../../../services/ticker.service';
import { NodeStats, ServerStats } from '../../../models/ticker';
import { Config } from '../../../config';

@Component({
  selector: 'app-node-ticker',
  templateUrl: './node-ticker.component.html',
  styleUrls: ['./node-ticker.component.css']
})
export class NodeTickerComponent implements OnInit {

  stats: ServerStats = new ServerStats();
  nodeStats: NodeStats = new NodeStats();
  config = Config;

  constructor(private tickerService: TickerService) { }

  ngOnInit() {
    this.tickerService
      .getNodeStats()
      .subscribe(
        response => this.nodeStats = response,
        response => this.onError(response)
      );

    this.tickerService
      .get()
      .subscribe(
        response => this.stats = response,
        response => this.onError(response)
      );
  }

  private onError(response: any) {
    console.log(response);
  }
}
