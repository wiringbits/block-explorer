import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { TransactionRawComponent } from './transaction-raw.component';

import { TranslateModule } from '@ngx-translate/core';
import { RouterTestingModule } from '@angular/router/testing';

import { NavigatorService } from '../../../services/navigator.service';
import { TransactionsService } from '../../../services/transactions.service';
import { ErrorService } from '../../../services/error.service';
import { Observable } from 'rxjs';

import { NO_ERRORS_SCHEMA, } from '@angular/core';

describe('TransactionRawComponent', () => {
  let component: TransactionRawComponent;
  let fixture: ComponentFixture<TransactionRawComponent>;

  const navigatorServiceSpy: jasmine.SpyObj<NavigatorService> = jasmine.createSpyObj('NavigatorService', ['']);
  const transactionsServiceSpy: jasmine.SpyObj<TransactionsService> = jasmine.createSpyObj('TransactionsService', ['getRaw']);
  const errorServiceSpy: jasmine.SpyObj<ErrorService> = jasmine.createSpyObj('ErrorService', ['renderServerErrors']);

  beforeEach(async(() => {
    transactionsServiceSpy.getRaw.and.returnValue(Observable.create());

    TestBed.configureTestingModule({
      declarations: [TransactionRawComponent],
      imports: [
        TranslateModule.forRoot(),
        RouterTestingModule
      ],
      providers: [
        { provide: NavigatorService, useValue: navigatorServiceSpy },
        { provide: TransactionsService, useValue: transactionsServiceSpy },
        { provide: ErrorService, useValue: errorServiceSpy }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(TransactionRawComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
