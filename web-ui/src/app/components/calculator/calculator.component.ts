import { Component, OnInit } from '@angular/core';
import { NodeStats, Prices, ServerStats } from '../../models/ticker';
import { TickerService } from '../../services/ticker.service';

@Component({
  selector: 'app-calculator',
  templateUrl: './calculator.component.html',
  styleUrls: ['./calculator.component.css']
})
export class CalculatorComponent implements OnInit {

  transaction: any;
  prices: Prices = new Prices();
  stats: NodeStats = new NodeStats();
  serverStats: ServerStats = new ServerStats();
  holdAmount = 17531;

  requiredForMasternode = 15000;

  masternodeCount = 1;
  xsnStaking = 2531;

  constructor(private tickerService: TickerService) { }

  ngOnInit() {
    this.tickerService
      .getNodeStats()
      .subscribe(
        response => this.stats = response,
        response => this.onError(response)
      );

    this.tickerService
      .get()
      .subscribe(
        response => this.serverStats = response,
        response => this.onError(response)
      );

    this.tickerService
      .getPrices()
      .subscribe(
        response => this.prices = response,
        response => this.onError(response)
      );
  }

  private onError(response: any) {
    console.log(response);
  }

  onChangeAmount() {
    this.masternodeCount = Math.floor(this.holdAmount / this.requiredForMasternode);
    this.xsnStaking = this.holdAmount % this.requiredForMasternode;
  }
}
