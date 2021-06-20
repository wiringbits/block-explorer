import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { TranslateModule } from '@ngx-translate/core';

import { BlockListComponent } from './block-list.component';

import { NO_ERRORS_SCHEMA, } from '@angular/core';
import { TickerService } from '../../../services/ticker.service';
import { BlocksService } from '../../../services/blocks.service';
import { ErrorService } from '../../../services/error.service';
import { Observable } from 'rxjs';

describe('BlockListComponent', () => {
    let component: BlockListComponent;
    let fixture: ComponentFixture<BlockListComponent>;

    const tickerServiceSpy: jasmine.SpyObj<TickerService> = jasmine.createSpyObj('TickerService', ['get', 'getPrices']);
    const blocksServiceSpy: jasmine.SpyObj<BlocksService> = jasmine.createSpyObj('BlocksService', ['getLatestV2']);
    const errorServiceSpy: jasmine.SpyObj<ErrorService> = jasmine.createSpyObj('ErrorService', ['renderServerErrors']);

    beforeEach(async(() => {
        tickerServiceSpy.get.and.returnValue(Observable.create());
        tickerServiceSpy.getPrices.and.returnValue(Observable.create());

        blocksServiceSpy.getLatestV2.and.returnValue(Observable.create());

        TestBed.configureTestingModule({
            declarations: [
                BlockListComponent
            ],
            imports: [
                TranslateModule.forRoot()
            ],
            schemas: [NO_ERRORS_SCHEMA],
            providers: [
                { provide: TickerService, useValue: tickerServiceSpy },
                { provide: BlocksService, useValue: blocksServiceSpy },
                { provide: ErrorService, useValue: errorServiceSpy }
            ]
        })
            .compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(BlockListComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
