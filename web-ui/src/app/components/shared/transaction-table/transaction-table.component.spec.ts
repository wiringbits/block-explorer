import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { TransactionTableComponent } from './transaction-table.component';

import { MomentModule } from 'ngx-moment';
import { TranslateModule } from '@ngx-translate/core';
import { RouterTestingModule } from '@angular/router/testing';
import { PipesModule } from '../../../pipes/pipes.module';

import { TransactionsService } from '../../../services/transactions.service';
import { AddressesService } from '../../../services/addresses.service';
import { ErrorService } from '../../../services/error.service';
import { Observable } from 'rxjs';

import { NO_ERRORS_SCHEMA, } from '@angular/core';

describe('TransactionTableComponent', () => {
  let component: TransactionTableComponent;
  let fixture: ComponentFixture<TransactionTableComponent>;

  const transactionsServiceSpy: jasmine.SpyObj<TransactionsService> = jasmine.createSpyObj('TransactionsService', ['getList']);
  const addressesServiceSpy: jasmine.SpyObj<AddressesService> = jasmine.createSpyObj('AddressesService', ['getTransactions']);
  const errorServiceSpy: jasmine.SpyObj<ErrorService> = jasmine.createSpyObj('ErrorService', ['renderServerErrors']);

  beforeEach(async(() => {
    transactionsServiceSpy.getList.and.returnValue(Observable.create());
    addressesServiceSpy.getTransactions.and.returnValue(Observable.create());

    TestBed.configureTestingModule({
      declarations: [
        TransactionTableComponent
      ],
      imports: [
        TranslateModule.forRoot(),
        MomentModule,
        PipesModule,
        RouterTestingModule
      ],
      providers: [
        { provide: TransactionsService, useValue: transactionsServiceSpy },
        { provide: AddressesService, useValue: addressesServiceSpy },
        { provide: ErrorService, useValue: errorServiceSpy }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(TransactionTableComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
