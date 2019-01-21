import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { TickerComponent } from './ticker.component';
import { ExplorerCurrencyPipe } from '../../pipes/explorer-currency.pipe';

import { TranslateModule } from '@ngx-translate/core';

import { TickerService } from '../../services/ticker.service';
import { Observable } from 'rxjs';

describe('TickerComponent', () => {
  let component: TickerComponent;
  let fixture: ComponentFixture<TickerComponent>;

  const tickerServiceSpy: jasmine.SpyObj<TickerService> = jasmine.createSpyObj('TickerService', ['get']);

  beforeEach(async(() => {
    tickerServiceSpy.get.and.returnValue(Observable.create());

    TestBed.configureTestingModule({
      declarations: [
        TickerComponent,
        ExplorerCurrencyPipe
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
    fixture = TestBed.createComponent(TickerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
