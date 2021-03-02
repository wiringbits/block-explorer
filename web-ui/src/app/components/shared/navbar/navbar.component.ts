import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';

class Tab {
  label: string;
  path?: string;
  mainTab: boolean;
  hasChildren: boolean;
  children?: Array<any>;
  selector?: any;
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
      mainTab: true,
      hasChildren: false,
      selector: ['']
    },
    {
      label: 'Nodes',
      path: '/nodes',
      mainTab: true,
      hasChildren: false,
      selector: ['nodes']
    },
    {
      label: 'Blocks',
      path: '/blocks',
      mainTab: true,
      hasChildren: false,
      selector: ['blocks']
    },
    {
      label: 'Transactions',
      path: '/transactions',
      mainTab: true,
      hasChildren: false,
      selector: ['transactions']
    },
    {
      label: 'Addresses',
      path: '/addresses',
      mainTab: true,
      hasChildren: false,
      selector: ['addresses']
    },
    {
      label: 'Tools',
      path: '/tools',
      mainTab: true,
      hasChildren: true,
      selector: ['trezor', 'calculator', 'governance'],
      children: [{
        label: 'Trazor Wallet',
        path: '/trezor',
        mainTab: true,
        hasChildren: false,
        selector: '/tools'
      },{
        label: 'Calculator',
        path: '/calculator',
        mainTab: false,
        hasChildren: false,
        selector: '/tools'
      },{
        label: 'DEX stats',
        path: '/dexstats',
        mainTab: false,
        hasChildren: false,
        selector: '/tools'
      },{
        label: 'Governance',
        path: '/',
        mainTab: false,
        hasChildren: false,
        selector: ''
      }]
    }
  ];

  public currentUrl = null;

  constructor(private router: Router) {
  }

  ngOnInit() {
  }

  /* tabs */
  isSelected(path: any, isSubmenu?: boolean): boolean {
    const segments = this.router.url.split('/');
    if (isSubmenu && segments[1] == "") return false;
    return path.indexOf(segments[1]) > -1;
  }
}
