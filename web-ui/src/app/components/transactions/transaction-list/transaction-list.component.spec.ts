import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { TranslateModule } from '@ngx-translate/core';

import { TransactionListComponent } from './transaction-list.component';

import { NO_ERRORS_SCHEMA, } from '@angular/core';

describe('TransactionListComponent', () => {
    let component: TransactionListComponent;
    let fixture: ComponentFixture<TransactionListComponent>;

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
