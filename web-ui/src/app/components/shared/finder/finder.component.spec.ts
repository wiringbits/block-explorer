import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { FinderComponent } from './finder.component';

import { TranslateModule } from '@ngx-translate/core';

import { TranslateService } from '@ngx-translate/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { NavigatorService } from '../../../services/navigator.service';
import { AddressesService } from '../../../services/addresses.service';
import { BlocksService } from '../../../services/blocks.service';
import { MasternodesService } from '../../../services/masternodes.service';
import { TransactionsService } from '../../../services/transactions.service';
import { ErrorService } from '../../../services/error.service';

import { NO_ERRORS_SCHEMA, } from '@angular/core';

import { Observable } from 'rxjs';

describe('TransactionFinderComponent', () => {
  let component: FinderComponent;
  let fixture: ComponentFixture<FinderComponent>;

  const navigatorServiceSpy: jasmine.SpyObj<NavigatorService> = jasmine.createSpyObj('NavigatorService', [
    'addressDetails',
    'transactionDetails',
    'masternodeDetails',
    'blockDetails'
  ]);
  const addressesServiceSpy: jasmine.SpyObj<AddressesService> = jasmine.createSpyObj('AddressesService', ['get']);
  const blocksServiceSpy: jasmine.SpyObj<BlocksService> = jasmine.createSpyObj('BlocksService', ['get']);
  const masternodesServiceSpy: jasmine.SpyObj<MasternodesService> = jasmine.createSpyObj('MasternodesService', ['getByIP']);
  const transactionsServiceSpy: jasmine.SpyObj<TransactionsService> = jasmine.createSpyObj('TransactionsService', ['get']);
  const errorServiceSpy: jasmine.SpyObj<ErrorService> = jasmine.createSpyObj('ErrorService', [
    'setFieldError',
    'hasCorrectValue',
    'hasWrongValue',
    'getFieldError']);

  beforeEach(async(() => {
    addressesServiceSpy.get.and.returnValue(Observable.create());
    blocksServiceSpy.get.and.returnValue(Observable.create());
    masternodesServiceSpy.getByIP.and.returnValue(Observable.create());
    transactionsServiceSpy.get.and.returnValue(Observable.create());

    TestBed.configureTestingModule({
      declarations: [
        FinderComponent
      ],
      imports: [
        TranslateModule.forRoot()
      ],
      providers: [
        FormBuilder,
        TranslateService,
        { provide: NavigatorService, useValue: navigatorServiceSpy },
        { provide: AddressesService, useValue: addressesServiceSpy },
        { provide: BlocksService, useValue: blocksServiceSpy },
        { provide: MasternodesService, useValue: masternodesServiceSpy },
        { provide: TransactionsService, useValue: transactionsServiceSpy },
        { provide: ErrorService, useValue: errorServiceSpy }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(FinderComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
