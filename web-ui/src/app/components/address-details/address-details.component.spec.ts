import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { AddressDetailsComponent } from './address-details.component';
import { ExplorerCurrencyPipe } from '../../pipes/explorer-currency.pipe';
import { ExplorerDatetimePipe } from '../../pipes/explorer-datetime.pipe';

import { RouterTestingModule } from '@angular/router/testing';
import { TranslateModule } from '@ngx-translate/core';

import { AddressesService } from '../../services/addresses.service';
import { ErrorService } from '../../services/error.service';
import { Observable } from 'rxjs';

import { NO_ERRORS_SCHEMA, } from '@angular/core';

describe('AddressDetailsComponent', () => {
  let component: AddressDetailsComponent;
  let fixture: ComponentFixture<AddressDetailsComponent>;

  const addressesServiceSpy: jasmine.SpyObj<AddressesService> = jasmine.createSpyObj('AddressesService', ['get', 'getAddressesV2']);
  const errorServiceSpy: jasmine.SpyObj<AddressesService> = jasmine.createSpyObj('ErrorService', ['renderServerErrors']);

  beforeEach(async(() => {
    addressesServiceSpy.get.and.returnValue(Observable.create());

    TestBed.configureTestingModule({
      declarations: [
        AddressDetailsComponent,
        ExplorerCurrencyPipe,
        ExplorerDatetimePipe
      ],
      imports: [
        RouterTestingModule,
        TranslateModule.forRoot()
      ],
      providers: [
        { provide: AddressesService, useValue: addressesServiceSpy },
        { provide: ErrorService, useValue: errorServiceSpy }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(AddressDetailsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
