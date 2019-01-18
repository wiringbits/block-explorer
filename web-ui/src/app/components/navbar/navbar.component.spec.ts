import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { NavbarComponent } from './navbar.component';

import { TranslateModule } from '@ngx-translate/core';
import { RouterModule } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { APP_BASE_HREF, LocationStrategy, PathLocationStrategy, Location } from '@angular/common';

import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';

describe('NavbarComponent', () => {
  let component: NavbarComponent;
  let fixture: ComponentFixture<NavbarComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [
        NavbarComponent
       ],
      imports: [
        TranslateModule.forRoot(),
        RouterModule,
        RouterTestingModule
      ],
      providers: [
        { provide: APP_BASE_HREF, useValue: '/' },
        { provide: LocationStrategy, useClass: PathLocationStrategy },
        Location
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(NavbarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
