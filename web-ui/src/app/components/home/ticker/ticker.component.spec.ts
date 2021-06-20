import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { TooltipModule } from 'ng2-tooltip-directive';
import { TickerComponent } from './ticker.component';
import { ExplorerCurrencyPipe } from '../../../pipes/explorer-currency.pipe';
import { ExplorerAmountPipe } from '../../../pipes/explorer-amount.pipe';

import { TranslateModule } from '@ngx-translate/core';

import { TickerService } from '../../../services/ticker.service';
import { XSNService } from '../../../services/xsn.service';
import { Observable } from 'rxjs';

describe('TickerComponent', () => {
  let component: TickerComponent;
  let fixture: ComponentFixture<TickerComponent>;

  const tickerServiceSpy: jasmine.SpyObj<TickerService> = jasmine.createSpyObj('TickerService', ['get']);
  const xsnServiceSpy: jasmine.SpyObj<XSNService> = jasmine.createSpyObj('XSNService', ['getRewardsSummary', 'getPrices', 'getNodeStats']);
  
  beforeEach(async(() => {
    tickerServiceSpy.get.and.returnValue(Observable.create());
    xsnServiceSpy.getNodeStats.and.returnValue(Observable.create());
    xsnServiceSpy.getPrices.and.returnValue(Observable.create());
    xsnServiceSpy.getRewardsSummary.and.returnValue(Observable.create());

    TestBed.configureTestingModule({
      declarations: [
        TickerComponent,
        ExplorerCurrencyPipe,
        ExplorerAmountPipe
      ],
      imports: [
        TranslateModule.forRoot(),
        TooltipModule
      ],
      providers: [
        { provide: TickerService, useValue: tickerServiceSpy },
        { provide: XSNService, useValue: xsnServiceSpy }
      ]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(TickerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
