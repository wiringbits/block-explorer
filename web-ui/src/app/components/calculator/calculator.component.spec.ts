import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { CalculatorComponent } from './calculator.component';

import { TranslateModule } from '@ngx-translate/core';
import { RouterTestingModule } from '@angular/router/testing';

import { NO_ERRORS_SCHEMA, } from '@angular/core';
import { TickerService } from '../../services/ticker.service';
import { Observable } from 'rxjs';

describe('CalculatorComponent', () => {
  let component: CalculatorComponent;
  let fixture: ComponentFixture<CalculatorComponent>;

  const tickerServiceSpy: jasmine.SpyObj<TickerService> = jasmine.createSpyObj('TickerService', ['getNodeStats', 'get', 'getPrices']);

  beforeEach(async(() => {
    tickerServiceSpy.getNodeStats.and.returnValue(Observable.create());
    tickerServiceSpy.get.and.returnValue(Observable.create());
    tickerServiceSpy.getPrices.and.returnValue(Observable.create());

    TestBed.configureTestingModule({
      declarations: [CalculatorComponent],
      imports: [
        TranslateModule.forRoot(),
        RouterTestingModule
      ],
      providers: [
        { provide: TickerService, useValue: tickerServiceSpy }],
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
