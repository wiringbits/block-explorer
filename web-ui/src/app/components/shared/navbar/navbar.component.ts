import { Component, OnInit, Input } from '@angular/core';
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

  @Input()
  public tabs: Tab[] = [];

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
