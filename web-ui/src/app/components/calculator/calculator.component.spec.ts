import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { CalculatorComponent } from './calculator.component';

import { TranslateModule } from '@ngx-translate/core';
import { RouterTestingModule } from '@angular/router/testing';

import { NO_ERRORS_SCHEMA, } from '@angular/core';
import { TickerService } from '../../services/ticker.service';
import { XSNService } from '../../services/xsn.service';
import { Observable } from 'rxjs';
import { ExplorerAmountPipe } from '../../pipes/explorer-amount.pipe';

describe('CalculatorComponent', () => {
  let component: CalculatorComponent;
  let fixture: ComponentFixture<CalculatorComponent>;

  const tickerServiceSpy: jasmine.SpyObj<TickerService> = jasmine.createSpyObj('TickerService', ['get']);
  const xsnServiceSpy: jasmine.SpyObj<XSNService> = jasmine.createSpyObj('XSNService', ['getRewardsSummary', 'getNodeStats', 'getPrices']);

  beforeEach(async(() => {
    tickerServiceSpy.get.and.returnValue(Observable.create());
    xsnServiceSpy.getRewardsSummary.and.returnValue(Observable.create());
    xsnServiceSpy.getNodeStats.and.returnValue(Observable.create());
    xsnServiceSpy.getPrices.and.returnValue(Observable.create());

    TestBed.configureTestingModule({
      declarations: [CalculatorComponent, ExplorerAmountPipe, ],
      imports: [
        TranslateModule.forRoot(),
        RouterTestingModule
      ],
      providers: [
        { provide: TickerService, useValue: tickerServiceSpy },
        { provide: XSNService, useValue: xsnServiceSpy }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(CalculatorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
