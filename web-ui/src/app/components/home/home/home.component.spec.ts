import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { CUSTOM_ELEMENTS_SCHEMA, Component } from '@angular/core';

import { APP_BASE_HREF } from '@angular/common';

import { TabsModule } from 'ngx-bootstrap/tabs';
import { TranslateModule } from '@ngx-translate/core';

import { HomeComponent } from './home.component';

import { BlocksService } from '../../../services/blocks.service';

describe('HomeComponent', () => {
  let component: HomeComponent;
  let fixture: ComponentFixture<HomeComponent>;

  const blocksServiceSpy: jasmine.SpyObj<BlocksService> = jasmine.createSpyObj('BlocksService', ['getLatest']);

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [
        HomeComponent
      ],
      imports: [
        TranslateModule.forRoot(),
        TabsModule.forRoot()
      ],
      providers: [
        { provide: APP_BASE_HREF, useValue: '/' },
        { provide: BlocksService, useValue: blocksServiceSpy }
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA]
    });
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(HomeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
