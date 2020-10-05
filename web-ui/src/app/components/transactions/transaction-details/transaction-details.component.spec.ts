import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { TransactionDetailsComponent } from './transaction-details.component';
import { ExplorerDatetimePipe } from '../../../pipes/explorer-datetime.pipe';
import { ExplorerCurrencyPipe } from '../../../pipes/explorer-currency.pipe';

import { TranslateModule } from '@ngx-translate/core';
import { RouterTestingModule } from '@angular/router/testing';

import { NavigatorService } from '../../../services/navigator.service';
import { TransactionsService } from '../../../services/transactions.service';
import { ErrorService } from '../../../services/error.service';
import { Observable } from 'rxjs';

import { NO_ERRORS_SCHEMA, } from '@angular/core';

describe('TransactionDetailsComponent', () => {
  let component: TransactionDetailsComponent;
  let fixture: ComponentFixture<TransactionDetailsComponent>;

  const navigatorServiceSpy: jasmine.SpyObj<NavigatorService> = jasmine.createSpyObj('NavigatorService', ['']);
  const transactionsServiceSpy: jasmine.SpyObj<TransactionsService> = jasmine.createSpyObj('TransactionsService', ['get']);
  const errorServiceSpy: jasmine.SpyObj<ErrorService> = jasmine.createSpyObj('ErrorService', ['renderServerErrors']);

  beforeEach(async(() => {
    transactionsServiceSpy.get.and.returnValue(Observable.create());

    TestBed.configureTestingModule({
      declarations: [
        TransactionDetailsComponent,
        ExplorerDatetimePipe,
        ExplorerCurrencyPipe
      ],
      imports: [
        TranslateModule.forRoot(),
        RouterTestingModule
      ],
      providers: [
        { provide: NavigatorService, useValue: navigatorServiceSpy },
        { provide: TransactionsService, useValue: transactionsServiceSpy },
        { provide: ErrorService, useValue: errorServiceSpy },
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(TransactionDetailsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
