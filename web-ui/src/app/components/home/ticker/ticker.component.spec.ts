import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { TooltipModule } from 'ng2-tooltip-directive';
import { TickerComponent } from './ticker.component';
import { ExplorerCurrencyPipe } from '../../../pipes/explorer-currency.pipe';
import { ExplorerAmountPipe } from '../../../pipes/explorer-amount.pipe';

import { TranslateModule } from '@ngx-translate/core';

import { TickerService } from '../../../services/ticker.service';
import { Observable } from 'rxjs';

describe('TickerComponent', () => {
  let component: TickerComponent;
  let fixture: ComponentFixture<TickerComponent>;

  const tickerServiceSpy: jasmine.SpyObj<TickerService> = jasmine.createSpyObj('TickerService', ['get', 'getPrices', 'getNodeStats']);

  beforeEach(async(() => {
    tickerServiceSpy.get.and.returnValue(Observable.create());
    tickerServiceSpy.getNodeStats.and.returnValue(Observable.create());
    tickerServiceSpy.getPrices.and.returnValue(Observable.create());

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
        { provide: TickerService, useValue: tickerServiceSpy }
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
