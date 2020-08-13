import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';

class Tab {
  label: string;
  path: string;
  mainTab: boolean;
}

@Component({
  selector: 'app-navbar',
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css']
})
export class NavbarComponent implements OnInit {

  public tabs: Tab[] = [
    {
      label: 'Dashboard',
      path: '/',
      mainTab: true
    },
    {
      label: 'Nodes',
      path: '/nodes',
      mainTab: true
    },
    {
      label: 'Blocks',
      path: '/blocks',
      mainTab: true
    },
    {
      label: 'Transactions',
      path: '/transactions',
      mainTab: true
    },
    {
      label: 'Addresses',
      path: '/addresses',
      mainTab: true
    },
    {
      label: 'Calculator',
      path: '/calculator',
      mainTab: false
    },
    {
      label: 'Governance',
      path: '/governance',
      mainTab: false
    }
  ];

  public currentUrl = null;

  constructor(private router: Router) {
  }

  ngOnInit() {
  }

  /* tabs */
  isSelected(path: string): boolean {
    path = path.replace(/\//g, '');

    const segments = this.router.url.split('/');
    if (path === '/') {
      return segments.length === 1;
    }

    return segments[1] === path;
  }
}
