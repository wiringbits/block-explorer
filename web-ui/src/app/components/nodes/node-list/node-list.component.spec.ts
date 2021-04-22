import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { TranslateModule } from '@ngx-translate/core';
import { HttpClientModule } from '@angular/common/http';
import { NodeListComponent } from './node-list.component';
import { TickerService } from '../../../services/ticker.service';

import { NO_ERRORS_SCHEMA, } from '@angular/core';

describe('NodeListComponent', () => {
    let component: NodeListComponent;
    let fixture: ComponentFixture<NodeListComponent>;

    beforeEach(async(() => {
        TestBed.configureTestingModule({
            declarations: [
                NodeListComponent
            ],
            imports: [
                TranslateModule.forRoot(),
                HttpClientModule
            ],
            schemas: [NO_ERRORS_SCHEMA],
            providers: [
                TickerService
            ]
        })
            .compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(NodeListComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
