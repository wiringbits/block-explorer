import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { RichestAddressesComponent } from './richest-addresses.component';
import { ExplorerCurrencyPipe } from '../../pipes/explorer-currency.pipe';

import { InfiniteScrollModule } from 'ngx-infinite-scroll';
import { TranslateModule } from '@ngx-translate/core';

import { BalancesService } from '../../services/balances.service';
import { TickerService } from '../../services/ticker.service';
import { Observable } from 'rxjs';

import { NO_ERRORS_SCHEMA, } from '@angular/core';

describe('RichestAddressesComponent', () => {
  let component: RichestAddressesComponent;
  let fixture: ComponentFixture<RichestAddressesComponent>;

  const balancesServiceSpy: jasmine.SpyObj<BalancesService> = jasmine.createSpyObj('BalancesService', ['getHighest']);
  const tickerServiceSpy: jasmine.SpyObj<TickerService> = jasmine.createSpyObj('TickerService', ['get']);

  beforeEach(async(() => {
    balancesServiceSpy.getHighest.and.returnValue(Observable.create());
    tickerServiceSpy.get.and.returnValue(Observable.create());

    TestBed.configureTestingModule({
      declarations: [
        RichestAddressesComponent,
        ExplorerCurrencyPipe
      ],
      imports: [
        InfiniteScrollModule,
        TranslateModule.forRoot()
      ],
      providers: [
        { provide: BalancesService, useValue: balancesServiceSpy },
        { provide: TickerService, useValue: tickerServiceSpy }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(RichestAddressesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
