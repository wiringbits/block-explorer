import { Component, OnInit, Input } from '@angular/core';
import { Router } from '@angular/router';
import { TickerService } from '../../../services/ticker.service';

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
  showNavbar = false;
  public lottieConfig: Object;

  constructor(private router: Router, private tickerService: TickerService) {
    this.lottieConfig = {
      path: 'assets/Updating.json',
      renderer: 'canvas',
      autoplay: true,
      loop: true
    };
  }

  ngOnInit() {
  }

  /* tabs */
  isSelected(path: any, isSubmenu?: boolean): boolean {
    const segments = this.router.url.split('/');
    if (isSubmenu && segments[1] == "") return false;
    return path.indexOf(segments[1]) > -1;
  }

  navbarToggle() {
    this.showNavbar = !this.showNavbar;
  }
}
