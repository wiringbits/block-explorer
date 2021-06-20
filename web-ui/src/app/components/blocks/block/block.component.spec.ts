import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { BlockComponent } from './block.component';

import { TranslateModule } from '@ngx-translate/core';

import { NO_ERRORS_SCHEMA, } from '@angular/core';
import { RouterTestingModule } from '@angular/router/testing';
import { HttpClientModule } from '@angular/common/http';
import { Observable } from 'rxjs';

import { BlocksService } from '../../../services/blocks.service';
import { ErrorService } from '../../../services/error.service';
import { NotificationService } from '../../../services/notification.service';

describe('BlockComponent', () => {
  let component: BlockComponent;
  let fixture: ComponentFixture<BlockComponent>;

  const blocksServiceSpy: jasmine.SpyObj<BlocksService> = jasmine.createSpyObj('BlocksService', ['get']);
  const errorServiceSpy: jasmine.SpyObj<ErrorService> = jasmine.createSpyObj('ErrorService', ['renderServerErrors']);

  beforeEach(async(() => {
    blocksServiceSpy.get.and.returnValue(Observable.create());

    TestBed.configureTestingModule({
      declarations: [
        BlockComponent
      ],
      imports: [
        TranslateModule.forRoot(),
        RouterTestingModule,
        HttpClientModule
      ],
      schemas: [NO_ERRORS_SCHEMA],
      providers: [
        { provide: BlocksService, useValue: blocksServiceSpy },
        { provide: ErrorService, useValue: errorServiceSpy },
        NotificationService
      ]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(BlockComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
