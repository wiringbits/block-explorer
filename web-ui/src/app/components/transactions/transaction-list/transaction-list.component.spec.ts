import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { TranslateModule } from '@ngx-translate/core';

import { TransactionListComponent } from './transaction-list.component';

import { NO_ERRORS_SCHEMA, } from '@angular/core';

import { TransactionsService } from '../../../services/transactions.service';
import { ErrorService } from '../../../services/error.service';
import { TickerService } from '../../../services/ticker.service';

describe('TransactionListComponent', () => {
    let component: TransactionListComponent;
    let fixture: ComponentFixture<TransactionListComponent>;

    const tickerServiceSpy: jasmine.SpyObj<TickerService> = jasmine.createSpyObj('TickerService', ['getPrices', 'get']);
    const transactionsServiceSpy: jasmine.SpyObj<TransactionsService> = jasmine.createSpyObj('TransactionsService', ['getList']);
    const errorServiceSpy: jasmine.SpyObj<ErrorService> = jasmine.createSpyObj('ErrorService', ['renderServerErrors']);

    beforeEach(async(() => {
        TestBed.configureTestingModule({
            declarations: [
                TransactionListComponent
            ],
            imports: [
                TranslateModule.forRoot()
            ],
            schemas: [NO_ERRORS_SCHEMA],
            providers: [
                { provide: TickerService, useValue: tickerServiceSpy },
                { provide: TransactionsService, useValue: transactionsServiceSpy },
                { provide: ErrorService, useValue: errorServiceSpy },
            ]
        })
            .compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(TransactionListComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
