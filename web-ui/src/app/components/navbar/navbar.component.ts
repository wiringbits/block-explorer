import { Component, OnInit } from '@angular/core';
import { Location } from '@angular/common';

class Tab {
  label: string;
  path: string;
}

@Component({
  selector: 'app-navbar',
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css']
})
export class NavbarComponent implements OnInit {

  public tabs: Tab[] = [
    { label: 'label.richestAddresses', path: 'richest-addresses' }
  ];

  constructor(private location: Location) { }

  ngOnInit() {
  }

  /* tabs */
  isSelected(path: string): boolean {
    if (!path.startsWith('/')) {
      path = '/' + path;
    }

    return this.location.isCurrentPathEqualTo(path);
  }
}
