import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { AddressListComponent } from './address-list.component';
import { ExplorerCurrencyPipe } from '../../../pipes/explorer-currency.pipe';

import { InfiniteScrollModule } from 'ngx-infinite-scroll';
import { TranslateModule } from '@ngx-translate/core';

import { BalancesService } from '../../../services/balances.service';
import { TickerService } from '../../../services/ticker.service';
import { NotificationService } from '../../../services/notification.service';
import { Observable } from 'rxjs';

import { NO_ERRORS_SCHEMA, } from '@angular/core';

describe('AddressListComponent', () => {
  let component: AddressListComponent;
  let fixture: ComponentFixture<AddressListComponent>;

  const balancesServiceSpy: jasmine.SpyObj<BalancesService> = jasmine.createSpyObj('BalancesService', ['getHighest']);
  const tickerServiceSpy: jasmine.SpyObj<TickerService> = jasmine.createSpyObj('TickerService', ['get']);
  const notificationServiceSpy: jasmine.SpyObj<NotificationService> = jasmine.createSpyObj('NotificationService', ['warning']);

  beforeEach(async(() => {
    balancesServiceSpy.getHighest.and.returnValue(Observable.create());
    tickerServiceSpy.get.and.returnValue(Observable.create());

    TestBed.configureTestingModule({
      declarations: [
        AddressListComponent,
        ExplorerCurrencyPipe
      ],
      imports: [
        InfiniteScrollModule,
        TranslateModule.forRoot()
      ],
      providers: [
        { provide: BalancesService, useValue: balancesServiceSpy },
        { provide: TickerService, useValue: tickerServiceSpy },
        { provide: NotificationService, useValue: notificationServiceSpy }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(AddressListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
