import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { NodeTickerComponent } from './node-ticker.component';
import { ExplorerCurrencyPipe } from '../../../pipes/explorer-currency.pipe';
import { ExplorerAmountPipe } from '../../../pipes/explorer-amount.pipe';

import { TranslateModule } from '@ngx-translate/core';

import { TickerService } from '../../../services/ticker.service';
import { Observable } from 'rxjs';

describe('NodeTickerComponent', () => {
  let component: NodeTickerComponent;
  let fixture: ComponentFixture<NodeTickerComponent>;

  const tickerServiceSpy: jasmine.SpyObj<TickerService> = jasmine.createSpyObj('TickerService', ['get', 'getNodeStats']);

  beforeEach(async(() => {
    tickerServiceSpy.get.and.returnValue(Observable.create());
    tickerServiceSpy.getNodeStats.and.returnValue(Observable.create());

    TestBed.configureTestingModule({
      declarations: [
        NodeTickerComponent,
        ExplorerCurrencyPipe,
        ExplorerAmountPipe
      ],
      imports: [
        TranslateModule.forRoot()
      ],
      providers: [
        { provide: TickerService, useValue: tickerServiceSpy }
      ]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(NodeTickerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
