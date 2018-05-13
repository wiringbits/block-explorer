import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-transaction',
  templateUrl: './transaction.component.html',
  styleUrls: ['./transaction.component.css']
})
export class TransactionComponent implements OnInit {

  currentView = 'details';

  constructor() { }

  ngOnInit() {
  }

  selectView(view: string) {
    this.currentView = view;
  }

  isSelected(view: string): boolean {
    return this.currentView === view;
  }
}
