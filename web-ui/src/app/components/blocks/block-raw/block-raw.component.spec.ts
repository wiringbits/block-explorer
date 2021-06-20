import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { BlockRawComponent } from './block-raw.component';

import { RouterTestingModule } from '@angular/router/testing';
import { TranslateModule } from '@ngx-translate/core';

import { NavigatorService } from '../../../services/navigator.service';
import { BlocksService } from '../../../services/blocks.service';
import { ErrorService } from '../../../services/error.service';
import { TranslateService } from '@ngx-translate/core';

import { NO_ERRORS_SCHEMA, } from '@angular/core';

import { Observable } from 'rxjs';

describe('BlockRawComponent', () => {
  let component: BlockRawComponent;
  let fixture: ComponentFixture<BlockRawComponent>;

  const navigatorServiceSpy: jasmine.SpyObj<NavigatorService> = jasmine.createSpyObj('NavigatorService', ['']);
  const blocksServiceSpy: jasmine.SpyObj<BlocksService> = jasmine.createSpyObj('BlocksService', ['getRaw']);
  const errorServiceSpy: jasmine.SpyObj<ErrorService> = jasmine.createSpyObj('ErrorService', ['renderServerErrors']);

  beforeEach(async(() => {
    blocksServiceSpy.getRaw.and.returnValue(Observable.create());

    TestBed.configureTestingModule({
      declarations: [BlockRawComponent],
      imports: [
        RouterTestingModule,
        TranslateModule.forRoot()
      ],
      providers: [
        { provide: NavigatorService, useValue: navigatorServiceSpy },
        { provide: BlocksService, useValue: blocksServiceSpy },
        { provide: ErrorService, useValue: errorServiceSpy },
        TranslateService
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(BlockRawComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
